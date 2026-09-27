package io.github.uniquepython.tyfe.typecheck;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.github.uniquepython.tyfe.ast.Mutability;
import io.github.uniquepython.tyfe.ast.Type;
import io.github.uniquepython.tyfe.common.Span;

public final class TypeEnvironment {

    private final TypeEnvironment parent;
    private final Map<String, VariableInfo> bindings = new HashMap<>();

    private TypeEnvironment(TypeEnvironment parent) {
        this.parent = parent;
    }

    public static TypeEnvironment root() {
        return new TypeEnvironment(null);
    }

    public TypeEnvironment child() {
        return new TypeEnvironment(this);
    }

    public void declare(String name, Type type, Mutability mutability, Span span) {
        Optional<VariableInfo> existing = resolve(name);
        if (existing.isPresent()) {
            throw new TypeCheckError.DuplicateDeclaration(name, existing.get().span(), span);
        }
        bindings.put(name, new VariableInfo(type, mutability, span));
    }

    public Optional<VariableInfo> resolve(String name) {
        VariableInfo local = bindings.get(name);
        if (local != null) {
            return Optional.of(local);
        }
        if (parent != null) {
            return parent.resolve(name);
        }
        return Optional.empty();
    }

}
