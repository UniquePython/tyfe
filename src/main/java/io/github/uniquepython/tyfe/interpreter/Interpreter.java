package io.github.uniquepython.tyfe.interpreter;

import io.github.uniquepython.tyfe.ast.Expr;
import io.github.uniquepython.tyfe.ast.Stmt;
import io.github.uniquepython.tyfe.common.Span;
import io.github.uniquepython.tyfe.ast.AssignmentTarget;
import io.github.uniquepython.tyfe.ast.BinaryOperator;
import io.github.uniquepython.tyfe.ast.UnaryOperator;

public final class Interpreter {

    private Interpreter() {
    }

    public static ExecResult evaluate(Expr expr, Environment env) {
        return switch (expr) {
            case Expr.Literal literal -> new ExecResult.Value(RuntimeValue.fromLiteral(literal.value()));
            case Expr.Identifier identifier -> new ExecResult.Value(evaluateIdentifier(identifier, env));
            case Expr.Unary unary -> new ExecResult.Value(evaluateUnary(unary, env));
            case Expr.Binary binary -> new ExecResult.Value(evaluateBinary(binary, env));
            case Expr.Block block -> evaluateBlock(block, env);
            case Expr.If ifExpr -> evaluateIf(ifExpr, env);
            case Expr.While whileExpr -> evaluateWhile(whileExpr, env);
        };
    }

    /**
     * Unwraps an ExecResult in an operand position, where the typechecker guarantees evaluation
     * can only ever complete with an ordinary value -- arithmetic/logical operands, if/while
     * conditions, and the value expressions of declarations, assignments, produce and yield.
     * A control-flow signal escaping to one of these positions would mean the typechecker missed
     * a stop/skip/yield in a non-statement position, since those are Stmt variants, not Expr ones.
     */
    private static RuntimeValue evaluateAsValue(Expr expr, Environment env) {
        ExecResult result = evaluate(expr, env);

        return switch (result) {
            case ExecResult.Value value -> value.value();
            case ExecResult.Normal _, ExecResult.Stop _, ExecResult.Skip _, ExecResult.Yield _ ->
                throw new IllegalStateException(
                        "unreachable: control-flow signal escaped an operand position, typechecker should have caught this: "
                                + result);
        };
    }

    private static RuntimeValue evaluateIdentifier(Expr.Identifier identifier, Environment env) {
        return env.resolve(identifier.name()).orElseThrow(() -> new IllegalStateException(
                "unreachable: undeclared identifier, typechecker should have caught this: " + identifier.name()));
    }

    private static RuntimeValue evaluateUnary(Expr.Unary unary, Environment env) {
        RuntimeValue operand = evaluateAsValue(unary.operand(), env);

        return switch (unary.operator()) {
            case UnaryOperator.Arithmetic.Checked.NEG -> evaluateNeg(operand, unary.span());
            case UnaryOperator.Logical.NOT -> {
                boolean value = ((RuntimeValue.BoolValue) operand).value();
                yield new RuntimeValue.BoolValue(!value);
            }
            default -> throw new IllegalStateException("unreachable: unknown unary operator");
        };
    }

    private static RuntimeValue evaluateNeg(RuntimeValue operand, Span span) {
        return switch (operand) {
            case RuntimeValue.IntValue(int v) -> {
                try {
                    yield new RuntimeValue.IntValue(Math.negateExact(v));
                } catch (ArithmeticException e) {
                    throw new TyfeCatastrophe.IntegerOverflow("unary -", span);
                }
            }
            case RuntimeValue.FloatValue(double v) -> new RuntimeValue.FloatValue(-v);
            default -> throw new IllegalStateException("unreachable: unary - on non-numeric type");
        };
    }

