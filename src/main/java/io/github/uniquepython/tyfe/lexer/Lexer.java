package io.github.uniquepython.tyfe.lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.uniquepython.tyfe.common.Span;

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

    private static final Map<String, TokenKind.Keyword> keywords = Map.ofEntries(
            Map.entry("mut", TokenKind.Keyword.MUT),
            Map.entry("const", TokenKind.Keyword.CONST),
            Map.entry("if", TokenKind.Keyword.IF),
            Map.entry("else", TokenKind.Keyword.ELSE),
            Map.entry("while", TokenKind.Keyword.WHILE),
            Map.entry("stop", TokenKind.Keyword.STOP),
            Map.entry("skip", TokenKind.Keyword.SKIP),
            Map.entry("produce", TokenKind.Keyword.PRODUCE),
            Map.entry("i32", TokenKind.Keyword.I32),
            Map.entry("f64", TokenKind.Keyword.F64),
            Map.entry("bool", TokenKind.Keyword.BOOL),
            Map.entry("char", TokenKind.Keyword.CHAR),
            Map.entry("nothing", TokenKind.Keyword.NOTHING),
            Map.entry("yes", TokenKind.Keyword.YES),
            Map.entry("no", TokenKind.Keyword.NO));

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

    private boolean isWhitespace(char c) {
        return switch (c) {
            case ' ', '\t', '\n', '\r', '\f' -> true;
            default -> false;
        };
    }

    private void skipWhitespaceAndComments() {
        while (!isAtEnd()) {
            char c = peek();

            if (isWhitespace(c)) {
                advance();

            } else if (c == '/' && peek(1) == '/') {
                while (!isAtEnd() && peek() != '\n') // consume until newline or EOF
                    advance();

            } else {
                break; // neither whitespace nor a comment start
            }
        }
    }

    private boolean isIdentifierStart(char c) {
        return c == '_' || ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z');
    }

    private boolean isIdentifierBody(char c) {
        return isIdentifierStart(c) || ('0' <= c && c <= '9');
    }

    private Token lexIdentifierOrKeyword() {
        int start = position;

        advance();

        while (isIdentifierBody(peek()))
            advance();

        if (peek() == '?')
            advance();

        String word = source.substring(start, position);
        TokenKind kind = keywords.get(word);

        if (kind == null)
            kind = TokenKind.Identifier.IDENT;

        return new Token(kind, new Span(start, position));
    }

    private void expectSuffix(int start) {
        if (source.startsWith("i32", position) || source.startsWith("f64", position)) {
            advance();
            advance();
            advance();
            return;
        }

        while (isIdentifierBody(peek()))
            advance();

        throw new LexerError.InvalidNumberLiteral(source.substring(start, position), new Span(start, position));
    }

    private boolean isDigit(char c) {
        return '0' <= c && c <= '9';
    }

    private boolean isBinDigit(char c) {
        return '0' <= c && c <= '1';
    }

    private boolean isOctDigit(char c) {
        return '0' <= c && c <= '7';
    }

    private boolean isHexDigit(char c) {
        return ('0' <= c && c <= '9') || ('A' <= c && c <= 'F');
    }

    private Token lexHexOrBinOrOct(int start) {
        advance(); // consume '0'
        char baseChar = advance(); // consume 'x' / 'b' / 'o'

        boolean hasDigit = switch (baseChar) {
            case 'x' -> isHexDigit(peek());
            case 'b' -> isBinDigit(peek());
            case 'o' -> isOctDigit(peek());
            default -> false;
        };

        if (!hasDigit)
            throw new LexerError.InvalidNumberLiteral(source.substring(start, position), new Span(start, position));

        while (switch (baseChar) {
            case 'x' -> isHexDigit(peek()) || peek() == '_';
            case 'b' -> isBinDigit(peek()) || peek() == '_';
            case 'o' -> isOctDigit(peek()) || peek() == '_';
            default -> false;
        }) {
            advance();
        }

        if (peek() != '#')
            throw new LexerError.InvalidNumberLiteral(source.substring(start, position), new Span(start, position));

        advance(); // consume '#'

        expectSuffix(start);

        return new Token(TokenKind.Literal.INTEGER, new Span(start, position));
    }

    private void consumeDecimalDigitRun() {
        if (!isDigit(peek()))
            throw new IllegalStateException("Expected decimal digit, but got: " + peek());

        while (isDigit(peek()) || peek() == '_')
            advance();
    }

    private Token lexDecimalOrFloat(int start) {
        consumeDecimalDigitRun();

        boolean isFloat = false;

        if (peek() == '.') {
            isFloat = true;
            advance(); // consume '.'

            if (!isDigit(peek()))
                throw new LexerError.InvalidNumberLiteral(source.substring(start, position), new Span(start, position));

            consumeDecimalDigitRun();
        }

        if (peek() != '#')
            throw new LexerError.InvalidNumberLiteral(source.substring(start, position), new Span(start, position));

        advance(); // consume '#'
        expectSuffix(start);

        TokenKind kind = isFloat ? TokenKind.Literal.FLOAT : TokenKind.Literal.INTEGER;

        return new Token(kind, new Span(start, position));
    }

    private Token lexNumber() {
        int start = position;

        if (peek() == '0' && (peek(1) == 'x' || peek(1) == 'b' || peek(1) == 'o'))
            return lexHexOrBinOrOct(start);

        return lexDecimalOrFloat(start);
    }

    private Token lexChar() {
        int start = position;

        advance(); // consume opening '\''

        if (isAtEnd() || peek() == '\n')
            throw new LexerError.UnterminatedCharLiteral(new Span(start, position));

        if (peek() == '\'') {
            advance(); // consume closing '\''

            throw new LexerError.InvalidCharacterLiteral(source.substring(start, position), new Span(start, position));
        }

        if (peek() == '\\') {
            advance(); // consume '\'

            if (isAtEnd() || peek() == '\n')
                throw new LexerError.UnterminatedCharLiteral(new Span(start, position));

            char escape = peek();

            if (escape != 'n' && escape != 't' && escape != 'r' && escape != '\\' && escape != '\'' && escape != '0')
                throw new LexerError.InvalidEscapeSequence(source.substring(position - 1, position + 1),
                        new Span(position - 1, position + 1));

            advance(); // consume escape character
        } else {
            advance(); // consume ordinary character
        }

        if (peek() != '\'')
            throw new LexerError.InvalidCharacterLiteral(source.substring(start, position), new Span(start, position));

        advance(); // consume closing '\''

        return new Token(TokenKind.Literal.CHAR, new Span(start, position));
    }

    private Token lexSymbol() {
        int start = position;

        for (int len : new int[] { 3, 2, 1 }) {
            if (position + len > source.length())
                continue;

            String candidate = source.substring(position, position + len);
            TokenKind kind = symbols.get(candidate);

            if (kind != null) {
                position += len;
                return new Token(kind, new Span(start, position));
            }
        }

        throw new LexerError.IllegalCharacter(String.valueOf(peek()), new Span(start, start + 1));
    }

    private Token nextToken() {
        skipWhitespaceAndComments();

        if (isAtEnd())
            return new Token(TokenKind.Sentinel.EOF, new Span(position, position));

        char c = peek();

        if (isIdentifierStart(c))
            return lexIdentifierOrKeyword();

        if (isDigit(c))
            return lexNumber();

        if (c == '\'')
            return lexChar();

        return lexSymbol();
    }

    public List<Token> lex() {
        List<Token> tokens = new ArrayList<>();

        while (true) {
            Token token = nextToken();
            tokens.add(token);

            if (token.kind() == TokenKind.Sentinel.EOF)
                break;
        }

        return tokens;
    }

}
