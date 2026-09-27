package io.github.uniquepython.tyfe.interpreter;

import io.github.uniquepython.tyfe.literal.LiteralValue;

public sealed interface RuntimeValue permits RuntimeValue.IntValue, RuntimeValue.FloatValue,
        RuntimeValue.BoolValue, RuntimeValue.CharValue, RuntimeValue.NothingValue {

    record IntValue(int value) implements RuntimeValue {
        @Override
        public String toString() {
            return Integer.toString(value);
        }
    }

    record FloatValue(double value) implements RuntimeValue {
        @Override
        public String toString() {
            return Double.toString(value);
        }
    }

    record BoolValue(boolean value) implements RuntimeValue {
        @Override
        public String toString() {
            return Boolean.toString(value);
        }
    }

    record CharValue(byte value) implements RuntimeValue {
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

    record NothingValue() implements RuntimeValue {
        @Override
        public String toString() {
            return "nothing";
        }
    }

    static RuntimeValue fromLiteral(LiteralValue value) {
        return switch (value) {
            case LiteralValue.IntValue(int v) -> new IntValue(v);
            case LiteralValue.FloatValue(double v) -> new FloatValue(v);
            case LiteralValue.BoolValue(boolean v) -> new BoolValue(v);
            case LiteralValue.CharValue(byte v) -> new CharValue(v);
            case LiteralValue.IdentValue ident ->
                throw new IllegalStateException("unreachable: cannot convert identifier to a runtime value: " + ident);
            case LiteralValue.None _ ->
                throw new IllegalStateException("unreachable: cannot convert None to a runtime value");
        };
    }

}
