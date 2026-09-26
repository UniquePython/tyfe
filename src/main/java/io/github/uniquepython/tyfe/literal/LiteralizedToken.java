package io.github.uniquepython.tyfe.literal;

import java.util.Objects;

import io.github.uniquepython.tyfe.common.Span;
import io.github.uniquepython.tyfe.lexer.Token;
import io.github.uniquepython.tyfe.lexer.TokenKind;

public record LiteralizedToken(TokenKind kind, Span span, LiteralValue value) {

    public LiteralizedToken {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(span, "span");
        Objects.requireNonNull(value, "value");
    }

    public static LiteralizedToken of(Token original, LiteralValue value) {
        return new LiteralizedToken(original.kind(), original.span(), value);
    }

    @Override
    public String toString() {
        return "%s @ %s".formatted(value, span);
    }

}
