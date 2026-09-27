package io.github.uniquepython.tyfe;

import java.util.List;

import io.github.uniquepython.tyfe.ast.Expr;
import io.github.uniquepython.tyfe.lexer.Lexer;
import io.github.uniquepython.tyfe.lexer.Token;
import io.github.uniquepython.tyfe.literal.LiteralizedToken;
import io.github.uniquepython.tyfe.literal.Literalizer;
import io.github.uniquepython.tyfe.parser.Parser;

public final class Main {

    public static void main(String[] args) {
        test("123#i32");
        test("123.45#f64");
        test("'a'");
        test("yes");
        test("no");
        test("foo");

        test("(123#i32)");
        test("((123#i32))");
        test("foo");
        test("(foo)");

        test("-123#i32");
        test("!!foo");
        test("!-foo");
        test("-!foo");

        test("1#i32 + 2#i32");
        test("1#i32 - 2#i32");
        test("1#i32 * 2#i32");
        test("1#i32 / 2#i32");
        test("1#i32 % 2#i32");
        test("2#i32 ** 3#i32");

        test("1#i32 + 2#i32 * 3#i32");
        test("1#i32 * 2#i32 + 3#i32");
        test("1#i32 + 2#i32 - 3#i32");
        test("1#i32 - 2#i32 + 3#i32");
        test("1#i32 * 2#i32 / 3#i32");
        test("1#i32 / 2#i32 * 3#i32");
        test("2#i32 ** 3#i32 * 4#i32");
        test("2#i32 * 3#i32 ** 4#i32");

        test("2#i32 ** 3#i32 ** 4#i32");
        test("(2#i32 ** 3#i32) ** 4#i32");
        test("2#i32 ** (3#i32 ** 4#i32)");

        test("1#i32 < 2#i32");
        test("1#i32 <= 2#i32");
        test("1#i32 > 2#i32");
        test("1#i32 >= 2#i32");
        test("1#i32 == 2#i32");
        test("1#i32 != 2#i32");
        test("1#i32 !< 2#i32");
        test("1#i32 !<= 2#i32");
        test("1#i32 !> 2#i32");
        test("1#i32 !>= 2#i32");

        test("foo && bar");
        test("foo || bar");
        test("foo && bar || baz");
        test("foo || bar && baz");
        test("!foo && bar");
        test("!(foo && bar)");

        test("1#i32 < 2#i32 && foo");
        test("foo || 1#i32 == 2#i32");
        test("!foo || bar && baz");
        test("(foo || bar) && baz");

        test("1#i32 + 2#i32 * 3#i32 == 7#i32");
        test("1#i32 + 2#i32 * 3#i32 < 8#i32 && yes");
        test("!(1#i32 < 2#i32) || no");
        test("-2#i32 ** 3#i32 + 4#i32 * 5#i32");

        test("foo + bar * baz");
        test("(foo + bar) * baz");
        test("foo < bar");
        test("foo > bar");
        test("foo == bar");
        test("foo != bar");

        test("0#i32");
        test("123_456#i32");
        test("0xFF#i32");
        test("0b1010#i32");
        test("0o755#i32");
        test("1.0#f64");
        test("123_456.789_012#f64");
        test("'\\n'");
        test("'\\t'");
        test("'\\\\'");
        test("'\\''");
        test("'\\0'");
    }

    private static void test(String source) {
        System.out.println("=== " + source + " ===");

        try {
            List<Token> tokens = new Lexer(source).lex();
            List<LiteralizedToken> literalizedTokens = Literalizer.literalize(source, tokens);
            Expr expression = new Parser(literalizedTokens).parseExpression();

            System.out.println(expression);
        } catch (RuntimeException e) {
            System.out.println("ERROR: " + e);
        }

        System.out.println();
    }

}
