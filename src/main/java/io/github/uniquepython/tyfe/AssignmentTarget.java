package io.github.uniquepython.tyfe;

import java.util.Objects;

public sealed interface AssignmentTarget permits AssignmentTarget.Identifier {

    Span span();

    record Identifier(String name, Span span) implements AssignmentTarget {
        public Identifier {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(span, "span");
        }
    }

}
