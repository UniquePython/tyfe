package io.github.uniquepython.tyfe.ast;

public sealed interface Type permits Type.Primitive, Type.Nothing {

    enum Primitive implements Type {
        I32,
        F64,
        BOOL,
        CHAR,
    }

    enum Nothing implements Type {
        NOTHING
    }

}
