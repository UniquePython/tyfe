package io.github.uniquepython.tyfe.ast;

public sealed interface UnaryOperator permits UnaryOperator.Arithmetic, UnaryOperator.Logical {

    sealed interface Arithmetic extends UnaryOperator permits Arithmetic.Checked {

        enum Checked implements Arithmetic {
            NEG
        }

    }

    enum Logical implements UnaryOperator {
        NOT
    }

}
