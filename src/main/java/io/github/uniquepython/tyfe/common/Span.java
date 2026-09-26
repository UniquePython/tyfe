package io.github.uniquepython.tyfe.common;

public record Span(int start, int end) {

    public Span {
        if (start > end)
            throw new IllegalArgumentException(
                    "Expected start <= end, but got start > end (%d > %d)".formatted(start, end));
    }

    public int length() {
        return end - start;
    }

    public boolean isEmpty() {
        return start == end;
    }

    public boolean contains(int position) {
        return start <= position && position < end;
    }

    public static Span merge(Span a, Span b) {
        return new Span(Math.min(a.start(), b.start()), Math.max(a.end(), b.end()));
    }

    public Span merge(Span other) {
        return Span.merge(this, other);
    }

    @Override
    public String toString() {
        return "[%d, %d)".formatted(start, end);
    }

}
