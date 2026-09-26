package io.github.uniquepython.tyfe;

import java.util.Objects;

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
