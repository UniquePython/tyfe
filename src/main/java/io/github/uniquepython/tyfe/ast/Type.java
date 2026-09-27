package io.github.uniquepython.tyfe.ast;

public sealed interface Type permits Type.Primitive, Type.Nothing {

    enum Primitive implements Type {
        I32 {
            @Override
            public String toString() {
                return this.name().toLowerCase();
            }
        },
        F64 {
            @Override
            public String toString() {
                return this.name().toLowerCase();
            }
        },
        BOOL {
            @Override
            public String toString() {
                return this.name().toLowerCase();
            }
        },
        CHAR {
            @Override
            public String toString() {
                return this.name().toLowerCase();
            }
        },
    }

    enum Nothing implements Type {
        NOTHING {
            @Override
            public String toString() {
                return this.name().toLowerCase();
            }
        }
    }

}
