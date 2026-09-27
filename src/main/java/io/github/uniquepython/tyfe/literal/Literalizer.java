package io.github.uniquepython.tyfe.literal;

import java.util.ArrayList;
import java.util.List;

import io.github.uniquepython.tyfe.lexer.Token;
import io.github.uniquepython.tyfe.lexer.TokenKind;

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
            case TokenKind.Literal.BOOL -> parseBool(source, token);
            case TokenKind.Identifier.IDENT -> parseIdentifier(source, token);
            case TokenKind.Keyword _ -> new LiteralValue.None();
            case TokenKind.Operator _ -> new LiteralValue.None();
            case TokenKind.Punctuation _ -> new LiteralValue.None();
            case TokenKind.Sentinel _ -> new LiteralValue.None();
        };

        return LiteralizedToken.of(token, value);
    }

    private static LiteralValue parseInteger(String source, Token token) {
        String text = source.substring(token.span().start(), token.span().end());
        int hashIndex = text.indexOf('#');
        String digitsPart = text.substring(0, hashIndex);
        String suffix = text.substring(hashIndex + 1);

        if (!suffix.equals("i32"))
            throw new LiteralizerError.SuffixShapeMismatch(suffix, token.span());

        int radix;
        String cleanDigits;

        if (digitsPart.startsWith("0x")) {
            radix = 16;
            cleanDigits = digitsPart.substring(2);
        } else if (digitsPart.startsWith("0b")) {
            radix = 2;
            cleanDigits = digitsPart.substring(2);
        } else if (digitsPart.startsWith("0o")) {
            radix = 8;
            cleanDigits = digitsPart.substring(2);
        } else {
            radix = 10;
            cleanDigits = digitsPart;
        }

        cleanDigits = cleanDigits.replace("_", "");

        try {
            int value = Integer.parseInt(cleanDigits, radix);
            return new LiteralValue.IntValue(value);
        } catch (NumberFormatException e) {
            throw new LiteralizerError.IntegerOverflow(text, token.span());
        }
    }

    private static LiteralValue parseFloat(String source, Token token) {
        String text = source.substring(token.span().start(), token.span().end());
        int hashIndex = text.indexOf('#');
        String digitsPart = text.substring(0, hashIndex);
        String suffix = text.substring(hashIndex + 1);

        if (!suffix.equals("f64"))
            throw new LiteralizerError.SuffixShapeMismatch(suffix, token.span());

        String cleanDigits = digitsPart.replace("_", "");

        double value;

        try {
            value = Double.parseDouble(cleanDigits);
        } catch (NumberFormatException e) {
            throw new LiteralizerError.FloatOverflow(text, token.span());
        }

        if (Double.isInfinite(value))
            throw new LiteralizerError.FloatOverflow(text, token.span());

        return new LiteralValue.FloatValue(value);
    }

    private static LiteralValue parseChar(String source, Token token) {
        String text = source.substring(token.span().start(), token.span().end());
        String content = text.substring(1, text.length() - 1);

        char decoded;

        if (content.length() == 1) {
            decoded = content.charAt(0);
        } else {
            char escapeChar = content.charAt(1);

            decoded = switch (escapeChar) {
                case 'n' -> '\n';
                case 't' -> '\t';
                case 'r' -> '\r';
                case '\\' -> '\\';
                case '\'' -> '\'';
                case '0' -> '\0';
                default -> throw new IllegalStateException("Unexpected character escape: \\" + escapeChar);
            };
        }

        if (decoded > 127)
            throw new LiteralizerError.NonAsciiCharLiteral(decoded, token.span());

        return new LiteralValue.CharValue((byte) decoded);
    }

    private static LiteralValue parseBool(String source, Token token) {
        String text = source.substring(token.span().start(), token.span().end());
        return switch (text) {
            case "yes" -> new LiteralValue.BoolValue(true);
            case "no" -> new LiteralValue.BoolValue(false);
            default -> throw new IllegalStateException("Unexpected boolean literal: " + text);
        };
    }

    private static LiteralValue parseIdentifier(String source, Token token) {
        String name = source.substring(token.span().start(), token.span().end());
        return new LiteralValue.IdentValue(name);
    }

}
