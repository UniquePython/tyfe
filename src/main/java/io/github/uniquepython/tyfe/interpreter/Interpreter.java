package io.github.uniquepython.tyfe.interpreter;

import io.github.uniquepython.tyfe.ast.Expr;

public final class Interpreter {

    private Interpreter() {
    }

    public static RuntimeValue evaluate(Expr expr) {
        return switch (expr) {
            case Expr.Literal literal -> RuntimeValue.fromLiteral(literal.value());
            default -> throw new UnsupportedOperationException("not yet implemented: " + expr);
        };
    }

}
