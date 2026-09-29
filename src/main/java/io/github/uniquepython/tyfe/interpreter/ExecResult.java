package io.github.uniquepython.tyfe.interpreter;

public sealed interface ExecResult
        permits ExecResult.Normal, ExecResult.Value, ExecResult.Stop, ExecResult.Skip, ExecResult.Yield {

    record Normal() implements ExecResult {
    }

    record Value(RuntimeValue value) implements ExecResult {
    }

    record Stop() implements ExecResult {
    }

    record Skip() implements ExecResult {
    }

    record Yield(RuntimeValue value) implements ExecResult {
    }

}
