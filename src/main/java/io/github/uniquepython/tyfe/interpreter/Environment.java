package io.github.uniquepython.tyfe.interpreter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class Environment {

    private final Environment parent;
    private final Map<String, RuntimeValue> bindings = new HashMap<>();

    private Environment(Environment parent) {
        this.parent = parent;
    }

    public static Environment root() {
        return new Environment(null);
    }

    public Environment child() {
        return new Environment(this);
    }

    public void declare(String name, RuntimeValue value) {
        bindings.put(name, value);
    }

    public Optional<RuntimeValue> resolve(String name) {
        RuntimeValue local = bindings.get(name);
        if (local != null) {
            return Optional.of(local);
        }
        if (parent != null) {
            return parent.resolve(name);
        }
        return Optional.empty();
    }

    public void assign(String name, RuntimeValue value) {
        if (bindings.containsKey(name)) {
            bindings.put(name, value);
            return;
        }
        if (parent != null) {
            parent.assign(name, value);
            return;
        }
        throw new IllegalStateException(
                "unreachable: assignment to undeclared variable, typechecker should have caught this: " + name);
    }

}
