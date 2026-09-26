package io.github.uniquepython.tyfe.ast;

import java.util.Objects;

import io.github.uniquepython.tyfe.common.Span;

public sealed interface AssignmentTarget permits AssignmentTarget.Identifier {

    Span span();

    record Identifier(String name, Span span) implements AssignmentTarget {
        public Identifier {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(span, "span");
        }
    }

}
