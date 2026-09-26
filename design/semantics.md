# Semantics notes

Freeform notes on decisions that aren't fully captured by the grammar
alone -- things that would surprise someone reading only the EBNF.

## Static typing

This language is **statically typed**. Every expression's type is
known/checked before the program runs (a type-checking pass happens
prior to execution, distinct from the tree-walking evaluation
itself). This retroactively explains/justifies several earlier
decisions: mandatory type suffixes on numeric literals, and the
total absence of implicit type coercion/mixing anywhere -- both are
natural fits for a static type system that wants every type made
explicit rather than inferred or silently converted.

Consequence: referencing an identifier that was never declared is a
**compile-time error** (caught during type-checking/name-resolution,
before execution begins), not a runtime error. Likewise, using a
value of the wrong type anywhere (e.g. `mut x = 5#i32;` then later
treating `x` as an `f32`) will be a compile-time type error once
declared-type tracking is fully designed.

## Declarations

Type annotations are **mandatory** at every declaration -- there is
no type inference anywhere in this language. Shape:
`mutability type identifier = value;`, e.g. `mut i32 x = 5#i32;` or
`const f64 y = 3.0#f64;`. The initializer expression's type must
match the declared type **exactly** -- e.g. `mut i32 x = 5#u8;` is a
compile-time type error, consistent with the total absence of
implicit coercion elsewhere in the language.

## Functions

### First-class, `procedure` keyword

Functions are **first-class values**. The same keyword, `procedure`,
is used both to write a function's *type* (`procedure(i32, i32) -> i32`,
naming only parameter types) and to *define* a named function
(`procedure add(const i32 a, const i32 b) -> i32 { produce a + b; }`,
naming parameter types AND names). A function's return value is
simply its body block's produced value (via `produce`) -- no separate
return mechanism; the existing block-typing rules apply directly
(the body's static type must match the declared return type).

A function declared to return `nothing` needs **no explicit
`produce`** -- this falls directly out of the existing rule that a
`produce`-less block's value is `nothing`, so the body can simply end
(or fall through any control flow) with nothing special required.

Function definitions are **statements**, usable anywhere a statement
is allowed -- including nested inside other functions or blocks, not
just at the top level.

### Closures

Nested/local function definitions are **real closures**: they capture
variables from enclosing scopes **by reference**. A captured `mut`
variable is genuinely shared -- if the closure mutates it, the
mutation is visible through the outer variable too (and vice versa),
and multiple closures capturing the same variable observe each
other's changes. (For a captured `const` variable, by-reference vs.
by-value capture is not observably different, since a `const` can
never change.)

### Parameters: mutability, and pass-by-value vs. pass-by-reference

Each parameter has three independent aspects: mutability (`mut`/
`const`), an optional pass-by-reference marker (`@`), and its type.
Four combinations:

- `const T a` -- by-value: `a` is an independent read-only copy of
  whatever the caller passed. This is the default/only behavior
  established prior to references being introduced.
- `mut T a` -- by-value: `a` is an independent mutable copy;
  reassigning/mutating `a` inside the function body never affects
  the caller's original value.
- `const @T a` -- by-reference: `a` is a read-only view directly
  onto the caller's actual variable (no copy made) -- useful for
  avoiding copies of large values without allowing mutation.
- `mut @T a` -- by-reference: `a` is a mutable view onto the
  caller's actual variable -- mutating `a` inside the function body
  **does** affect the caller's original variable.

**`@` is illegal on function-typed parameters**, by deliberate
decision (settling what was previously an open question). A function
value isn't itself mutable the way an `i32`, array, or struct is --
there's no meaningful "give me a live, mutable view onto this
function" operation to perform on it, since you can't mutate a
function value in place. So function-typed parameters may only be
declared plain `const T` or `mut T` (by-value -- meaning an
independent copy of *which* function value is currently bound, which
is a cheap/trivial copy for a function value, unlike copying a large
struct or array). Writing `const @procedure(i32)->i32 f` (or the
`mut` equivalent) is a compile-time error. This closes the question
rather than leaving room for a narrower "reference to the variable
binding, not the value" reading -- there is no such distinct
behavior in this language.

### Calls

