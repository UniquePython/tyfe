package io.github.uniquepython.tyfe.parser;

import java.util.ArrayList;
import java.util.List;

import io.github.uniquepython.tyfe.literal.LiteralValue;
import io.github.uniquepython.tyfe.literal.LiteralizedToken;
import io.github.uniquepython.tyfe.lexer.TokenKind;
import io.github.uniquepython.tyfe.ast.Expr;
import io.github.uniquepython.tyfe.ast.Mutability;
import io.github.uniquepython.tyfe.ast.Stmt;
import io.github.uniquepython.tyfe.ast.BinaryOperator;
import io.github.uniquepython.tyfe.ast.UnaryOperator;
import io.github.uniquepython.tyfe.ast.Type;
import io.github.uniquepython.tyfe.common.Span;

public final class Parser {
    private final List<LiteralizedToken> tokens;
    private int position;

    public Parser(List<LiteralizedToken> tokens) {
        this.tokens = tokens;
        this.position = 0;
    }

    private LiteralizedToken peek(int offset) {
        int index = position + offset;
        if (index >= tokens.size()) {
            return tokens.get(tokens.size() - 1); // EOF is guaranteed to be the last token
        }
        return tokens.get(index);
    }

    private LiteralizedToken peek() {
        return peek(0);
    }

    private LiteralizedToken advance() {
        LiteralizedToken current = peek();
        if (!isAtEnd())
            position++;
        return current;
    }

    private boolean isAtEnd() {
        return peek().kind() == TokenKind.Sentinel.EOF;
    }

    private LiteralizedToken expect(TokenKind kind) {
        if (peek().kind() != kind) {
            throw new ParserError.UnexpectedToken(peek().kind(), kind, peek().span());
        }
        return advance();
    }

    private Expr parseOr() {
        Expr left = parseAnd();
        while (peek().kind() == TokenKind.Operator.Logical.OR) {
            advance();
            Expr right = parseAnd();
            left = new Expr.Binary(left, BinaryOperator.Logical.OR, right, left.span().merge(right.span()));
        }
        return left;
    }

    private Expr parseAnd() {
        Expr left = parseComparison();
        while (peek().kind() == TokenKind.Operator.Logical.AND) {
            advance();
            Expr right = parseComparison();
            left = new Expr.Binary(left, BinaryOperator.Logical.AND, right, left.span().merge(right.span()));
        }
        return left;
    }

    private Expr parseComparison() {
        Expr left = parseAdditive();

        TokenKind kind = peek().kind();
        if (kind instanceof TokenKind.Operator.Comparison.Checked op) {
            advance();
            Expr right = parseAdditive();
            Span span = left.span().merge(right.span());

            left = switch (op) {
                case EQ -> new Expr.Binary(left, BinaryOperator.Comparison.EQ, right, span);
                case NEQ -> new Expr.Binary(left, BinaryOperator.Comparison.NEQ, right, span);
                case LT -> new Expr.Binary(left, BinaryOperator.Comparison.LT, right, span);
                case LTE -> new Expr.Binary(left, BinaryOperator.Comparison.LTE, right, span);
                case GT -> new Expr.Binary(right, BinaryOperator.Comparison.LT, left, span);
                case GTE -> new Expr.Binary(right, BinaryOperator.Comparison.LTE, left, span);
                case NLT -> new Expr.Binary(right, BinaryOperator.Comparison.LTE, left, span);
                case NLTE -> new Expr.Binary(right, BinaryOperator.Comparison.LT, left, span);
                case NGT -> new Expr.Binary(left, BinaryOperator.Comparison.LTE, right, span);
                case NGTE -> new Expr.Binary(left, BinaryOperator.Comparison.LT, right, span);
            };
        }

        return left;
    }

    private Expr parseAdditive() {
        Expr left = parseMultiplicative();
        while (true) {
            TokenKind kind = peek().kind();
            BinaryOperator op;
            if (kind == TokenKind.Operator.Arithmetic.Checked.PLUS) {
                op = BinaryOperator.Arithmetic.Checked.ADD;
            } else if (kind == TokenKind.Operator.Arithmetic.Checked.MINUS) {
                op = BinaryOperator.Arithmetic.Checked.SUB;
            } else {
                break;
            }
            advance();
            Expr right = parseMultiplicative();
            left = new Expr.Binary(left, op, right, left.span().merge(right.span()));
        }
        return left;
    }

