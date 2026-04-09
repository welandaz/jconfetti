package io.github.welandaz;

final class Characters {

    private Characters() {
    }

    static boolean isSpaceButNotLineTerminator(final char character) {
        return isNotLineTerminator(character) && (Character.isWhitespace(character) || Character.isSpaceChar(character));
    }

    static boolean isForbiddenCodePoint(final int codePoint) {
        if (codePoint == '\t' || isLineTerminator(codePoint)) {
            return false;
        }
        if (codePoint < 0x20 || codePoint == 0x7F) {
            return true;
        }
        if (codePoint >= 0xD800 && codePoint <= 0xDFFF) {
            return true;
        }
        if (codePoint >= 0xFDD0 && codePoint <= 0xFDEF) {
            return true;
        }

        return (codePoint & 0xFFFF) == 0xFFFE || (codePoint & 0xFFFF) == 0xFFFF;
    }

    static int advancePastLineTerminator(final CharSequence source, final int position, final int length) {
        final int next = position + 1;
        if (next < length && source.charAt(position) == '\r' && source.charAt(next) == '\n') {
            return next + 1;
        }

        return next;
    }

    static boolean isNotLineTerminator(final char character) {
        return !isLineTerminator((int) character);
    }

    static boolean isLineTerminator(final char character) {
        return isLineTerminator((int) character);
    }

    private static boolean isLineTerminator(final int codePoint) {
        switch (codePoint) {
            case '\n':
            case '\r':
            case '\f':
            case '\u000B': // VT
            case '\u0085': // NEL
            case '\u2028': // LS
            case '\u2029': // PS
                return true;
            default:
                return false;
        }
    }

}