Call syntax is postfix and works on **any** expression producing a
function value, not just bare identifiers -- so `get_adder()(5#i32)`
(calling a call's result) and calling through a variable that holds
a function both work directly, with no special-casing needed in the
grammar.

For a by-reference (`@`) parameter, the **call site** must mark the
corresponding argument with `@` too (e.g. `add(@x, y)`) -- whether a
given call passes something by reference is visible right there at
the call site, not silently implied by the callee's signature alone.
(Passing `@x` where the parameter is not declared `@` is presumably
a type/compile error, and likewise omitting `@` where the parameter
requires it -- the call site's markings must match the callee's
signature exactly.)

`Type` now generalizes over both primitive and function types
everywhere a type can be written -- declarations, casts, parameters,
return types -- so a variable can hold or be declared to hold a
function type. **Casting to or from a function type is never legal**,
in either direction and either spelling (`as`/`<as>`) -- settling
what was previously an open question. Every existing cast category
(widening/narrowing integers, signed<->unsigned, int<->float,
char<->int) is fundamentally about reinterpreting or converting a
value's bit pattern or numeric meaning; a function value has no such
representation to convert (no "wider"/"narrower" function type,
nothing to truncate/saturate/reinterpret). This puts function types
in the exact same cast-immune category as `bool`, for the same
underlying reason: there is nothing meaningful for a cast to do.

### Recursion

**Self-recursion** works naturally with no special rule needed: a
function's own name is in scope within its own body, exactly like
any other name is visible within the block it's declared in.

**Mutual recursion does NOT work, by deliberate design decision (not
a placeholder).** Function names follow the exact same strict
top-to-bottom visibility rule as every other declaration in this
language -- a name only becomes visible *after* its declaring
statement has executed/been processed. So if `a` calls `b` but `b`
is defined *after* `a` in the same scope, that call is an
undefined-identifier error, since `b` doesn't exist yet at the point
`a` is defined. There is no hoisting and no forward-declaration
mechanism to work around this, and none is planned -- this keeps
function names fully consistent with variable names rather than
special-casing them. (A genuinely mutually-recursive algorithm can
still be written today by passing one function as a parameter to the
other, since functions are already first-class values.)

## Arrays

Fixed-size only (for now -- growable/dynamic arrays are a deferred
future decision). Type written `T mutability[N]` (e.g. `i32 mut[5]`
or `i32 const[5]`) -- `N` is a plain unsigned literal digit sequence
baked into the type at compile time, not a typed runtime value (no
`#type` suffix, unlike ordinary integer literals). Elements are
homogeneous: a single element type per array, consistent with the
total absence of implicit type mixing elsewhere in the language.
(Nesting, e.g. `i32 mut[5] mut[3]`, falls out naturally from the
grammar rather than being separately designed -- not yet given
explicit semantics beyond "an array of arrays," and element
assignment currently only supports one level of indexing at the
assignment-target level; see Assignment below.)

**Two independent mutabilities.** A full declaration reads
`mutability type mutability[N] identifier = value;`, e.g.
`mut i32 mut[5] arr = [...];`. The two mutability slots are
independent:
- The **outer** mutability (ordinary declaration mutability) governs
  whether the variable binding `arr` itself can be reassigned to a
  wholly different array value.
- The **inner** mutability (part of the array type itself, written
  right before `[N]`) governs whether individual elements can be
  mutated in place via `arr[i] = ...;`.

This gives four real combinations, e.g.: `const i32 const[5]` (fully
frozen -- can't reassign `arr`, can't mutate elements), `const i32
mut[5]` (can't reassign `arr` to a different array, but elements
inside it can still be mutated), `mut i32 const[5]` (can reassign
`arr` to an entirely different array, but can't mutate elements of
whichever array it currently holds), `mut i32 mut[5]` (fully
mutable).

**Literal construction**: `[elem1, elem2, ...]`. The element count
must exactly match the declared size `N` of the array type it's
being used for -- checked at compile time, consistent with the rest
of the type system. All elements must match the array's element type
exactly.

**Indexing (read)**: `arr[i]` (checked) and `arr<[i]>` (wrapping),
following the same checked/wrapping bracket convention as arithmetic,
casting, etc. `i` may be any unsigned integer type (`u8`/`u16`/`u32`/
`u64`) -- whatever type the index expression evaluates to.

- `arr[i]` **panics** if `i` is out of bounds (`i >= N`).
- `arr<[i]>` **never panics** -- the effective index used is
  `i % N` (modular wraparound, always landing in `[0, N)`). Since
  `i` is always unsigned, there is no negative-index case to worry
  about (unlike the signed `%` truncation rules for ordinary
  arithmetic).

**Indexing (write / mutation)**: `arr[i] = value;` (and the compound
forms, e.g. `arr[i] += value;`) follow the exact same checked/
wrapping and out-of-bounds rules as reading. Mutation is only legal
if the array's element-mutability is `mut` -- attempting to mutate
an element of a `const`-element array is a compile-time error,
exactly mirroring how reassigning a `const` variable is a
compile-time error.

Indexing chains to any depth for nested arrays (e.g.
`matrix[i][j] = value;`, freely mixing checked and wrapping forms at
each level: `matrix[i]<[j]> = value;`). For a nested assignment
target, **every level** from the outermost array down to (but not
including) the final indexed element must have `mut` element-
mutability, not just the innermost level -- mutating through a
`const`-element array at any point in the chain is a compile-time
error, since a `const`-element array's contents can't be reached
into and changed, only read.

## Panics

A **panic** is an unconditional, unrecoverable runtime termination
of the entire program. There is no catching, handling, or recovering
from a panic within the language -- when one occurs, the program
stops immediately, full stop. This is distinct from a **compile-time
error** (e.g. reassigning a `const`, a type mismatch, referencing an
undeclared name), which is caught before the program ever starts
running; a panic, by contrast, can only occur once the program is
already executing, since it depends on a runtime value (an overflow,
an out-of-bounds index, a division by zero, a narrowing cast that
doesn't fit, etc.).

Every place elsewhere in this document that says a checked operation
"panics" means precisely this: immediate, total program termination.
Exact reporting mechanics (what message is produced, where it's
written, what the process's exit status is) are deferred until I/O
and program-entry-point structure are designed.

Recoverable/catchable error handling -- for failures that are
*expected* and should be handled gracefully within the program,
rather than indicating "this should never happen" bugs -- is a
**separate, not-yet-designed mechanism** (e.g. possibly a sum type
like `Result`, once sum types exist). Panics are deliberately
reserved for the latter category and are not meant to be a
general-purpose error-handling tool.

## Program execution model

There is **no designated entry point** (no `main`-style function).
"Running the program" means executing the top-level `Program` --
every top-level statement, in strict top-to-bottom order -- exactly
like a conventional scripting language (Python, shell, etc.), not
like a compiled systems language with a special entry function.
Function definitions at the top level simply define functions
(subject to the same strict top-to-bottom visibility rule as every
other declaration -- see Recursion above); ordinary variable
declarations, expression-statements, and any other statement are
equally legal directly at the top level, with no requirement to
wrap anything in a function first. This requires no dedicated
grammar of its own -- it falls directly out of the existing
`Program ::= Statement*` rule.

## Data (structs) and privacy

### Definition and methods

`data TypeName { ... }` is a statement, like `FunctionDefinition` --
it introduces `TypeName` as a new usable `Type` for subsequent code,
following the exact same strict top-to-bottom visibility rule as
every other name in this language (a `data` block must be defined
before any code that references its name as a type).

A `data` block contains **fields** and, optionally, **procedures**.
There is no separate `impl`-style block -- procedures are declared
directly inside the `data` block, alongside the fields, but (unlike
ordinary top-level `procedure` definitions) every procedure inside a
`data` block must be marked with exactly one of two keywords, AND
with a visibility, exactly mirroring fields (mandatory, no default):

- **`iproc`** (instance procedure) -- requires an instance to call.
  Its receiver is an ordinary parameter using the already-existing
  `@` reference syntax, and this is **enforced, not just a naming
  convention**: an `iproc`'s first parameter must be an `@`-reference
  to the enclosing struct's own type (e.g.
  `pub iproc procedure deposit(mut @Account self, const f64 amount) ->
  nothing { self->cash += amount; }` inside `data Account { ... }`)
  -- checked at compile time. By convention this parameter is named
  `self`, though the grammar does not require that specific name.
- **`dproc`** (data-associated procedure) -- no instance needed or
  implied. Ordinary parameter list, commonly used for constructors
  (e.g. `pub dproc procedure new(const u64 new_id) -> Account { ... }`).

Every `iproc`/`dproc` must also be marked `pub` or `priv`, written
before the `iproc`/`dproc` keyword (e.g. `priv iproc procedure
validate(const @Account self) -> bool { ... }`) -- there is no
default visibility, same "always explicit" principle already applied
to fields, mutability, and numeric literal types throughout this
language. This closes what was previously an open question (whether
procedures needed their own visibility marking, separate from
fields) -- they do, and the rule is identical: see Privacy model
below.

An `iproc` is called through an existing instance using `.`:
`instance.method(...)` (e.g. `acc.deposit(5.0#f64)`). A `dproc` is
called through the type's own name using `:`:
`TypeName:proc_name(...)` (e.g. `Account:new(1#u64)`). Field access
always uses `->` (`instance->field`), never `.` -- the three
operators are kept strictly distinct: `->` for field access, `.` for
iproc calls, `:` for dproc calls.

### Fields

Each field is `visibility mutability type identifier;`, e.g.
`pub mut f64 cash;` or `priv const u64 id;`. Visibility (`pub`/
`priv`) and mutability (`mut`/`const`) are fully independent axes,
same spirit as the two independent mutabilities on array types.

### Privacy model

`pub` fields are accessible via `instance->field` from **anywhere**
the instance itself is reachable -- no restriction beyond ordinary
scoping/visibility of the instance.

`priv` fields are accessible via `instance->field` **only from
within an `iproc` or `dproc` declared inside that same struct's
`data` block** ("inside" means textually part of that `data` block,
regardless of which of the two kinds it is). Privacy is **per-type,
not per-instance**: any `iproc`/`dproc` of `Account` can access
`priv` fields of *any* `Account` value it has access to (e.g. a
parameter of type `Account` passed in alongside `self`, in an
`iproc`), not only fields of `self`. Code outside every `iproc`/
`dproc` of that `data` block -- free functions, top-level code,
procedures of a *different* struct -- gets a compile-time error
attempting to access a `priv` field via `->`, regardless of how it
obtained the instance.

**Construction also respects privacy.** A struct literal
(`TypeName { field1 = expr1, ... }`) that sets any `priv` field is
only legal from within an `iproc` or `dproc` of that same `data`
block -- exactly the same "inside vs. outside" boundary as field
access. Code outside the struct's own procedures cannot construct a
`TypeName` value directly if doing so would require setting a
`priv` field; it must instead call some publicly-callable `dproc` of
the struct that constructs and returns an instance on its behalf
(conventionally named `new` or similar -- not a distinct language
concept, just a `dproc` whose body happens to build and `produce` a
`TypeName` value). This is what makes `priv` actually protect
something end-to-end, rather than only blocking access after an
outside caller has already been able to freely set the field at
construction time. (Note: `pub`/`priv` visibility is currently only
defined for *fields* -- whether `iproc`/`dproc` procedures themselves
need their own visibility marking, e.g. to have a fully private
helper procedure, is a separate open question not yet decided.)

Every field, `pub` or `priv`, must be given exactly once in a struct
literal, by name (order need not match declaration order) -- there
are no default field values and no partial construction, consistent
with mandatory initialization everywhere else in this language.

### Field access and assignment

Field access is `instance->field` (not the more common dot
notation -- a deliberate choice for this language, since `.` is
reserved specifically for iproc calls, see above). It is postfix and
chains/composes freely with calls and indexing in any order and to
any depth: `acc.deposit(5#f64)` (iproc call), `accounts[i]->cash`
(field access through an indexed element), `acc->sub_account.deposit
(5#f64)` (field access, then an iproc call), `a->b->c` (chained field
access), etc.

Assigning to a field (`acc->cash = 100.0#f64;`, including compound
forms like `acc->cash += 5.0#f64;`) requires the field to be `mut`
-- assigning to a `const` field is a compile-time error, mirroring
`const` variables and `const`-element arrays. Assigning to a `priv`
field additionally requires being inside a method of that struct's
own `data` block, per the privacy model above. For a chained/nested
assignment target mixing fields and array indices, every
intermediate step must be mutable (and, for `priv` steps, the
assignment must occur from inside the appropriate struct's methods)
-- same principle as nested array mutation.

## `char` representation

`char` represents a single byte, range 0-255 (extended ASCII /
single-byte), not a full Unicode codepoint. Its value space is
exactly the same size as `u8`'s -- no wasted or missing values. This
is what makes the `char`/`u8` casting relationship exact (see
Casting below) rather than requiring truncation or padding.

## Casting

Explicit-only type conversion via `expr as Type` (checked) or
`expr as` wrapped as `expr <as> Type` (permissive). There is no
implicit conversion anywhere, even for conversions that can never
lose information (e.g. `u8 as i64` still requires an explicit cast).
Casts chain left-to-right (`x as i64 as f32` casts to `i64` first,
then that result to `f32`).

By category:

- **Widening integer cast** (e.g. `u8 as i64`): always safe. `as`
  and `<as>` behave identically -- both always succeed, never panic.
- **Narrowing integer cast** (e.g. `i64 as u8`): `as` panics if the
  source value doesn't fit in the target type's range; `<as>` wraps
  (keeps the low bits, two's-complement style, same wrapping
  behavior as the wrapping arithmetic operators).
- **Signed <-> unsigned cast, same width** (e.g. `i32 as u32`):
  treated as a narrowing-shaped case -- `as` panics if the source
  value doesn't fit the target (e.g. a negative `i32` can't become a
  `u32`); `<as>` reinterprets the bits directly (two's-complement
  reinterpretation), no panic.
- **Int -> float cast** (e.g. `i64 as f32`): always allowed, rounds
  to the nearest representable float. `as` and `<as>` behave
  identically -- never panics, even if precision is lost (precision
  loss on int->float is too common/fuzzy a condition to treat as a
  panic case, unlike a value being flatly out of range).
- **Float -> int cast** (e.g. `f64 as i32`): the fractional part is
  truncated toward zero first (consistent with `/`'s truncation
  rule). `as` then panics if the truncated integer value is out of
  the target type's range, or if the source was `NaN`/`inf`/`-inf`.
  `<as>` performs the same truncation but saturates/wraps instead of
  panicking on those cases (exact saturation value for `NaN`/`inf`
  under `<as>` to be pinned down when the interpreter is built --
  not yet fully specified).
- **`char` <-> integer**: `char` is castable to/from any integer
  type, following exactly the same rules as if `char` were `u8` --
  because `char`'s value space is identical to `u8`'s (see `char`
  representation above). `char` <-> `u8` and wider unsigned types
  follow the widening-cast rule (always safe); `char` <-> signed
  types follow the same-width-ish signed/unsigned panic(`as`)/
  reinterpret(`<as>`) rule, since a `char`/`u8` value of 128-255 has
  no direct positive representation in a signed 8-bit type.
- **`bool`**: **never castable** to or from anything, in either
  direction, with either spelling. Consistent with this language
  having no truthy/falsy coercion anywhere.
- **Function types**: **never castable** to or from anything, in
  either direction, with either spelling -- same status as `bool`.
  A function value has no numeric/bit-level representation for a
  cast to convert or reinterpret (see Functions, above).

## Assignment

Assignment is **statement-only** -- unlike C, `x = 5#i32` is not an
expression and has no value; it cannot be nested (`y = (x = 5#i32);`
is not valid). This mirrors declarations, which are also statements
only.

Only `mut` variables may be reassigned; reassigning a `const` is a
**compile-time error** (caught by the same name-resolution/type pass
that catches undeclared identifiers), enforcing what `const` means.

Besides plain `=` (replace outright), there is a full set of compound
assignment operators, one pair (checked + wrapping) per arithmetic
operator, following the same `<op=>` wrapping-bracket convention used
for the wrapping arithmetic operators themselves:

- Checked: `+= -= *= /= %= **=`
- Wrapping: `<+=> <-=> <*=> </=> <%=> <**=>`

Every compound form is **pure desugaring** with no additional
semantics of its own: `x += e;` means exactly `x = x + e;`,
`x <+=> e;` means exactly `x = x <+> e;`, and so on. All the usual
rules of the underlying operator still apply (checked forms panic on
overflow/underflow per the normal rules, wrapping forms wrap, `/=`
and `%=` panic on division by zero, etc.). The right-hand side's
type must match the variable's declared type exactly, same
requirement as at declaration.

## `if` / `unless`

`if (cond) { ... } else { ... }` and `unless (cond) { ... } else { ... }`
are the same construct; `unless`'s branch runs when `cond` is
**false** (i.e. `unless (cond) {A} else {B}` means exactly
`if (!cond) {A} else {B}`). Condition is always parenthesized.

`else` is **mandatory** (for now -- may revisit later to allow
omitting it when the whole `if`/`unless` is used purely in statement
position, mirroring how `produce`-less blocks default to `nothing`).
This is required by static typing: both branches are blocks, and
since `if`/`unless` is usable in expression position, both branches'
produced types must match exactly -- there is no value to fall back
to if a branch is missing.

Chains freely mix `if`, `unless`, `else if`, and `else unless` in any
combination and any order (a chain may start with either `if` or
`unless`) -- each is just sugar for nested `if`/`unless` inside an
`else`, so there's nothing structurally special about any particular
mix.

Example:
```
mut i32 x = if (a > b) {
    produce a;
} else unless (b > 0#i32) {
    produce 0#i32;
} else {
    produce b;
};
```

## Loops

Three forms: `while (cond) { ... }`, `until (cond) { ... }` (exactly
`while (!cond) { ... }` -- mirrors how `unless` relates to `if`), and
`loop (n) { ... }` (repeats the body exactly `n` times, `n` being any
unsigned integer type -- `u8`/`u16`/`u32`/`u64`, whichever the given
expression evaluates to; no loop counter is exposed inside the body).

A fourth form, `for (mutability [@]? Type identifier in arrExpr) {
... }`, iterates once per element of an array expression. The
per-element binding reuses the **exact same four combinations**
already established for function parameters (see Functions ->
Parameters above), rather than inventing new rules:

- `const T x in arr` -- `x` is a read-only copy of each element.
- `mut T x in arr` -- `x` is a mutable copy; mutating `x` never
  affects `arr` itself (by-value, same as a by-value parameter).
- `const @T x in arr` -- `x` is a read-only live view directly onto
  each element in turn.
- `mut @T x in arr` -- `x` is a mutable live view; mutating `x`
  mutates the actual array element in place. Only legal if `arr`'s
  element-mutability is `mut` -- same rule as ordinary indexed
  assignment (`arr[i] = ...`) -- attempting this on a `const`-element
  array is a compile-time error.

`Type` must be written explicitly and match the array's element type
exactly -- no inference, consistent with every declaration and
parameter in the language having a mandatory, exact type annotation.

For a **nested array** (array of arrays), `for` iterates **one level
at a time**: `identifier` is bound to each inner array (e.g. for
`i32 mut[3] mut[5] matrix`, a `for` over `matrix` binds each
`i32 mut[3]` row, not individual `i32`s), exactly mirroring how a
single level of indexing (`matrix[i]`) yields a row rather than a
scalar. Reaching individual elements of a nested array requires
nesting a `for` per level, same as chaining indices does
(`matrix[i][j]`). There is no automatic flattening.

Like the other three loop forms, `for` is a genuinely new loop
keyword rather than an overload of `loop`'s existing syntax (e.g.
`loop (arr) {...}` was considered and rejected) -- this keeps the
language's "no behavior inferred from an expression's type" pattern
intact; what kind of loop you're writing is visible at the keyword,
not inferred from what's in the parentheses.

All four loop forms are usable in **expression position**, consistent
with blocks and `if`/`unless`: their value comes from `produce`
executed inside the body. Unlike `if`/`unless` (where both branches
are mandatory and always run exactly one of them), a loop can easily
finish -- condition becomes false, all `n` iterations complete, or
(for `for`) the array is exhausted or empty -- without `produce` ever
having run. In that case the loop's value is `nothing`, same as a
`produce`-less block. This means a loop used in expression position
may or may not actually produce a meaningful value, unlike `if`/
`unless`.

`stop;` exits the innermost enclosing loop immediately. It is
**strictly value-less** -- it always yields `nothing` as the loop's
value; there is no `stop expr;` form. Use `produce` instead if the
loop needs to exit early with a value.

`skip;` jumps to the next iteration of the innermost enclosing loop
(re-checks the condition for `while`/`until`; moves to the next
repetition for `loop`). Takes no argument.

Both `stop` and `skip` affect only the **innermost** enclosing loop.
There is no labeled-loop support (e.g. `stop outer;`), **by
deliberate permanent design decision, not a placeholder**. If code
needs to exit or continue an *outer* loop from inside a nested one,
the workaround is to restructure -- e.g. extract the inner loop into
its own function and use its return value as a signal the outer loop
checks, or use a `mut bool` flag set inside the inner loop and
checked in the outer loop's condition.

## Blocks and `produce`

A block (`{ ... }`) is a statement sequence usable in expression
position. It introduces a new lexical scope -- declarations inside
go out of scope when the block ends.

A block's value comes from `produce expr;`, a statement legal
anywhere inside the block (not just at the end). Executing a
`produce` immediately ends the block with that value; no later
statements in the block run. If execution reaches the end of the
block without ever executing a `produce`, the block's value is
`nothing`.

`produce` is syntactically legal in any block regardless of whether
that block is used in expression or statement position -- in
statement position, the produced value is simply discarded, same as
any other expression-statement.

Since the language is statically typed, a block has exactly one
static type. Once branching exists (so a block could contain more
than one reachable `produce`, e.g. one per `if` branch), every
`produce` reachable within a given block must agree on type -- that
shared type is the block's static type. (Not yet enforceable in
practice, since without branches a block can only ever reach at
most one `produce`, but recorded now so the rule is already settled
when `if` is designed.)

