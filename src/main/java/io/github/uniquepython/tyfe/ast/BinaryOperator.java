package io.github.uniquepython.tyfe.ast;

public sealed interface BinaryOperator
        permits BinaryOperator.Arithmetic, BinaryOperator.Comparison, BinaryOperator.Logical {

    sealed interface Arithmetic extends BinaryOperator permits Arithmetic.Checked {

        enum Checked implements Arithmetic {
            ADD,
            SUB,
            MUL,
            DIV,
            MOD,
            EXP,
        }

    }

    enum Comparison implements BinaryOperator {
        EQ,
        NEQ,
        LT,
        LTE,
    }

    enum Logical implements BinaryOperator {
        AND,
        OR,
    }

}
