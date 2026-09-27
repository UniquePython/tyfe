package io.github.uniquepython.tyfe.typecheck;

import io.github.uniquepython.tyfe.common.Span;
import io.github.uniquepython.tyfe.ast.Type;
import io.github.uniquepython.tyfe.ast.Mutability;

public record VariableInfo(Type type, Mutability mutability, Span span) {
}