## No null / None / nil

This language has **no null-like value at all**. There is no concept
of "a variable of type T that might actually hold nothing." The unit
type `nothing` is NOT this -- `nothing` is a proper type with exactly
one value, used for "this expression/function produces no meaningful
result," not for "this value is absent/missing."

Consequence (not yet designed, just flagged so future decisions don't
accidentally reintroduce null through the back door): if/when the
language needs to express "a value that may or may not be present"
(e.g. a lookup that might fail), that will require a deliberate sum
type / Option-like construct designed later -- it will not be bolted
on as an implicit null.

## Arithmetic operators: checked vs. wrapping

Every binary arithmetic operator comes in two forms:
- **Checked** (`+ - * / % **`): panics at runtime if the true
  mathematical result cannot be represented in the operand type.
- **Wrapping** (`<+> <-> <*> </> <%> <**>`): silently wraps
  (two's-complement style) on the same condition instead of
  panicking.

This distinction is **integer-only** (applies to i8/i16/.../u64).
It does NOT apply to floats -- see "Floats" below.

No implicit numeric type mixing exists anywhere: both operands of
any arithmetic operator must be the exact same primitive type, or
it is an error. (E.g. `1#i32 + 1#i64` is invalid; a cast, to be
designed later, would be required.)

### Division and remainder (`/ % </> <%>`)

C semantics: `/` truncates toward zero; `%`'s result takes the sign
of the dividend (matches C/Java/Rust, NOT Python's floored mod).

- Division by zero is **always a panic**, for both the checked and
  wrapping spellings of `/` and `%`. It is not treated as an
  overflow/underflow condition, so the wrapping variant does not
  suppress it.
- The overflow/underflow condition that the checked-vs-wrapping
  distinction actually governs for `/` and `%` is the classic
  `MIN / -1` (and `MIN % -1`) edge case on signed integer types --
  the one situation where truncating division's true result doesn't
  fit back into the type's range. Checked `/`/`%` panics on this;
  wrapping `</>`/`<%>` wraps.

### Exponentiation (`** <**>`)

Integer-only (base and exponent are both integers of the same
type; no float exponentiation via this operator -- would need a
future library function). Checked `**` panics on overflow; wrapping
`<**>` wraps.

### Logical operators

`&&`, `||`, `!` (unary). Operands must be `bool` -- there is no
truthy/falsy coercion of any other type (consistent with "no
implicit type mixing/coercion" everywhere else in this language).
Always produce `bool`. No checked/wrapping distinction (nothing can
overflow).

`&&` and `||` **short-circuit**: `a && b` does not evaluate `b` if
`a` is `no`; `a || b` does not evaluate `b` if `a` is `yes`. (Inert
today since no expressions have side effects yet, but the rule is
fixed now so it's already settled once function calls exist.)

## Comparison operators

Same-type-only, like arithmetic -- no implicit mixing, both operands
must match exactly or it's an error. No checked/wrapping distinction
(comparison can't overflow). Always produces `bool`.

In addition to the standard six (`== != < > <= >=`), this language
also has explicit negated-ordering spellings: `!<` (not less than),
`!<=` (not less-or-equal), `!>` (not greater than), `!>=` (not
greater-or-equal). These are semantically equivalent to `>=`, `>`,
`<=`, `<` respectively (for the totally-ordered types below), but
exist as distinct operators so the negation can be written directly
rather than requiring the writer to mentally flip the comparison.
They follow the exact same type-applicability rules as the operators
they mirror.

- **Integers, floats**: all ten (`== != < > <= >= !< !<= !> !>=`).
- **char**: all ten, ordered by code point.
- **bool**: only `== !=` -- no ordering (no implicit bool->int
  coercion exists to make ordering meaningful).
- **nothing**: only `== !=`, trivially (the single value always
  equals itself).

Comparisons do **not** chain: `a < b < c` is not valid syntax (it
would try to compare a `bool` result against `c` with `<`, and
`bool` doesn't support `<`). Write `(a < b) && (b < c)` instead, once
logical operators exist.

## Bitwise operators

`&` (AND), `|` (OR), `^` (XOR) -- binary -- and `~` (NOT) -- unary.
**Unsigned integer types only** (`u8`/`u16`/`u32`/`u64`) -- illegal
on signed integers, `bool`, or any other type. This was a deliberate
choice over also allowing signed types: pairing bit-level operations
with a type where the top bit means "negative" is a common source of
confusion, and restricting to unsigned sidesteps that entirely (a
signed value can simply be cast to unsigned first, using the
existing cast system, if bit-twiddling is needed). `bool` was also
deliberately excluded, even though some languages (Rust) allow
`&`/`|`/`^`/`~` on `bool` as non-short-circuiting logical operators
-- doing so here would introduce a second, redundant way to express
negation/AND/OR alongside `!`/`&&`/`||`, cutting against this
language's "one clear way to do each thing" style.

For the three binary operators, both operands must be the exact same
unsigned type -- same "no implicit numeric mixing" rule as
arithmetic, no exception carved out for bitwise ops. No checked/
wrapping distinction on any of the four: combining or complementing
existing same-width bit patterns can never produce an out-of-range
result, so there's nothing to overflow -- each has exactly one
spelling, same status as the logical operators and comparisons in
that regard.

### Precedence

Bitwise operators and shifts sit **between comparisons and additive
operators**, tighter than comparisons (loosest to tightest: `|`,
then `^`, then `&`, then shifts, then `+`/`-`). This is the
Rust-style choice, deliberately *not* C's precedence. In C, shifts
and bitwise operators sit *below* comparisons, which makes
`a & b == c` silently parse as `a & (b == c)` -- one of C's most
infamous grammar footguns. Putting them above comparisons instead
means `a & b == c` parses as `(a & b) == c`, matching what most
people intuitively expect, while still keeping the operators
themselves familiar to anyone from a C-family background.

### Shifts (`<< >> <<<> <>>>`)

Also unsigned-integer-only. Logical shift only (always zero-fill) --
unambiguous since signed types are excluded, so there's no
arithmetic-vs-logical right-shift ambiguity to resolve (unlike C,
which must choose for signed right shift).

Checked/wrapping split governs shift-amount-too-large (shift amount
>= the left operand's bit-width, e.g. `5#u8 << 9#u8`, since `u8` is
only 8 bits wide):
- Checked (`<<`, `>>`): panics if the shift amount is out of range.
- Wrapping (`<<<>`, `<>>>`): masks the shift amount instead
  (equivalent to `shift_amount % bit_width`), so it never panics.

The **shift amount's type is not required to match the left
operand's type** -- e.g. `300#u16 << 2#u8` is fine, the `u8` shift
count doesn't need to be widened or matched to `u16`. This is a
deliberate, narrow exception to "both operands must be the exact
same type": a shift amount is a *count*, not a value being combined
bitwise with the left operand, so there's nothing to mix -- the
"same type" rule elsewhere exists to prevent silently combining two
different *value* representations, which doesn't apply here. Any
unsigned integer type is valid as the shift amount, whatever type
the shift-amount expression happens to evaluate to.

## Unary minus

Only legal on **signed** integer types and floats -- applying `-`
or `<->` to an unsigned type (u8..u64) is an error. Checked `-`
panics only on negating a signed type's MIN value (e.g. `-(-128#i8)`,
since `128` has no representation in `i8`); wrapping `<->` wraps
instead. No unary plus exists.

### Floats (f32/f64)

Checked and wrapping spellings behave **identically** on floats --
standard IEEE 754 semantics apply either way (overflow silently
produces `inf`/`-inf`, invalid operations silently produce `NaN`,
never a panic). The checked/wrapping distinction exists specifically
to guard against fixed-width integer wraparound; IEEE 754 already
fully defines float edge-case behavior, so there is nothing extra
for a "checked" float operator to guard against.

## Modules, files, and top-level visibility

### Top-level visibility

Top-level `mut`/`const` declarations, `procedure` definitions, and
`data` definitions each require an explicit, mandatory `pub` or
`priv` marker, written first (before `mut`/`const`, before
`procedure`, before `data`) -- e.g. `pub mut i32 x = 5#i32;`,
`priv procedure helper(...) -> nothing {...}`, `pub data Account
{...}`. No default exists, same "always explicit" principle used
everywhere else (fields, params, mutability, numeric literal types).

This is deliberately **only** required at the top level. A local
variable declaration or a nested/local function definition (inside a
block or another function's body) stays exactly as already designed
-- no visibility marker at all -- because visibility only has meaning
relative to *other modules*, and a local binding is never reachable
from outside its enclosing block/function regardless. Top-level and
local declarations/functions are therefore genuinely two different
grammar productions (`TopLevelDeclarationStatement` vs.
`DeclarationStatement`, `TopLevelFunctionDefinition` vs.
`FunctionDefinition`, `TopLevelDataDefinition` vs. `DataDefinition`),
identical in every respect except the leading visibility marker.

For `data`, this top-level `pub`/`priv` on the *definition itself*
(governing whether the type is importable/usable from other modules
at all) is a separate, independent concern from the `pub`/`priv`
already required on each individual field and `iproc`/`dproc` inside
it (governing access *within* code that can already see the type --
see Privacy model, above). A `priv data` type could theoretically
still have `pub` fields, for instance -- that combination just means
"nothing outside this file can ever get hold of an instance of this
type to read those fields from," since the type itself never leaves
the module; the field-level `pub` only matters for code inside the
same file, or in some other module which imported the type.

### Modules = files

There is no separate `module` keyword or block construct. **One
source file is one module**, implicitly, named by its filename. This
was a deliberate choice over alternatives (explicit `module { ... }`
blocks independent of files, or deferring the file/module question
entirely) -- keeping the file itself as the only boundary is simpler
and avoids introducing a second, redundant scoping construct on top
of files that already exist on disk.

Source files use the **`.tyfe`** extension (matching the language's
own name).

### Directories, and the `.tyfe.mod` gate

A directory of `.tyfe` files only becomes a traversable segment in a
module path if it contains a marker file named
**`<directory_name>.tyfe.mod`** (e.g. `geometry.tyfe.mod` inside a
directory named `geometry/`). This marker file is plain text, **not**
tyfe source code -- it simply lists, one per line, the filenames
(without the `.tyfe` extension) of the files in that *same* directory
that are allowed to be imported from outside it.

Key properties of this mechanism:

- **Non-recursive, per-directory.** A directory's `.tyfe.mod` only
  ever lists files sitting directly inside that same directory. A
  subdirectory is not automatically covered and needs its own,
  separate `<subdir_name>.tyfe.mod` to be traversable in turn -- there
  is no single top-level manifest that declares the whole project's
  importable surface at once; each directory gates its own contents.
- **The project root needs one too.** There is no exemption for the
  outermost directory -- if any file at the project root should be
  importable from elsewhere, the root directory needs its own
  `<rootdirname>.tyfe.mod` exactly like any other directory.
- **Unlisted means unreachable, full stop.** A `.tyfe` file that
  exists on disk but isn't named in its directory's `.tyfe.mod` cannot
  be reached via `use` at all -- not "its `pub` items are private,"
  but genuinely absent from the space of importable paths, regardless
  of what's marked `pub` inside it.
- **Two independent, stacked gates.** Being listed in `.tyfe.mod`
  (file-level: "this file can be imported from at all") and being
  `pub` (item-level: "this specific thing inside the file is
  exposed") are two separate checks that both must pass. Listing a
  file doesn't make everything in it public -- its `priv` items are
  still `priv`; it only makes the file's `pub` items reachable.

### Module paths and `use`

A `ModulePath` is a `::`-separated chain of directory segments
followed by a final file segment, e.g. `geometry::shapes` refers to
`shapes.tyfe` inside a directory `geometry/` (reachable only if
`geometry/geometry.tyfe.mod` lists `shapes`). A single bare segment
(e.g. `shapes`) refers to a file directly at the project root.

Import syntax: `use ModulePath::UseList;`, e.g.:
```
use shapes::{Square as s, Circle as c};
use geometry::shapes::Circle;
```
- **Always selective, never a blanket/whole-module import.** There is
  no form that imports "everything `pub` in a module" at once --
  every name brought in must be individually named. This was a
  deliberate choice for consistency with the language's "one clear
  way to do each thing" style, rather than supporting both a
  qualified whole-module form and a selective form side by side.
- **Always brought in unqualified.** Once imported, an item is
  referenced directly by its (possibly renamed) name -- never
  re-qualified by its module path at the use site. There is no
  `shapes::Circle` reference syntax *outside* a `use` statement itself.
- **Braces are optional for exactly one item**, required for two or
  more: `use shapes::Circle;` and `use shapes::{Circle};` are both
  legal and equivalent; `use shapes::{Square, Circle};` requires the
  braces.
- **Renaming (`as`) is optional and independent per item** within a
  multi-item list -- e.g. `use shapes::{Square, Circle as c};` renames
  only `Circle`, leaving `Square` as-is.
- **Only reachable, `pub` items are ever valid to `use`.** Attempting
  to `use` an item that is `priv`, or that lives in a file not listed
  in its directory's `.tyfe.mod`, is a compile-time error.

## Mutability

Variables are declared as one of two co-equal forms, chosen explicitly
at the declaration site: `mut` (reassignable) or `const` (not
reassignable after init). Neither is the "default" -- there is no bare
declaration form without one of these two keywords. Initialization is
always mandatory; there is no such thing as an uninitialized
declaration in this language.
