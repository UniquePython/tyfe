package io.github.uniquepython.tyfe.common;

import java.util.Objects;

public abstract class TyfeError extends RuntimeException {

    private final Span span;

    protected TyfeError(String message, Span span) {
        super(message);
        this.span = Objects.requireNonNull(span, "span");
    }

    public Span span() {
        return span;
    }
}
