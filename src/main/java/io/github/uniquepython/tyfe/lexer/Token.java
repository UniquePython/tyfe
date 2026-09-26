package io.github.uniquepython.tyfe.lexer;

import java.util.Objects;

import io.github.uniquepython.tyfe.common.Span;

public record Token(TokenKind kind, Span span) {

    public Token {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(span, "span");
    }

    @Override
    public String toString() {
        return "%s @ %s".formatted(kind, span);
    }

}
