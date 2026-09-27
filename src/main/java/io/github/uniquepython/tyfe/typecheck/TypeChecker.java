package io.github.uniquepython.tyfe.typecheck;

import io.github.uniquepython.tyfe.ast.Expr;
import io.github.uniquepython.tyfe.ast.Type;
import io.github.uniquepython.tyfe.literal.LiteralValue;
import io.github.uniquepython.tyfe.ast.UnaryOperator;

public final class TypeChecker {

    private TypeChecker() {
    }

    public static Type checkExpr(Expr expr, TypeCheckContext ctx) {
        return switch (expr) {
            case Expr.Literal literal -> checkLiteral(literal);
            case Expr.Identifier identifier -> checkIdentifier(identifier, ctx);
            case Expr.Unary unary -> checkUnary(unary, ctx);
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

    private static Type checkIdentifier(Expr.Identifier identifier, TypeCheckContext ctx) {
        return ctx.env().resolve(identifier.name()).map(VariableInfo::type)
                .orElseThrow(() -> new TypeCheckError.UndeclaredIdentifier(identifier.name(), identifier.span()));
    }

    private static Type checkUnary(Expr.Unary unary, TypeCheckContext ctx) {
        Type operandType = checkExpr(unary.operand(), ctx);

        return switch (unary.operator()) {
            case UnaryOperator.Arithmetic _ -> {
                if (operandType != Type.Primitive.I32 && operandType != Type.Primitive.F64) {
                    throw new TypeCheckError.InvalidOperandType(unary.operator(), operandType, unary.span());
                }
                yield operandType;
            }
            case UnaryOperator.Logical _ -> {
                if (operandType != Type.Primitive.BOOL) {
                    throw new TypeCheckError.InvalidOperandType(unary.operator(), operandType, unary.span());
                }
                yield Type.Primitive.BOOL;
            }
        };
    }

}
