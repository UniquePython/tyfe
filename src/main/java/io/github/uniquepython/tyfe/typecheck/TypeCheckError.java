package io.github.uniquepython.tyfe.typecheck;

import io.github.uniquepython.tyfe.common.Span;
import io.github.uniquepython.tyfe.common.TyfeError;
import io.github.uniquepython.tyfe.ast.Type;
import io.github.uniquepython.tyfe.ast.UnaryOperator;
import io.github.uniquepython.tyfe.ast.BinaryOperator;

public abstract sealed class TypeCheckError extends TyfeError
        permits TypeCheckError.DuplicateDeclaration, TypeCheckError.UndeclaredIdentifier,
        TypeCheckError.InvalidOperandType, TypeCheckError.OperandTypeMismatch, TypeCheckError.UnreachableCode {

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

}
