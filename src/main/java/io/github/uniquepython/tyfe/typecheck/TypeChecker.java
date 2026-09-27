package io.github.uniquepython.tyfe.typecheck;

import io.github.uniquepython.tyfe.ast.Expr;
import io.github.uniquepython.tyfe.ast.Type;
import io.github.uniquepython.tyfe.literal.LiteralValue;

public final class TypeChecker {

    private TypeChecker() {
    }

    public static Type checkExpr(Expr expr, TypeCheckContext ctx) {
        return switch (expr) {
            case Expr.Literal literal -> checkLiteral(literal);
            case Expr.Identifier identifier -> throw new UnsupportedOperationException("not yet implemented");
            case Expr.Unary unary -> throw new UnsupportedOperationException("not yet implemented");
            case Expr.Binary binary -> throw new UnsupportedOperationException("not yet implemented");
            case Expr.Block block -> throw new UnsupportedOperationException("not yet implemented");
            case Expr.If ifExpr -> throw new UnsupportedOperationException("not yet implemented");
            case Expr.While whileExpr -> throw new UnsupportedOperationException("not yet implemented");
        };
    }

    private static Type checkLiteral(Expr.Literal literal) {
        return switch (literal.value()) {
            case LiteralValue.IntValue _ -> Type.Primitive.I32;
            case LiteralValue.FloatValue _ -> Type.Primitive.F64;
            case LiteralValue.BoolValue _ -> Type.Primitive.BOOL;
            case LiteralValue.CharValue _ -> Type.Primitive.CHAR;
            case LiteralValue.IdentValue _ ->
                throw new IllegalStateException("unreachable: Expr.Literal cannot wrap an identifier");
            case LiteralValue.None _ -> throw new IllegalStateException("unreachable: Expr.Literal cannot wrap None");
        };
    }

}
