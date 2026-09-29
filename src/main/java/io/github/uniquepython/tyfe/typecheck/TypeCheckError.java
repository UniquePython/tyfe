package io.github.uniquepython.tyfe.typecheck;

import io.github.uniquepython.tyfe.common.Span;
import io.github.uniquepython.tyfe.common.TyfeError;
import io.github.uniquepython.tyfe.ast.Type;
import io.github.uniquepython.tyfe.ast.UnaryOperator;
import io.github.uniquepython.tyfe.ast.BinaryOperator;

public abstract sealed class TypeCheckError extends TyfeError
        permits TypeCheckError.DuplicateDeclaration, TypeCheckError.UndeclaredIdentifier,
        TypeCheckError.InvalidOperandType, TypeCheckError.OperandTypeMismatch, TypeCheckError.UnreachableCode,
        TypeCheckError.StopOrSkipOutsideLoop, TypeCheckError.ProduceOutsideBlock,
        TypeCheckError.DeclarationTypeMismatch,
        TypeCheckError.AssignmentTypeMismatch,
        TypeCheckError.ReassignmentOfConst, TypeCheckError.NonBooleanCondition, TypeCheckError.BranchTypeMismatch,
        TypeCheckError.YieldOutsideLoop, TypeCheckError.YieldTypeMismatch, TypeCheckError.LoopMayNotYield,
        TypeCheckError.BareStopWithYield, TypeCheckError.ProduceDirectlyInLoopBody {

    protected TypeCheckError(String message, Span span) {
        super(message, span);
    }

    public static final class DuplicateDeclaration extends TypeCheckError {

        private final String name;
        private final Span originalSpan;

        public DuplicateDeclaration(String name, Span originalSpan, Span span) {
            super("Duplicate declaration of '%s' in this scope".formatted(name), span);
            this.name = name;
            this.originalSpan = originalSpan;
        }

        public String name() {
            return name;
        }

        public Span originalSpan() {
            return originalSpan;
        }

    }

    public static final class UndeclaredIdentifier extends TypeCheckError {

        private final String name;

        public UndeclaredIdentifier(String name, Span span) {
            super("Undeclared identifier: '%s'".formatted(name), span);
            this.name = name;
        }

        public String name() {
            return name;
        }

    }

    public static final class InvalidOperandType extends TypeCheckError {

        private final String operator;
        private final Type actualType;

        public InvalidOperandType(UnaryOperator operator, Type type, Span span) {
            this(operator.toString(), type, span);
        }

        public InvalidOperandType(BinaryOperator operator, Type type, Span span) {
            this(operator.toString(), type, span);
        }

        private InvalidOperandType(String operator, Type actualType, Span span) {
            super("Operator '%s' is not defined for type %s".formatted(operator, actualType), span);
            this.operator = operator;
            this.actualType = actualType;
        }

        public String operator() {
            return operator;
        }

        public Type actualType() {
            return actualType;
        }

    }

    public static final class OperandTypeMismatch extends TypeCheckError {

        private final String operator;
        private final Type leftType;
        private final Type rightType;

        public OperandTypeMismatch(BinaryOperator operator, Type leftType, Type rightType, Span span) {
            super("Operator '%s' requires both operands to be the same type, but got %s and %s"
                    .formatted(operator.toString(), leftType, rightType), span);
            this.operator = operator.toString();
            this.leftType = leftType;
            this.rightType = rightType;
        }

        public String operator() {
            return operator;
        }

        public Type leftType() {
            return leftType;
        }

        public Type rightType() {
            return rightType;
        }

    }

    public static final class UnreachableCode extends TypeCheckError {

        public UnreachableCode(Span span) {
            super("Unreachable code", span);
        }

    }

    public static final class StopOrSkipOutsideLoop extends TypeCheckError {

        private final String keyword;

        public StopOrSkipOutsideLoop(String keyword, Span span) {
            super("'%s' used outside of a loop".formatted(keyword), span);
            this.keyword = keyword;
        }

        public String keyword() {
            return keyword;
        }

    }

    public static final class ProduceOutsideBlock extends TypeCheckError {

        public ProduceOutsideBlock(Span span) {
            super("'produce' used outside of a block", span);
        }

    }

    public static final class DeclarationTypeMismatch extends TypeCheckError {

        private final Type declaredType;
        private final Type initializerType;

        public DeclarationTypeMismatch(Type declaredType, Type initializerType, Span span) {
            super("Declared type %s does not match initializer type %s".formatted(declaredType, initializerType), span);
            this.declaredType = declaredType;
            this.initializerType = initializerType;
        }

        public Type declaredType() {
            return declaredType;
        }

        public Type initializerType() {
            return initializerType;
        }

    }

    public static final class AssignmentTypeMismatch extends TypeCheckError {

        private final Type targetType;
        private final Type valueType;

        public AssignmentTypeMismatch(Type targetType, Type valueType, Span span) {
            super("Cannot assign value of type %s to variable of type %s".formatted(valueType, targetType), span);
            this.targetType = targetType;
            this.valueType = valueType;
        }

        public Type targetType() {
            return targetType;
        }

        public Type valueType() {
            return valueType;
        }

    }

    public static final class ReassignmentOfConst extends TypeCheckError {

        private final String name;

        public ReassignmentOfConst(String name, Span span) {
            super("Cannot reassign '%s': declared as const".formatted(name), span);
            this.name = name;
        }

        public String name() {
            return name;
        }

    }

    public static final class NonBooleanCondition extends TypeCheckError {

        private final Type actualType;

        public NonBooleanCondition(Type actualType, Span span) {
            super("Condition must be of type bool, but got %s".formatted(actualType), span);
            this.actualType = actualType;
        }

        public Type actualType() {
            return actualType;
        }

    }

    public static final class BranchTypeMismatch extends TypeCheckError {

        private final Type thenType;
        private final Type elseType;

        public BranchTypeMismatch(Type thenType, Type elseType, Span span) {
            super("Branches of conditionals must produce the same type, but got %s and %s".formatted(thenType,
                    elseType), span);
            this.thenType = thenType;
            this.elseType = elseType;
        }

        public Type thenType() {
            return thenType;
        }

        public Type elseType() {
            return elseType;
        }

    }

    public static final class YieldOutsideLoop extends TypeCheckError {

        public YieldOutsideLoop(Span span) {
            super("'yield' used outside of a loop", span);
        }

    }

    public static final class YieldTypeMismatch extends TypeCheckError {

        private final Type firstType;
        private final Span firstSpan;
        private final Type mismatchedType;

        public YieldTypeMismatch(Type firstType, Span firstSpan, Type mismatchedType, Span span) {
            super("All 'yield's in a loop must produce the same type: found %s here, but %s was yielded earlier"
                    .formatted(mismatchedType, firstType), span);
            this.firstType = firstType;
            this.firstSpan = firstSpan;
            this.mismatchedType = mismatchedType;
        }

        public Type firstType() {
            return firstType;
        }

        public Span firstSpan() {
            return firstSpan;
        }

        public Type mismatchedType() {
            return mismatchedType;
        }

    }

    public static final class LoopMayNotYield extends TypeCheckError {

        private final Type yieldType;

        public LoopMayNotYield(Type yieldType, Span span) {
            super(("This loop yields %s on some paths, but may also end without yielding "
                    + "(the loop's condition may become false, or all repetitions may complete, "
                    + "without a 'yield' being reached); every path out of a yielding loop must yield")
                    .formatted(yieldType), span);
            this.yieldType = yieldType;
        }

        public Type yieldType() {
            return yieldType;
        }

    }

    public static final class BareStopWithYield extends TypeCheckError {

        public BareStopWithYield(Span span) {
            super("'stop;' cannot be used in a loop that also contains a 'yield': "
                    + "a bare 'stop' has no value, but this loop must produce one", span);
        }

    }

    public static final class ProduceDirectlyInLoopBody extends TypeCheckError {

        public ProduceDirectlyInLoopBody(Span span) {
            super("'produce' cannot be used directly in a loop's body: it would only end the loop body's own "
                    + "block, and that value is never observable, since a loop's value comes from 'yield', not "
                    + "'produce'. Use 'yield' instead if the loop should exit with this value here", span);
        }

    }

}
