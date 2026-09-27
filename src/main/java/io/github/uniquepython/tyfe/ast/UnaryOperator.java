package io.github.uniquepython.tyfe.ast;

public sealed interface UnaryOperator permits UnaryOperator.Arithmetic, UnaryOperator.Logical {

    sealed interface Arithmetic extends UnaryOperator permits Arithmetic.Checked {

        enum Checked implements Arithmetic {
            NEG {
                @Override
                public String toString() {
                    return "-";
                }
            },
        }

    }

    enum Logical implements UnaryOperator {
        NOT {
            @Override
            public String toString() {
                return "!";
            }
        },
    }

}