    private static RuntimeValue evaluateBinary(Expr.Binary binary, Environment env) {
        if (binary.operator() instanceof BinaryOperator.Logical logical) {
            return evaluateLogical(logical, binary.left(), binary.right(), env);
        }

        RuntimeValue left = evaluateAsValue(binary.left(), env);
        RuntimeValue right = evaluateAsValue(binary.right(), env);

        return switch (binary.operator()) {
            case BinaryOperator.Arithmetic arithmetic -> evaluateArithmetic(arithmetic, left, right, binary.span());
            case BinaryOperator.Comparison comparison -> evaluateComparison(comparison, left, right);
            case BinaryOperator.Logical _ -> throw new IllegalStateException("unreachable: handled above");
        };
    }

    private static RuntimeValue evaluateLogical(BinaryOperator.Logical operator, Expr leftExpr, Expr rightExpr,
            Environment env) {
        boolean left = ((RuntimeValue.BoolValue) evaluateAsValue(leftExpr, env)).value();

        return switch (operator) {
            case AND -> left ? evaluateAsValue(rightExpr, env) : new RuntimeValue.BoolValue(false);
            case OR -> left ? new RuntimeValue.BoolValue(true) : evaluateAsValue(rightExpr, env);
        };
    }

    private static RuntimeValue evaluateArithmetic(BinaryOperator.Arithmetic operator, RuntimeValue left,
            RuntimeValue right, Span span) {
        return switch (left) {
            case RuntimeValue.IntValue(int l) -> {
                int r = ((RuntimeValue.IntValue) right).value();
                yield evaluateIntArithmetic(operator, l, r, span);
            }
            case RuntimeValue.FloatValue(double l) -> {
                double r = ((RuntimeValue.FloatValue) right).value();
                yield evaluateFloatArithmetic(operator, l, r);
            }
            default -> throw new IllegalStateException("unreachable: arithmetic on non-numeric type");
        };
    }

    private static RuntimeValue evaluateIntArithmetic(BinaryOperator.Arithmetic operator, int l, int r, Span span) {
        try {
            return switch (operator) {
                case BinaryOperator.Arithmetic.Checked.ADD -> new RuntimeValue.IntValue(Math.addExact(l, r));
                case BinaryOperator.Arithmetic.Checked.SUB -> new RuntimeValue.IntValue(Math.subtractExact(l, r));
                case BinaryOperator.Arithmetic.Checked.MUL -> new RuntimeValue.IntValue(Math.multiplyExact(l, r));
                case BinaryOperator.Arithmetic.Checked.DIV -> {
                    if (r == 0)
                        throw new TyfeCatastrophe.DivisionByZero(span);
                    if (l == Integer.MIN_VALUE && r == -1)
                        throw new TyfeCatastrophe.IntegerOverflow("/", span);
                    yield new RuntimeValue.IntValue(l / r);
                }
                case BinaryOperator.Arithmetic.Checked.MOD -> {
                    if (r == 0)
                        throw new TyfeCatastrophe.DivisionByZero(span);
                    if (l == Integer.MIN_VALUE && r == -1)
                        throw new TyfeCatastrophe.IntegerOverflow("%", span);
                    yield new RuntimeValue.IntValue(l % r);
                }
                case BinaryOperator.Arithmetic.Checked.EXP -> new RuntimeValue.IntValue(checkedIntPow(l, r, span));
            };
        } catch (ArithmeticException e) {
            throw new TyfeCatastrophe.IntegerOverflow(operator.toString(), span);
        }
    }

    private static int checkedIntPow(int base, int exponent, Span span) {
        if (base == 0 && exponent == 0) {
            throw new TyfeCatastrophe.IndeterminateValue(span);
        }
        if (exponent < 0) {
            throw new TyfeCatastrophe.NegativeExponent(span);
        }

        int result = 1;
        for (int i = 0; i < exponent; i++) {
            result = Math.multiplyExact(result, base);
        }
        return result;
    }

