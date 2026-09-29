package io.github.uniquepython.tyfe.typecheck;

import io.github.uniquepython.tyfe.ast.Expr;
import io.github.uniquepython.tyfe.ast.Mutability;
import io.github.uniquepython.tyfe.ast.Stmt;
import io.github.uniquepython.tyfe.ast.Type;
import io.github.uniquepython.tyfe.literal.LiteralValue;
import io.github.uniquepython.tyfe.ast.UnaryOperator;
import io.github.uniquepython.tyfe.common.Span;

import java.util.List;

import io.github.uniquepython.tyfe.ast.AssignmentTarget;
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
            case Expr.If ifExpr -> checkIf(ifExpr, ctx);
            case Expr.While whileExpr -> checkWhile(whileExpr, ctx);
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
        return statement instanceof Stmt.Produce || statement instanceof Stmt.Stop || statement instanceof Stmt.Skip
                || statement instanceof Stmt.Yield;
    }

    private static Type checkIf(Expr.If ifExpr, TypeCheckContext ctx) {
        Type conditionType = checkExpr(ifExpr.condition(), ctx);

        if (conditionType != Type.Primitive.BOOL) {
            throw new TypeCheckError.NonBooleanCondition(conditionType, ifExpr.condition().span());
        }

        Type thenType = checkBlock(ifExpr.thenBranch(), ctx);
        Type elseType = checkElseBranch(ifExpr.elseBranch(), ctx);

        if (thenType != elseType) {
            throw new TypeCheckError.BranchTypeMismatch(thenType, elseType, ifExpr.span());
        }

        return thenType;
    }

    private static Type checkElseBranch(Expr.ElseBranch elseBranch, TypeCheckContext ctx) {
        return switch (elseBranch) {
            case Expr.Block block -> checkBlock(block, ctx);
            case Expr.If ifExpr -> checkIf(ifExpr, ctx);
        };
    }

    private static Type checkWhile(Expr.While whileExpr, TypeCheckContext ctx) {
        Type conditionType = checkExpr(whileExpr.condition(), ctx);

        if (conditionType != Type.Primitive.BOOL) {
            throw new TypeCheckError.NonBooleanCondition(conditionType, whileExpr.condition().span());
        }

        TypeCheckContext bodyCtx = ctx.withInsideLoop();
        checkBlock(whileExpr.body(), bodyCtx);

        YieldReachability reachability = analyzeYieldReachability(whileExpr.body(), bodyCtx);

        if (reachability.reachableYieldSpans().isEmpty()) {
            return Type.Nothing.NOTHING;
        }

        Type yieldType = reachability.yieldType();
        Span firstSpan = reachability.reachableYieldSpans().getFirst();

        if (anyReachableBareStop(whileExpr.body())) {
            throw new TypeCheckError.BareStopWithYield(firstSpan);
        }

        if (!reachability.allPathsYield()) {
            throw new TypeCheckError.LoopMayNotYield(yieldType, whileExpr.span());
        }

        return yieldType;
    }

    private record YieldReachability(boolean allPathsYield, Type yieldType, List<Span> reachableYieldSpans) {
    }

    private static YieldReachability analyzeYieldReachability(Expr.Block block, TypeCheckContext ctx) {
        TypeCheckContext blockCtx = ctx.withNewScope();
        List<Stmt> statements = block.statements();

        if (statements.isEmpty()) {
            return new YieldReachability(false, null, List.of());
        }

        Stmt last = statements.getLast();

        return switch (last) {
            case Stmt.Yield yield -> {
                Type yieldType = checkExpr(yield.value(), blockCtx);
                yield new YieldReachability(true, yieldType, List.of(yield.span()));
            }
            case Stmt.ExpressionStatement exprStmt when exprStmt.expression() instanceof Expr.If ifExpr ->
                mergeBranches(analyzeYieldReachability(ifExpr.thenBranch(), blockCtx),
                        analyzeElseBranch(ifExpr.elseBranch(), blockCtx), ifExpr.span());
            default -> new YieldReachability(false, null, List.of());
        };
    }

    private static YieldReachability analyzeElseBranch(Expr.ElseBranch elseBranch, TypeCheckContext ctx) {
        return switch (elseBranch) {
            case Expr.Block block -> analyzeYieldReachability(block, ctx);
            case Expr.If ifExpr -> mergeBranches(analyzeYieldReachability(ifExpr.thenBranch(), ctx),
                    analyzeElseBranch(ifExpr.elseBranch(), ctx), ifExpr.span());
        };
    }

    private static YieldReachability mergeBranches(YieldReachability thenInfo, YieldReachability elseInfo,
            Span span) {
        List<Span> allSpans = new java.util.ArrayList<>(thenInfo.reachableYieldSpans());
        allSpans.addAll(elseInfo.reachableYieldSpans());

        Type unifiedType = thenInfo.yieldType() != null ? thenInfo.yieldType() : elseInfo.yieldType();

        if (thenInfo.yieldType() != null && elseInfo.yieldType() != null
                && thenInfo.yieldType() != elseInfo.yieldType()) {
            throw new TypeCheckError.YieldTypeMismatch(thenInfo.yieldType(),
                    thenInfo.reachableYieldSpans().getFirst(), elseInfo.yieldType(),
                    elseInfo.reachableYieldSpans().getFirst());
        }

        return new YieldReachability(thenInfo.allPathsYield() && elseInfo.allPathsYield(), unifiedType, allSpans);
    }

    private static boolean anyReachableBareStop(Expr.Block block) {
        for (Stmt statement : block.statements()) {
            if (statement instanceof Stmt.Stop) {
                return true;
            }
            if (statement instanceof Stmt.ExpressionStatement exprStmt
                    && exprStmt.expression() instanceof Expr.If ifExpr && anyReachableBareStopInIf(ifExpr)) {
                return true;
            }
        }
        return false;
    }

    private static boolean anyReachableBareStopInIf(Expr.If ifExpr) {
        if (anyReachableBareStop(ifExpr.thenBranch())) {
            return true;
        }
        return switch (ifExpr.elseBranch()) {
            case Expr.Block block -> anyReachableBareStop(block);
            case Expr.If elseIf -> anyReachableBareStopInIf(elseIf);
        };
    }

    public static Type checkStmt(Stmt stmt, TypeCheckContext ctx) {
        return switch (stmt) {
            case Stmt.Declaration declaration -> checkDeclaration(declaration, ctx);
            case Stmt.Assignment assignment -> checkAssignment(assignment, ctx);
            case Stmt.ExpressionStatement expressionStatement -> checkExpressionStatement(expressionStatement, ctx);
            case Stmt.Produce produce -> checkProduce(produce, ctx);
            case Stmt.Stop stop -> checkStop(stop, ctx);
            case Stmt.Skip skip -> checkSkip(skip, ctx);
            case Stmt.Yield yieldStmt -> checkYield(yieldStmt, ctx);

        };
    }

    private static Type checkExpressionStatement(Stmt.ExpressionStatement stmt, TypeCheckContext ctx) {
        checkExpr(stmt.expression(), ctx);
        return Type.Nothing.NOTHING;
    }

    private static Type checkProduce(Stmt.Produce stmt, TypeCheckContext ctx) {
        if (!ctx.insideBlock()) {
            throw new TypeCheckError.ProduceOutsideBlock(stmt.span());
        }
        return checkExpr(stmt.value(), ctx);
    }

    private static Type checkStop(Stmt.Stop stmt, TypeCheckContext ctx) {
        if (!ctx.insideLoop()) {
            throw new TypeCheckError.StopOrSkipOutsideLoop("stop", stmt.span());
        }
        return Type.Nothing.NOTHING;
    }

    private static Type checkSkip(Stmt.Skip stmt, TypeCheckContext ctx) {
        if (!ctx.insideLoop()) {
            throw new TypeCheckError.StopOrSkipOutsideLoop("skip", stmt.span());
        }
        return Type.Nothing.NOTHING;
    }

    private static Type checkYield(Stmt.Yield stmt, TypeCheckContext ctx) {
        if (!ctx.insideLoop()) {
            throw new TypeCheckError.YieldOutsideLoop(stmt.span());
        }
        return checkExpr(stmt.value(), ctx);
    }

    private static Type checkDeclaration(Stmt.Declaration stmt, TypeCheckContext ctx) {
        Type initializerType = checkExpr(stmt.initializer(), ctx);

        if (initializerType != stmt.type()) {
            throw new TypeCheckError.DeclarationTypeMismatch(stmt.type(), initializerType, stmt.span());
        }

        ctx.env().declare(stmt.name(), stmt.type(), stmt.mutability(), stmt.span());

        return Type.Nothing.NOTHING;
    }

    private static Type checkAssignment(Stmt.Assignment stmt, TypeCheckContext ctx) {
        String name = ((AssignmentTarget.Identifier) stmt.target()).name();

        VariableInfo info = ctx.env().resolve(name)
                .orElseThrow(() -> new TypeCheckError.UndeclaredIdentifier(name, stmt.target().span()));

        Type valueType = checkExpr(stmt.value(), ctx);

        if (info.mutability() != Mutability.MUT) {
            throw new TypeCheckError.ReassignmentOfConst(name, stmt.span());
        }

        if (valueType != info.type()) {
            throw new TypeCheckError.AssignmentTypeMismatch(info.type(), valueType, stmt.span());
        }

        return Type.Nothing.NOTHING;
    }

}
