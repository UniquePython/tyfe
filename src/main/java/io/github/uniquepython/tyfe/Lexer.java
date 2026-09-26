package io.github.uniquepython.tyfe;

import java.util.Map;

public class Lexer {

    private static final Map<String, TokenKind> symbols = Map.ofEntries(
            Map.entry("!<=", TokenKind.Operator.Comparison.Checked.NLTE),
            Map.entry("!>=", TokenKind.Operator.Comparison.Checked.NGTE),
            Map.entry("==", TokenKind.Operator.Comparison.Checked.EQ),
            Map.entry("!=", TokenKind.Operator.Comparison.Checked.NEQ),
            Map.entry("<=", TokenKind.Operator.Comparison.Checked.LTE),
            Map.entry(">=", TokenKind.Operator.Comparison.Checked.GTE),
            Map.entry("!<", TokenKind.Operator.Comparison.Checked.NLT),
            Map.entry("!>", TokenKind.Operator.Comparison.Checked.NGT),
            Map.entry("&&", TokenKind.Operator.Logical.AND),
            Map.entry("||", TokenKind.Operator.Logical.OR),
            Map.entry("**", TokenKind.Operator.Arithmetic.Checked.EXP),
            Map.entry("+", TokenKind.Operator.Arithmetic.Checked.PLUS),
            Map.entry("-", TokenKind.Operator.Arithmetic.Checked.MINUS),
            Map.entry("*", TokenKind.Operator.Arithmetic.Checked.MUL),
            Map.entry("/", TokenKind.Operator.Arithmetic.Checked.DIV),
            Map.entry("%", TokenKind.Operator.Arithmetic.Checked.MOD),
            Map.entry("=", TokenKind.Operator.Assignment.ASSIGN),
            Map.entry("<", TokenKind.Operator.Comparison.Checked.LT),
            Map.entry(">", TokenKind.Operator.Comparison.Checked.GT),
            Map.entry("!", TokenKind.Operator.Logical.NOT),
            Map.entry("(", TokenKind.Punctuation.LPAREN),
            Map.entry(")", TokenKind.Punctuation.RPAREN),
            Map.entry("{", TokenKind.Punctuation.LBRACE),
            Map.entry("}", TokenKind.Punctuation.RBRACE),
            Map.entry(";", TokenKind.Punctuation.SEMI_COLON));

    private final String source;
    private int position;

    public Lexer(String source) {
        this.source = source;
        position = 0;
    }

    private boolean isAtEnd() {
        return position >= source.length();
    }

    private char peek(int offset) {
        if (offset < 0)
            return '\0';

        return position + offset >= source.length() ? '\0' : source.charAt(position + offset);
    }

    private char peek() {
        return peek(0);
    }

    private char advance() {
        if (isAtEnd())
            return '\0';

        return source.charAt(position++);
    }

}
