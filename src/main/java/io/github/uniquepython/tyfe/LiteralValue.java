package io.github.uniquepython.tyfe;

public sealed interface LiteralValue
        permits LiteralValue.IntValue, LiteralValue.FloatValue, LiteralValue.CharValue, LiteralValue.None {

    record IntValue(int value) implements LiteralValue {
    }

    record FloatValue(double value) implements LiteralValue {
    }

    record CharValue(byte value) implements LiteralValue {
    }

    record None() implements LiteralValue {
    }

}
