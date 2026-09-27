package io.github.uniquepython.tyfe.interpreter;

import io.github.uniquepython.tyfe.ast.Expr;
import io.github.uniquepython.tyfe.common.Span;
import io.github.uniquepython.tyfe.ast.BinaryOperator;
import io.github.uniquepython.tyfe.ast.UnaryOperator;

public final class Interpreter {

    private Interpreter() {
    }

    public static RuntimeValue evaluate(Expr expr, Environment env) {
        return switch (expr) {
            case Expr.Literal literal -> RuntimeValue.fromLiteral(literal.value());
            case Expr.Identifier identifier -> evaluateIdentifier(identifier, env);
            case Expr.Unary unary -> evaluateUnary(unary, env);
            case Expr.Binary binary -> evaluateBinary(binary, env);
            default -> throw new UnsupportedOperationException("not yet implemented: " + expr);
        };
    }

    private static RuntimeValue evaluateIdentifier(Expr.Identifier identifier, Environment env) {
        return env.resolve(identifier.name()).orElseThrow(() -> new IllegalStateException(
                "unreachable: undeclared identifier, typechecker should have caught this: " + identifier.name()));
    }

    private static RuntimeValue evaluateUnary(Expr.Unary unary, Environment env) {
        RuntimeValue operand = evaluate(unary.operand(), env);

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

        RuntimeValue left = evaluate(binary.left(), env);
        RuntimeValue right = evaluate(binary.right(), env);

        return switch (binary.operator()) {
            case BinaryOperator.Arithmetic arithmetic -> evaluateArithmetic(arithmetic, left, right, binary.span());
            case BinaryOperator.Comparison comparison -> evaluateComparison(comparison, left, right);
            case BinaryOperator.Logical _ -> throw new IllegalStateException("unreachable: handled above");
        };
    }

    private static RuntimeValue evaluateLogical(BinaryOperator.Logical operator, Expr leftExpr, Expr rightExpr,
            Environment env) {
        boolean left = ((RuntimeValue.BoolValue) evaluate(leftExpr, env)).value();

        return switch (operator) {
            case AND -> left ? evaluate(rightExpr, env) : new RuntimeValue.BoolValue(false);
            case OR -> left ? new RuntimeValue.BoolValue(true) : evaluate(rightExpr, env);
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
        };
    }

}
