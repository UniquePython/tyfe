package io.github.uniquepython.tyfe.interpreter;

import io.github.uniquepython.tyfe.common.Span;
import io.github.uniquepython.tyfe.common.TyfeError;

public abstract sealed class TyfeCatastrophe extends TyfeError
        permits TyfeCatastrophe.DivisionByZero, TyfeCatastrophe.IntegerOverflow, TyfeCatastrophe.NegativeExponent {

    protected TyfeCatastrophe(String message, Span span) {
        super(message, span);
    }

    public static final class DivisionByZero extends TyfeCatastrophe {
        public DivisionByZero(Span span) {
            super("Division by zero", span);
        }
    }

    public static final class IntegerOverflow extends TyfeCatastrophe {
        private final String operation;

        public IntegerOverflow(String operation, Span span) {
            super("Integer overflow in %s".formatted(operation), span);
            this.operation = operation;
        }

        public String operation() {
            return operation;
        }
    }

    public static final class NegativeExponent extends TyfeCatastrophe {
        public NegativeExponent(Span span) {
            super("Cannot raise an integer to a negative power", span);
        }
    }

}
