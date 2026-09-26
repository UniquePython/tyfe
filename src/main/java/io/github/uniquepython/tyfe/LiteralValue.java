package io.github.uniquepython.tyfe;

public sealed interface LiteralValue {

    record IntValue(int value) implements LiteralValue {
        @Override
        public String toString() {
            return Integer.toString(value);
        }
    }

    record FloatValue(double value) implements LiteralValue {
        @Override
        public String toString() {
            return Double.toString(value);
        }
    }

    record CharValue(byte value) implements LiteralValue {
        @Override
        public String toString() {
            return switch (value) {
                case '\n' -> "'\\n'";
                case '\t' -> "'\\t'";
                case '\r' -> "'\\r'";
                case '\\' -> "'\\\\'";
                case '\'' -> "'\\''";
                case '\0' -> "'\\0'";
                default -> "'%c'".formatted((char) value);
            };
        }
    }

    record None() implements LiteralValue {
        @Override
        public String toString() {
            return "none";
        }
    }

}
