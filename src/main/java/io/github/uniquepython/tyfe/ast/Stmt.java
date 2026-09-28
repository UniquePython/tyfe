package io.github.uniquepython.tyfe.ast;

import java.util.Objects;

import io.github.uniquepython.tyfe.common.Span;

public sealed interface Stmt
        permits Stmt.Declaration, Stmt.Assignment, Stmt.ExpressionStatement, Stmt.Produce, Stmt.Stop, Stmt.Skip,
        Stmt.Yield {

    Span span();

    record Declaration(Mutability mutability, Type type, String name, Expr initializer, Span span) implements Stmt {
        public Declaration {
            Objects.requireNonNull(mutability, "mutability");
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(initializer, "initializer");
            Objects.requireNonNull(span, "span");
        }
    }

    record Assignment(AssignmentTarget target, Expr value, Span span) implements Stmt {
        public Assignment {
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(span, "span");
        }
    }

    record ExpressionStatement(Expr expression, Span span) implements Stmt {
        public ExpressionStatement {
            Objects.requireNonNull(expression, "expression");
            Objects.requireNonNull(span, "span");
        }
    }

    record Produce(Expr value, Span span) implements Stmt {
        public Produce {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(span, "span");
        }
    }

    record Stop(Span span) implements Stmt {
        public Stop {
            Objects.requireNonNull(span, "span");
        }
    }

    record Skip(Span span) implements Stmt {
        public Skip {
            Objects.requireNonNull(span, "span");
        }
    }

    record Yield(Expr value, Span span) implements Stmt {
        public Yield {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(span, "span");
        }
    }

}
