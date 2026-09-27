package io.github.uniquepython.tyfe.typecheck;

public record TypeCheckContext(TypeEnvironment env, boolean insideLoop, boolean insideBlock) {

    public static TypeCheckContext root() {
        return new TypeCheckContext(TypeEnvironment.root(), false, false);
    }

    public TypeCheckContext withNewScope() {
        return new TypeCheckContext(env.child(), insideLoop, true);
    }

    public TypeCheckContext withInsideLoop() {
        return new TypeCheckContext(env, true, insideBlock);
    }

}
