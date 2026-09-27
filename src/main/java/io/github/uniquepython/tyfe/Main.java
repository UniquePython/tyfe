package io.github.uniquepython.tyfe;

import java.util.List;

import io.github.uniquepython.tyfe.ast.Stmt;
import io.github.uniquepython.tyfe.lexer.Lexer;
import io.github.uniquepython.tyfe.lexer.Token;
import io.github.uniquepython.tyfe.literal.LiteralizedToken;
import io.github.uniquepython.tyfe.literal.Literalizer;
import io.github.uniquepython.tyfe.parser.Parser;

public final class Main {

    public static void main(String[] args) {
        // ---- Declarations: mut/const, all four v1 types ----
        test("mut i32 x = 5#i32;");
        test("const i32 y = 10#i32;");
        test("mut f64 pi = 3.14#f64;");
        test("const bool flag = yes;");
        test("mut char c = 'a';");
        test("const i32 z = 1#i32 + 2#i32 * 3#i32;");

        // ---- Assignment ----
        test("x = 5#i32;");
        test("x = x + 1#i32;");

        // ---- Expression statements ----
        test("5#i32;");
        test("foo;");
        test("(1#i32 + 2#i32);");

        // ---- produce / stop / skip ----
        test("produce 5#i32;");
        test("produce x + y;");
        test("stop;");
        test("skip;");

        // ---- Blocks ----
        test("{ }");
        test("{ mut i32 x = 1#i32; produce x; }");
        test("{ produce 1#i32; produce 2#i32; }"); // second produce unreachable, but should still parse

        // ---- if / else, else if chains ----
        test("if (yes) { produce 1#i32; } else { produce 2#i32; }");
        test("if (x < y) { produce x; } else if (x > y) { produce y; } else { produce 0#i32; }");
        test("if (a == b) { } else { }");
        test(
                "if (a) { produce 1#i32; } " +
                        "else if (b) { produce 2#i32; } " +
                        "else if (c) { produce 3#i32; } " +
                        "else { produce 4#i32; }");

        // ---- while ----
        test("while (x < 10#i32) { x = x + 1#i32; }");
        test("while (yes) { stop; }");
        test("while (x !> 0#i32) { skip; }");

        // ---- nested constructs ----
        test("while (x < 10#i32) { if (x == 5#i32) { stop; } else { skip; } x = x + 1#i32; }");
        test("if (x) { while (y) { skip; } } else { produce 0#i32; }");
        test("mut i32 x = if (a) { produce 1#i32; } else { produce 2#i32; };");

        // ---- full precedence/associativity sweep (still parser-relevant, not just
        // Expr) ----
        test("mut i32 a = 2#i32 ** 3#i32 ** 4#i32;"); // right-assoc exponent
        test("mut i32 b = -2#i32 ** 3#i32 + 4#i32 * 5#i32;");
        test("mut bool c2 = 1#i32 < 2#i32 && foo || bar;");
        test("mut bool d = !(foo && bar) || 1#i32 !>= 2#i32;");

        // ---- all ten comparison spellings, each in a full statement ----
        test("mut bool r1 = 1#i32 == 2#i32;");
        test("mut bool r2 = 1#i32 != 2#i32;");
        test("mut bool r3 = 1#i32 < 2#i32;");
        test("mut bool r4 = 1#i32 > 2#i32;");
        test("mut bool r5 = 1#i32 <= 2#i32;");
        test("mut bool r6 = 1#i32 >= 2#i32;");
        test("mut bool r7 = 1#i32 !< 2#i32;");
        test("mut bool r8 = 1#i32 !> 2#i32;");
        test("mut bool r9 = 1#i32 !<= 2#i32;");
        test("mut bool r10 = 1#i32 !>= 2#i32;");

        // ---- a "whole program": multiple top-level statements together ----
        test(
                "mut i32 total = 0#i32;\n" +
                        "mut i32 i = 0#i32;\n" +
                        "while (i < 10#i32) {\n" +
                        "    if (i == 5#i32) {\n" +
                        "        skip;\n" +
                        "    } else {\n" +
                        "        total = total + i;\n" +
                        "    }\n" +
                        "    i = i + 1#i32;\n" +
                        "}\n" +
                        "produce total;\n");

        // ---- error cases: should each print a clean ParserError, not crash ----
        test("mut i32 x = ;"); // missing initializer
        test("if (yes) { produce 1#i32; }"); // missing mandatory else
        test("mut i32 x = 5#i32"); // missing semicolon
        test("x = ;"); // missing rhs
        test("while yes { }"); // missing parens around condition
        test("mut bad_type x = 5#i32;"); // not a recognized type keyword
        test("{ produce 1#i32;"); // unterminated block
    }

    private static void test(String source) {
        System.out.println("=== " + source.replace("\n", "\\n") + " ===");

        try {
            List<Token> tokens = new Lexer(source).lex();
            List<LiteralizedToken> literalizedTokens = Literalizer.literalize(source, tokens);
            List<Stmt> statements = new Parser(literalizedTokens).parse();

            for (Stmt statement : statements) {
                System.out.println(statement);
            }
        } catch (RuntimeException e) {
            System.out.println("ERROR: " + e);
        }

        System.out.println();
    }

}
