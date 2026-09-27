package io.github.uniquepython.tyfe;

import java.util.List;

import io.github.uniquepython.tyfe.ast.Expr;
import io.github.uniquepython.tyfe.ast.Stmt;
import io.github.uniquepython.tyfe.interpreter.Interpreter;
import io.github.uniquepython.tyfe.lexer.Lexer;
import io.github.uniquepython.tyfe.lexer.Token;
import io.github.uniquepython.tyfe.literal.LiteralizedToken;
import io.github.uniquepython.tyfe.literal.Literalizer;
import io.github.uniquepython.tyfe.parser.Parser;

public final class Main {
    public static void main(String[] args) {
        String sourceString = "5#i32;";
        List<Token> tokens = new Lexer(sourceString).lex();
        List<LiteralizedToken> literalized = Literalizer.literalize(sourceString, tokens);
        Parser parser = new Parser(literalized);
        List<Stmt> stmts = parser.parse();
        Expr expr = ((Stmt.ExpressionStatement) stmts.get(0)).expression();
        System.out.println(Interpreter.evaluate(expr));
    }
}