    private static RuntimeValue evaluateFloatArithmetic(BinaryOperator.Arithmetic operator, double l, double r) {
        return switch (operator) {
            case BinaryOperator.Arithmetic.Checked.ADD -> new RuntimeValue.FloatValue(l + r);
            case BinaryOperator.Arithmetic.Checked.SUB -> new RuntimeValue.FloatValue(l - r);
            case BinaryOperator.Arithmetic.Checked.MUL -> new RuntimeValue.FloatValue(l * r);
            case BinaryOperator.Arithmetic.Checked.DIV -> new RuntimeValue.FloatValue(l / r);
            case BinaryOperator.Arithmetic.Checked.MOD -> new RuntimeValue.FloatValue(l % r);
            case BinaryOperator.Arithmetic.Checked.EXP -> new RuntimeValue.FloatValue(Math.pow(l, r));
        };
    }

    private static RuntimeValue evaluateComparison(BinaryOperator.Comparison operator, RuntimeValue left,
            RuntimeValue right) {
        return switch (operator) {
            case EQ -> new RuntimeValue.BoolValue(valuesEqual(left, right));
            case NEQ -> new RuntimeValue.BoolValue(!valuesEqual(left, right));
            case LT -> new RuntimeValue.BoolValue(lessThan(left, right));
            case LTE -> new RuntimeValue.BoolValue(lessThanOrEqual(left, right));
        };
    }

    private static boolean valuesEqual(RuntimeValue left, RuntimeValue right) {
        return switch (left) {
            case RuntimeValue.IntValue(int l) -> l == ((RuntimeValue.IntValue) right).value();
            case RuntimeValue.FloatValue(double l) -> l == ((RuntimeValue.FloatValue) right).value();
            case RuntimeValue.BoolValue(boolean l) -> l == ((RuntimeValue.BoolValue) right).value();
            case RuntimeValue.CharValue(byte l) -> l == ((RuntimeValue.CharValue) right).value();
            case RuntimeValue.NothingValue _ -> true;
        };
    }

    private static boolean lessThan(RuntimeValue left, RuntimeValue right) {
        return switch (left) {
            case RuntimeValue.IntValue(int l) -> l < ((RuntimeValue.IntValue) right).value();
            case RuntimeValue.FloatValue(double l) -> l < ((RuntimeValue.FloatValue) right).value();
            case RuntimeValue.CharValue(byte l) -> l < ((RuntimeValue.CharValue) right).value();
            case RuntimeValue.BoolValue _ ->
                throw new IllegalStateException(
                        "unreachable: bool has no ordering, typechecker should have rejected this");
            case RuntimeValue.NothingValue _ ->
                throw new IllegalStateException(
                        "unreachable: nothing has no ordering, typechecker should have rejected this");
        };
    }

    private static boolean lessThanOrEqual(RuntimeValue left, RuntimeValue right) {
        return switch (left) {
            case RuntimeValue.IntValue(int l) -> l <= ((RuntimeValue.IntValue) right).value();
            case RuntimeValue.FloatValue(double l) -> l <= ((RuntimeValue.FloatValue) right).value();
            case RuntimeValue.CharValue(byte l) -> l <= ((RuntimeValue.CharValue) right).value();
            case RuntimeValue.BoolValue _ ->
                throw new IllegalStateException(
                        "unreachable: bool has no ordering, typechecker should have rejected this");
            case RuntimeValue.NothingValue _ ->
                throw new IllegalStateException(
                        "unreachable: nothing has no ordering, typechecker should have rejected this");
        };
    }

    private static ExecResult evaluateBlock(Expr.Block block, Environment env) {
        Environment blockEnv = env.child();

        for (Stmt stmt : block.statements()) {
            ExecResult result = execute(stmt, blockEnv);

            if (!(result instanceof ExecResult.Normal)) {
                return result;
            }
        }

        return new ExecResult.Normal();
    }

