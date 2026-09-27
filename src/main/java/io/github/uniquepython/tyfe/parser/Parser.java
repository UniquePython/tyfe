package io.github.uniquepython.tyfe.parser;

import java.util.List;
import io.github.uniquepython.tyfe.literal.LiteralizedToken;
import io.github.uniquepython.tyfe.lexer.TokenKind;
import io.github.uniquepython.tyfe.ast.Expr;

public final class Parser {

    private final List<LiteralizedToken> tokens;
    private int position;

    public Parser(List<LiteralizedToken> tokens) {
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

    public Expr.Literal parseLiteral() {
        TokenKind kind = peek().kind();
        boolean isLiteralToken = kind instanceof TokenKind.Literal;

        if (!isLiteralToken) {
            throw new ParserError.UnexpectedToken(kind, "a literal", peek().span());
        }

        LiteralizedToken token = advance();
        return new Expr.Literal(token.value(), token.span());
    }

}
