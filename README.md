# tyfe

A statically-typed, interpreted scripting language, built as a hobby
project (and as a way to get back into Java after a few years away).

Design goals: every type explicit (no inference, no implicit coercion),
and one clear way to do each thing rather than several overlapping ones.
Full design notes live in [`semantics.md`](./design/semantics.md) and the
grammar in [`grammar.ebnf`](./design/grammar.ebnf).

## Status

Early days. Currently implemented:

- Lexer (identifiers, keywords, numeric/char/bool literals with
  mandatory type suffixes, comments)
- Literal evaluation (turning literal tokens into runtime values)
- A first slice of the AST (expressions, statements, types)

Not yet implemented: parser, typechecker, interpreter — so nothing
actually *runs* yet beyond `Main` printing a placeholder line.

v1 scope (the first end-to-end slice being aimed for) is intentionally
narrow: `i32`/`f64`/`bool`/`char` only, checked arithmetic/comparison/
logical operators only (wrapping variants deferred), `mut`/`const`
declarations, `if`/`else`, blocks + `produce`, `while` loops,
`stop`/`skip`, top-level statements only. No functions, structs,
arrays, or modules yet — those come later, once this slice works
end-to-end.

## Building

Requires Java 25 and Maven.

```bash
mvn compile
```

## Running

```bash
mvn -q compile exec:java
```

(There's nothing to run against yet — no parser/interpreter exists,
so this just executes `Main`.)
