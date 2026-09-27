package io.github.uniquepython.tyfe.ast;

public sealed interface BinaryOperator
        permits BinaryOperator.Arithmetic, BinaryOperator.Comparison, BinaryOperator.Logical {

    sealed interface Arithmetic extends BinaryOperator permits Arithmetic.Checked {

        enum Checked implements Arithmetic {
            ADD {
                @Override
                public String toString() {
                    return "+";
                }
            },
            SUB {
                @Override
                public String toString() {
                    return "-";
                }
            },
            MUL {
                @Override
                public String toString() {
                    return "*";
                }
            },
            DIV {
                @Override
                public String toString() {
                    return "/";
                }
            },
            MOD {
                @Override
                public String toString() {
                    return "%";
                }
            },
            EXP {
                @Override
                public String toString() {
                    return "**";
                }
            },
        }

    }

    enum Comparison implements BinaryOperator {
        EQ {
            @Override
            public String toString() {
                return "==";
            }
        },
        NEQ {
            @Override
            public String toString() {
                return "!=";
            }
        },
        LT {
            @Override
            public String toString() {
                return "<";
            }
        },
        LTE {
            @Override
            public String toString() {
                return "<=";
            }
        },
    }

    enum Logical implements BinaryOperator {
        AND {
            @Override
            public String toString() {
                return "&&";
            }
        },
        OR {
            @Override
            public String toString() {
                return "||";
            }
        },
    }

}