    private Expr parseMultiplicative() {
        Expr left = parseExponent();
        while (true) {
            TokenKind kind = peek().kind();
            BinaryOperator op;
            if (kind == TokenKind.Operator.Arithmetic.Checked.MUL) {
                op = BinaryOperator.Arithmetic.Checked.MUL;
            } else if (kind == TokenKind.Operator.Arithmetic.Checked.DIV) {
                op = BinaryOperator.Arithmetic.Checked.DIV;
            } else if (kind == TokenKind.Operator.Arithmetic.Checked.MOD) {
                op = BinaryOperator.Arithmetic.Checked.MOD;
            } else {
                break;
            }
            advance();
            Expr right = parseExponent();
            left = new Expr.Binary(left, op, right, left.span().merge(right.span()));
        }
        return left;
    }

    private Expr parseExponent() {
        Expr left = parseUnary();
        if (peek().kind() == TokenKind.Operator.Arithmetic.Checked.EXP) {
            advance();
            Expr right = parseExponent();
            return new Expr.Binary(left, BinaryOperator.Arithmetic.Checked.EXP, right, left.span().merge(right.span()));
        }
        return left;
    }

    private Expr parseUnary() {
        TokenKind kind = peek().kind();

        if (kind == TokenKind.Operator.Arithmetic.Checked.MINUS) {
            Span start = advance().span();
            Expr operand = parseUnary();
            return new Expr.Unary(UnaryOperator.Arithmetic.Checked.NEG, operand, start.merge(operand.span()));
        }

        if (kind == TokenKind.Operator.Logical.NOT) {
            Span start = advance().span();
            Expr operand = parseUnary();
            return new Expr.Unary(UnaryOperator.Logical.NOT, operand, start.merge(operand.span()));
        }

        return parsePrimary();
    }

    private Expr.Literal parseLiteral() {
        LiteralizedToken token = advance();
        return new Expr.Literal(token.value(), token.span());
    }

    private Expr.Identifier parseIdentifier() {
        LiteralizedToken token = advance();
        LiteralValue.IdentValue ident = (LiteralValue.IdentValue) token.value();
        return new Expr.Identifier(ident.name(), token.span());
    }

    public Expr parsePrimary() {
        TokenKind kind = peek().kind();

        if (kind instanceof TokenKind.Literal)
            return parseLiteral();

        if (kind == TokenKind.Identifier.IDENT)
            return parseIdentifier();

        if (kind == TokenKind.Punctuation.LPAREN) {
            advance();
            Expr inner = parseExpression();
            expect(TokenKind.Punctuation.RPAREN);
            return inner;
        }

        throw new ParserError.UnexpectedToken(kind, "an expression", peek().span());
    }

    public Expr parseExpression() {
        return parseOr();
    }

    private Expr.Block parseBlock() {
        Span start = expect(TokenKind.Punctuation.LBRACE).span();

        List<Stmt> statements = new ArrayList<>();
        while (peek().kind() != TokenKind.Punctuation.RBRACE && !isAtEnd()) {
            statements.add(parseStatement());
        }

        Span end = expect(TokenKind.Punctuation.RBRACE).span();
        return new Expr.Block(statements, start.merge(end));
    }

    private Stmt parseStatement() {
        TokenKind kind = peek().kind();

        if (kind == TokenKind.Keyword.MUT || kind == TokenKind.Keyword.CONST)
            return parseDeclaration();

        if (kind == TokenKind.Identifier.IDENT && peek(1).kind() == TokenKind.Operator.Assignment.ASSIGN)
            return parseAssignment();

        if (kind == TokenKind.Keyword.PRODUCE)
            return parseProduce();

        if (kind == TokenKind.Keyword.STOP)
            return parseStop();

        if (kind == TokenKind.Keyword.SKIP)
            return parseSkip();

        return parseExpressionStatement();
    }

    private Type parseType() {
        TokenKind kind = peek().kind();
        Type type = switch (kind) {
            case TokenKind.Keyword.I32 -> Type.Primitive.I32;
            case TokenKind.Keyword.F64 -> Type.Primitive.F64;
            case TokenKind.Keyword.BOOL -> Type.Primitive.BOOL;
            case TokenKind.Keyword.CHAR -> Type.Primitive.CHAR;
            default -> throw new ParserError.UnexpectedToken(kind, "a type", peek().span());
        };
        advance();
        return type;
    }

    private Stmt.Declaration parseDeclaration() {
        LiteralizedToken mutToken = advance(); // consumes 'mut' or 'const', already checked by caller
        Mutability mutability = mutToken.kind() == TokenKind.Keyword.MUT ? Mutability.MUT : Mutability.CONST;

        Type type = parseType();

        LiteralizedToken nameToken = expect(TokenKind.Identifier.IDENT);
        LiteralValue.IdentValue ident = (LiteralValue.IdentValue) nameToken.value();
        String name = ident.name();

        expect(TokenKind.Operator.Assignment.ASSIGN);
        Expr initializer = parseExpression();
        Span end = expect(TokenKind.Punctuation.SEMI_COLON).span();

        return new Stmt.Declaration(mutability, type, name, initializer, mutToken.span().merge(end));
    }

}
