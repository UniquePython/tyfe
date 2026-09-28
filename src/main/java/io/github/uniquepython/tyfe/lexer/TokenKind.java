package io.github.uniquepython.tyfe.lexer;

public sealed interface TokenKind
        permits TokenKind.Keyword, TokenKind.Literal, TokenKind.Identifier, TokenKind.Operator, TokenKind.Punctuation,
        TokenKind.Sentinel {

    enum Keyword implements TokenKind {
        MUT,
        CONST,
        IF,
        ELSE,
        WHILE,
        STOP,
        SKIP,
        PRODUCE,
        YIELD,
        I32,
        F64,
        BOOL,
        CHAR,
        NOTHING;

        @Override
        public String toString() {
            return "keyword %s".formatted(this.name().toLowerCase());
        }
    }

    enum Literal implements TokenKind {
        INTEGER,
        FLOAT,
        CHAR,
        BOOL;

        @Override
        public String toString() {
            return "%s literal".formatted(this.name().toLowerCase());
        }
    }

    enum Identifier implements TokenKind {
        IDENT;

        @Override
        public String toString() {
            return "identifier";
        }
    }

    sealed interface Operator extends TokenKind
            permits Operator.Arithmetic, Operator.Comparison, Operator.Logical, Operator.Assignment {

        sealed interface Arithmetic extends Operator permits Arithmetic.Checked {

            enum Checked implements Arithmetic {
                PLUS,
                MINUS,
                MUL,
                DIV,
                MOD,
                EXP;

                @Override
                public String toString() {
                    return switch (this) {
                        case PLUS -> "+";
                        case MINUS -> "-";
                        case MUL -> "*";
                        case DIV -> "/";
                        case MOD -> "%";
                        case EXP -> "**";
                    };
                }
            }

        }

        sealed interface Comparison extends Operator permits Comparison.Checked {

            enum Checked implements Comparison {
                EQ,
                NEQ,
                LT,
                GT,
                LTE,
                GTE,
                NLT,
                NGT,
                NLTE,
                NGTE;

                @Override
                public String toString() {
                    return switch (this) {
                        case EQ -> "==";
                        case NEQ -> "!=";
                        case LT -> "<";
                        case GT -> ">";
                        case LTE -> "<=";
                        case GTE -> ">=";
                        case NLT -> "!<";
                        case NGT -> "!>";
                        case NLTE -> "!<=";
                        case NGTE -> "!>=";
                    };
                }
            }

        }

        enum Logical implements Operator {
            AND,
            OR,
            NOT;

            @Override
            public String toString() {
                return switch (this) {
                    case AND -> "&&";
                    case OR -> "||";
                    case NOT -> "!";
                };
            }
        }

        enum Assignment implements Operator {
            ASSIGN;

            @Override
            public String toString() {
                return "=";
            }
        }

    }

    enum Punctuation implements TokenKind {
        LPAREN,
        RPAREN,
        LBRACE,
        RBRACE,
        SEMI_COLON;

        @Override
        public String toString() {
            return switch (this) {
                case LPAREN -> "(";
                case RPAREN -> ")";
                case LBRACE -> "{";
                case RBRACE -> "}";
                case SEMI_COLON -> ";";
            };
        }
    }

    enum Sentinel implements TokenKind {
        EOF;

        @Override
        public String toString() {
            return "end of file";
        }
    }

}
