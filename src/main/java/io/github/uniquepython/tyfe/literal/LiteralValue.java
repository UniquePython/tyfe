package io.github.uniquepython.tyfe.literal;

public sealed interface LiteralValue permits LiteralValue.IntValue, LiteralValue.FloatValue, LiteralValue.CharValue,
        LiteralValue.BoolValue, LiteralValue.IdentValue, LiteralValue.None {

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

    record BoolValue(boolean value) implements LiteralValue {
        @Override
        public String toString() {
            return Boolean.toString(value);
        }
    }

    record IdentValue(String name) implements LiteralValue {
        @Override
        public String toString() {
            return name;
        }
    }

    record None() implements LiteralValue {
        @Override
        public String toString() {
            return "none";
        }
    }

}
