package io.github.uniquepython.tyfe.parser;

import java.util.List;
import io.github.uniquepython.tyfe.literal.LiteralizedToken;
import io.github.uniquepython.tyfe.lexer.TokenKind;
import io.github.uniquepython.tyfe.ast.Expr;
import io.github.uniquepython.tyfe.ast.BinaryOperator;
import io.github.uniquepython.tyfe.common.Span;

public final class Parser {

    private final String source;
    private final List<LiteralizedToken> tokens;
    private int position;

    public Parser(String source, List<LiteralizedToken> tokens) {
        this.source = source;
        this.tokens = tokens;
        this.position = 0;
    }

    private LiteralizedToken peek() {
        return tokens.get(position);
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
            throw new ParserError.UnexpectedToken(peek().kind(), kind.toString(), peek().span());
        }
        return advance();
    }

    private Expr.Literal parseLiteral() {
        LiteralizedToken token = advance();
        return new Expr.Literal(token.value(), token.span());
    }

    private Expr.Identifier parseIdentifier() {
        LiteralizedToken token = advance();
        String name = source.substring(token.span().start(), token.span().end());
        return new Expr.Identifier(name, token.span());
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

}
