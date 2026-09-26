package io.github.uniquepython.tyfe.lexer;

import io.github.uniquepython.tyfe.common.Span;
import io.github.uniquepython.tyfe.common.TyfeError;

public abstract sealed class LexerError extends TyfeError
        permits LexerError.IllegalCharacter,
        LexerError.UnterminatedCharLiteral,
        LexerError.InvalidEscapeSequence,
        LexerError.InvalidCharacterLiteral,
        LexerError.InvalidNumberLiteral {

    protected LexerError(String message, Span span) {
        super(message, span);
    }

    public static final class IllegalCharacter extends LexerError {

        private final String character;

        public IllegalCharacter(String character, Span span) {
            super("Illegal character: %s".formatted(character), span);
            this.character = character;
        }

        public String character() {
            return character;
        }

    }

    public static final class UnterminatedCharLiteral extends LexerError {

        public UnterminatedCharLiteral(Span span) {
            super("Unterminated character literal", span);
        }

    }

    public static final class InvalidEscapeSequence extends LexerError {

        private final String escape;

        public InvalidEscapeSequence(String escape, Span span) {
            super("Invalid escape sequence: %s".formatted(escape), span);
            this.escape = escape;
        }

        public String escape() {
            return escape;
        }

    }

    public static final class InvalidCharacterLiteral extends LexerError {

        private final String characterLiteral;

        public InvalidCharacterLiteral(String characterLiteral, Span span) {
            super("Invalid character literal: %s".formatted(characterLiteral), span);
            this.characterLiteral = characterLiteral;
        }

        public String characterLiteral() {
            return characterLiteral;
        }

    }

    public static final class InvalidNumberLiteral extends LexerError {

        private final String text;

        public InvalidNumberLiteral(String text, Span span) {
            super("Invalid number literal: %s".formatted(text), span);
            this.text = text;
        }

        public String text() {
            return text;
        }

    }

}