    private static ExecResult evaluateIf(Expr.If ifExpr, Environment env) {
        boolean conditionValue = ((RuntimeValue.BoolValue) evaluateAsValue(ifExpr.condition(), env)).value();

        if (conditionValue) {
            return evaluateBlock(ifExpr.thenBranch(), env);
        } else {
            return evaluateElseBranch(ifExpr.elseBranch(), env);
        }
    }

    private static ExecResult evaluateElseBranch(Expr.ElseBranch elseBranch, Environment env) {
        return switch (elseBranch) {
            case Expr.Block block -> evaluateBlock(block, env);
            case Expr.If ifExpr -> evaluateIf(ifExpr, env);
        };
    }

    private static ExecResult evaluateWhile(Expr.While whileExpr, Environment env) {
        while (true) {
            boolean conditionValue = ((RuntimeValue.BoolValue) evaluateAsValue(whileExpr.condition(), env)).value();

            if (!conditionValue) {
                return new ExecResult.Value(new RuntimeValue.NothingValue());
            }

            ExecResult bodyResult = evaluateBlock(whileExpr.body(), env);

            switch (bodyResult) {
                case ExecResult.Normal _ -> {
                }
                case ExecResult.Skip _ -> {
                }
                case ExecResult.Value _ ->
                    throw new IllegalStateException(
                            "unreachable: loop body block produced a value via trailing produce, but a loop body's value is never observable, typechecker should have caught this: "
                                    + bodyResult);
                case ExecResult.Stop _ -> {
                    return new ExecResult.Value(new RuntimeValue.NothingValue());
                }
                case ExecResult.Yield yieldResult -> {
                    return new ExecResult.Value(yieldResult.value());
                }
            }
        }
    }

    public static ExecResult execute(Stmt stmt, Environment env) {
        return switch (stmt) {
            case Stmt.Declaration declaration -> executeDeclaration(declaration, env);
            case Stmt.Assignment assignment -> executeAssignment(assignment, env);
            case Stmt.ExpressionStatement expressionStatement -> executeExpressionStatement(expressionStatement, env);
            case Stmt.Produce produce -> executeProduce(produce, env);
            case Stmt.Stop stop -> executeStop(stop, env);
            case Stmt.Skip skip -> executeSkip(skip, env);
            case Stmt.Yield yieldStmt -> executeYield(yieldStmt, env);
        };
    }

    private static ExecResult executeDeclaration(Stmt.Declaration stmt, Environment env) {
        RuntimeValue value = evaluateAsValue(stmt.initializer(), env);
        env.declare(stmt.name(), value);
        return new ExecResult.Normal();
    }

    private static ExecResult executeAssignment(Stmt.Assignment stmt, Environment env) {
        String name = ((AssignmentTarget.Identifier) stmt.target()).name();
        RuntimeValue value = evaluateAsValue(stmt.value(), env);
        env.assign(name, value);
        return new ExecResult.Normal();
    }

    private static ExecResult executeExpressionStatement(Stmt.ExpressionStatement stmt, Environment env) {
        ExecResult result = evaluate(stmt.expression(), env);

        return switch (result) {
            case ExecResult.Value _, ExecResult.Normal _ -> new ExecResult.Normal();
            case ExecResult.Stop _, ExecResult.Skip _, ExecResult.Yield _ -> result;
        };
    }

    private static ExecResult executeProduce(Stmt.Produce stmt, Environment env) {
        RuntimeValue value = evaluateAsValue(stmt.value(), env);
        return new ExecResult.Value(value);
    }

    private static ExecResult executeStop(Stmt.Stop stmt, Environment env) {
        return new ExecResult.Stop();
    }

    private static ExecResult executeSkip(Stmt.Skip stmt, Environment env) {
        return new ExecResult.Skip();
    }

    private static ExecResult executeYield(Stmt.Yield stmt, Environment env) {
        RuntimeValue value = evaluateAsValue(stmt.value(), env);
        return new ExecResult.Yield(value);
    }

}
