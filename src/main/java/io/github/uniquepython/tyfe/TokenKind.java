package io.github.uniquepython.tyfe;

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
        I32,
        F64,
        BOOL,
        CHAR,
        NOTHING,
        YES,
        NO,
    }

    enum Literal implements TokenKind {
        INTEGER,
        FLOAT,
        CHAR,
    }

    enum Identifier implements TokenKind {
        IDENT,
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
                EXP
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
                NGTE,
            }

        }

        enum Logical implements Operator {
            AND,
            OR,
            NOT
        }

        enum Assignment implements Operator {
            ASSIGN,
        }

    }

    enum Punctuation implements TokenKind {
        LPAREN,
        RPAREN,
        LBRACE,
        RBRACE,
        SEMI_COLON,
    }

    enum Sentinel implements TokenKind {
        EOF,
    }

}
