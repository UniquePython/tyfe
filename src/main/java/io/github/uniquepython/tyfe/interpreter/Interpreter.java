package io.github.uniquepython.tyfe.interpreter;

import io.github.uniquepython.tyfe.ast.Expr;
import io.github.uniquepython.tyfe.common.Span;
import io.github.uniquepython.tyfe.ast.BinaryOperator;

public final class Interpreter {

    private Interpreter() {
    }

    public static RuntimeValue evaluate(Expr expr) {
        return switch (expr) {
            case Expr.Literal literal -> RuntimeValue.fromLiteral(literal.value());
            default -> throw new UnsupportedOperationException("not yet implemented: " + expr);
        };
    }

    private static RuntimeValue evaluateBinary(Expr.Binary binary) {
        if (binary.operator() instanceof BinaryOperator.Logical logical) {
            return evaluateLogical(logical, binary.left(), binary.right());
        }

        RuntimeValue left = evaluate(binary.left());
        RuntimeValue right = evaluate(binary.right());

        return switch (binary.operator()) {
            case BinaryOperator.Arithmetic arithmetic -> evaluateArithmetic(arithmetic, left, right, binary.span());
            case BinaryOperator.Comparison comparison -> evaluateComparison(comparison, left, right);
            case BinaryOperator.Logical _ -> throw new IllegalStateException("unreachable: handled above");
        };
    }

    private static RuntimeValue evaluateLogical(BinaryOperator.Logical operator, Expr leftExpr, Expr rightExpr) {
        boolean left = ((RuntimeValue.BoolValue) evaluate(leftExpr)).value();

        return switch (operator) {
            case AND -> left ? evaluate(rightExpr) : new RuntimeValue.BoolValue(false);
            case OR -> left ? new RuntimeValue.BoolValue(true) : evaluate(rightExpr);
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
