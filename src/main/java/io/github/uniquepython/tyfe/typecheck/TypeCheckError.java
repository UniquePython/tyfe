package io.github.uniquepython.tyfe.typecheck;

import io.github.uniquepython.tyfe.common.Span;
import io.github.uniquepython.tyfe.common.TyfeError;

public abstract sealed class TypeCheckError extends TyfeError permits TypeCheckError.DuplicateDeclaration {

    protected TypeCheckError(String message, Span span) {
        super(message, span);
    }

    public static final class DuplicateDeclaration extends TypeCheckError {

        private final String name;
        private final Span originalSpan;

        public DuplicateDeclaration(String name, Span originalSpan, Span span) {
            super("Duplicate declaration of '%s' in this scope".formatted(name), span);
            this.name = name;
            this.originalSpan = originalSpan;
        }

        public String name() {
            return name;
        }

        public Span originalSpan() {
            return originalSpan;
        }

    }

}
