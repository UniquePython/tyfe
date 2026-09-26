package io.github.uniquepython.tyfe;

import java.util.Objects;

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
