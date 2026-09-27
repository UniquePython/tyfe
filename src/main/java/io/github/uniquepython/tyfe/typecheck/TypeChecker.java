package io.github.uniquepython.tyfe.typecheck;

import io.github.uniquepython.tyfe.ast.Expr;
import io.github.uniquepython.tyfe.ast.Stmt;
import io.github.uniquepython.tyfe.ast.Type;
import io.github.uniquepython.tyfe.literal.LiteralValue;
import io.github.uniquepython.tyfe.ast.UnaryOperator;
import io.github.uniquepython.tyfe.common.Span;

import java.util.List;

import io.github.uniquepython.tyfe.ast.BinaryOperator;

public final class TypeChecker {

    private TypeChecker() {
    }

    public static Type checkExpr(Expr expr, TypeCheckContext ctx) {
        return switch (expr) {
            case Expr.Literal literal -> checkLiteral(literal);
            case Expr.Identifier identifier -> checkIdentifier(identifier, ctx);
            case Expr.Unary unary -> checkUnary(unary, ctx);
            case Expr.Binary binary -> checkBinary(binary, ctx);
            case Expr.Block block -> checkBlock(block, ctx);
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

    private static Type checkBinary(Expr.Binary binary, TypeCheckContext ctx) {
        Type leftType = checkExpr(binary.left(), ctx);
        Type rightType = checkExpr(binary.right(), ctx);

        if (leftType != rightType) {
            throw new TypeCheckError.OperandTypeMismatch(binary.operator(), leftType, rightType, binary.span());
        }

        return switch (binary.operator()) {
            case BinaryOperator.Arithmetic arithmetic -> checkArithmetic(arithmetic, leftType, binary.span());
            case BinaryOperator.Comparison comparison -> checkComparison(comparison, leftType, binary.span());
            case BinaryOperator.Logical logical -> checkLogical(logical, leftType, binary.span());
        };
    }

    private static Type checkArithmetic(BinaryOperator.Arithmetic operator, Type operandType, Span span) {
        if (operandType != Type.Primitive.I32 && operandType != Type.Primitive.F64) {
            throw new TypeCheckError.InvalidOperandType(operator, operandType, span);
        }
        return operandType;
    }

    private static Type checkComparison(BinaryOperator.Comparison operator, Type operandType, Span span) {
        boolean orderingOp = operator == BinaryOperator.Comparison.LT || operator == BinaryOperator.Comparison.LTE;

        if (orderingOp && operandType != Type.Primitive.I32 && operandType != Type.Primitive.F64
                && operandType != Type.Primitive.CHAR) {
            throw new TypeCheckError.InvalidOperandType(operator, operandType, span);
        }

        return Type.Primitive.BOOL;
    }

    private static Type checkLogical(BinaryOperator.Logical operator, Type operandType, Span span) {
        if (operandType != Type.Primitive.BOOL) {
            throw new TypeCheckError.InvalidOperandType(operator, operandType, span);
        }
        return Type.Primitive.BOOL;
    }

    private static Type checkBlock(Expr.Block block, TypeCheckContext ctx) {
        TypeCheckContext blockCtx = ctx.withNewScope();

        Type blockType = Type.Nothing.NOTHING;
        boolean seenTerminator = false;

        List<Stmt> statements = block.statements();
        for (int i = 0; i < statements.size(); i++) {
            Stmt statement = statements.get(i);

            if (seenTerminator) {
                throw new TypeCheckError.UnreachableCode(statement.span());
            }

            Type stmtType = checkStmt(statement, blockCtx);

            if (isTerminator(statement)) {
                seenTerminator = true;
            }

            if (blockType == Type.Nothing.NOTHING && stmtType != Type.Nothing.NOTHING) {
                blockType = stmtType;
            }
        }

        return blockType;
    }

    private static boolean isTerminator(Stmt statement) {
        return statement instanceof Stmt.Produce || statement instanceof Stmt.Stop || statement instanceof Stmt.Skip;
    }

}
