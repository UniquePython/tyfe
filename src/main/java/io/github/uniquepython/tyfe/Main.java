package io.github.uniquepython.tyfe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import io.github.uniquepython.tyfe.ast.Stmt;
import io.github.uniquepython.tyfe.common.TyfeError;
import io.github.uniquepython.tyfe.interpreter.Environment;
import io.github.uniquepython.tyfe.interpreter.Interpreter;
import io.github.uniquepython.tyfe.lexer.Lexer;
import io.github.uniquepython.tyfe.lexer.Token;
import io.github.uniquepython.tyfe.literal.LiteralizedToken;
import io.github.uniquepython.tyfe.literal.Literalizer;
import io.github.uniquepython.tyfe.parser.Parser;
import io.github.uniquepython.tyfe.typecheck.TypeCheckContext;
import io.github.uniquepython.tyfe.typecheck.TypeChecker;

public final class Main {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: tyfe <path-to-source-file>");
            System.exit(64);
            return;
        }

        String source;
        try {
            source = Files.readString(Path.of(args[0]));
        } catch (IOException e) {
            System.err.println("Could not read file: " + args[0]);
            System.exit(66);
            return;
        }

        try {
            run(source);
        } catch (TyfeError error) {
            System.err.println("error: " + error.getMessage() + " at " + error.span());
            System.exit(70);
        }
    }

    private static void run(String source) {
        List<Token> tokens = new Lexer(source).lex();
        List<LiteralizedToken> literalized = Literalizer.literalize(source, tokens);
        List<Stmt> statements = new Parser(literalized).parse();

        TypeCheckContext typeCheckContext = TypeCheckContext.root();
        for (Stmt statement : statements) {
            TypeChecker.checkStmt(statement, typeCheckContext);
        }

        Environment env = Environment.root();
        for (Stmt statement : statements) {
            Interpreter.execute(statement, env);
        }
    }

}
