package io.github.uniquepython.tyfe.parser;

import java.util.List;
import io.github.uniquepython.tyfe.literal.LiteralizedToken;
import io.github.uniquepython.tyfe.lexer.TokenKind;
import io.github.uniquepython.tyfe.ast.Expr;

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

}
