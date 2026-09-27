package io.github.uniquepython.tyfe.typecheck;

public record TypeCheckContext(TypeEnvironment env, boolean insideLoop) {

    public static TypeCheckContext root() {
        return new TypeCheckContext(TypeEnvironment.root(), false);
    }

    public TypeCheckContext withNewScope() {
        return new TypeCheckContext(env.child(), insideLoop);
    }

    public TypeCheckContext withInsideLoop() {
        return new TypeCheckContext(env, true);
    }

}
