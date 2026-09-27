package io.github.uniquepython.tyfe.ast;

import java.util.List;
import java.util.Objects;

import io.github.uniquepython.tyfe.common.Span;
import io.github.uniquepython.tyfe.literal.LiteralValue;

public sealed interface Expr
        permits Expr.Literal, Expr.Identifier, Expr.Unary, Expr.Binary, Expr.Block, Expr.If, Expr.While {

    Span span();

    record Literal(LiteralValue value, Span span) implements Expr {
        public Literal {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(span, "span");
        }
    }

    record Identifier(String name, Span span) implements Expr {
        public Identifier {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(span, "span");
        }
    }

    record Unary(UnaryOperator operator, Expr operand, Span span) implements Expr {
        public Unary {
            Objects.requireNonNull(operator, "operator");
            Objects.requireNonNull(operand, "operand");
            Objects.requireNonNull(span, "span");
        }
    }

    record Binary(Expr left, BinaryOperator operator, Expr right, Span span) implements Expr {
        public Binary {
            Objects.requireNonNull(left, "left");
            Objects.requireNonNull(operator, "operator");
            Objects.requireNonNull(right, "right");
            Objects.requireNonNull(span, "span");
        }
    }

    record Block(List<Stmt> statements, Span span) implements Expr, ElseBranch {
        public Block {
            Objects.requireNonNull(statements, "statements");
            statements = List.copyOf(statements);
            Objects.requireNonNull(span, "span");
        }
    }

    record If(Expr condition, Block thenBranch, ElseBranch elseBranch, Span span) implements Expr, ElseBranch {
        public If {
            Objects.requireNonNull(condition, "condition");
            Objects.requireNonNull(thenBranch, "thenBranch");
            Objects.requireNonNull(elseBranch, "elseBranch");
            Objects.requireNonNull(span, "span");
        }
    }

    record While(Expr condition, Block body, Span span) implements Expr {
        public While {
            Objects.requireNonNull(condition, "condition");
            Objects.requireNonNull(body, "body");
            Objects.requireNonNull(span, "span");
        }
    }

    sealed interface ElseBranch permits Block, If {
        Span span();
    }

}
