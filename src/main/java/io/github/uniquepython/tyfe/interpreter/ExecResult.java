package io.github.uniquepython.tyfe.interpreter;

public sealed interface ExecResult permits ExecResult.Normal, ExecResult.Produce, ExecResult.Stop, ExecResult.Skip {

    record Normal() implements ExecResult {
    }

    record Produce(RuntimeValue value) implements ExecResult {
    }

    record Stop() implements ExecResult {
    }

    record Skip() implements ExecResult {
    }

}
