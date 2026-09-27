package io.github.uniquepython.tyfe.parser;

import io.github.uniquepython.tyfe.common.Span;
import io.github.uniquepython.tyfe.common.TyfeError;
import io.github.uniquepython.tyfe.lexer.TokenKind;

public sealed abstract class ParserError extends TyfeError permits ParserError.UnexpectedToken {

    protected ParserError(String message, Span span) {
        super(message, span);
    }

    public static final class UnexpectedToken extends ParserError {

        private final TokenKind found;
        private final String expected;

        public UnexpectedToken(TokenKind found, String expected, Span span) {
            super("Expected %s, but found %s".formatted(expected, found), span);
            this.found = found;
            this.expected = expected;
        }

        public UnexpectedToken(TokenKind found, TokenKind expected, Span span) {
            super("Expected %s, but found %s".formatted(expected, found), span);
            this.found = found;
            this.expected = expected.toString();
        }

        public TokenKind found() {
            return found;
        }

        public String expected() {
            return expected;
        }

    }

}
