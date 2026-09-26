package io.github.uniquepython.tyfe.literal;

import io.github.uniquepython.tyfe.common.Span;
import io.github.uniquepython.tyfe.common.TyfeError;

public abstract sealed class LiteralizerError extends TyfeError
        permits LiteralizerError.IntegerOverflow,
        LiteralizerError.FloatOverflow,
        LiteralizerError.SuffixShapeMismatch,
        LiteralizerError.NonAsciiCharLiteral {

    protected LiteralizerError(String message, Span span) {
        super(message, span);
    }

    public static final class IntegerOverflow extends LiteralizerError {

        private final String text;

        public IntegerOverflow(String text, Span span) {
            super("Integer literal too large: %s".formatted(text), span);
            this.text = text;
        }

        public String text() {
            return text;
        }

    }

    public static final class FloatOverflow extends LiteralizerError {

        private final String text;

        public FloatOverflow(String text, Span span) {
            super("Float literal too large to represent: %s".formatted(text), span);
            this.text = text;
        }

        public String text() {
            return text;
        }

    }

    public static final class SuffixShapeMismatch extends LiteralizerError {

        private final String suffix;

        public SuffixShapeMismatch(String suffix, Span span) {
            super("Suffix '%s' does not match the literal's shape".formatted(suffix), span);
            this.suffix = suffix;
        }

        public String suffix() {
            return suffix;
        }

    }

    public static final class NonAsciiCharLiteral extends LiteralizerError {

        private final char character;

        public NonAsciiCharLiteral(char character, Span span) {
            super("Character literal is not ASCII: '%c' (code point %d)".formatted(character, (int) character), span);
            this.character = character;
        }

        public char character() {
            return character;
        }

    }

}
