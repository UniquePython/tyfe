package io.github.uniquepython.tyfe;

import java.util.ArrayList;
import java.util.List;

public final class Literalizer {

    private Literalizer() {
    }

    public static List<LiteralizedToken> literalize(String source, List<Token> tokens) {
        List<LiteralizedToken> result = new ArrayList<>();

        for (Token token : tokens) {
            result.add(literalizeOne(source, token));
        }

        return result;
    }

    private static LiteralizedToken literalizeOne(String source, Token token) {
        LiteralValue value = switch (token.kind()) {
            case TokenKind.Literal.INTEGER -> parseInteger(source, token);
            case TokenKind.Literal.FLOAT -> parseFloat(source, token);
            case TokenKind.Literal.CHAR -> parseChar(source, token);
            case TokenKind.Keyword k -> new LiteralValue.None();
            case TokenKind.Identifier i -> new LiteralValue.None();
            case TokenKind.Operator o -> new LiteralValue.None();
            case TokenKind.Punctuation p -> new LiteralValue.None();
            case TokenKind.Sentinel s -> new LiteralValue.None();
        };

        return LiteralizedToken.of(token, value);
    }

    // parseInteger, parseFloat, parseChar: TBD

}
