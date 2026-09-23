package com.longdrange.ldattribute.spell;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * 簡單算術表達式解析器（遞歸下降）
 * 支援：+ - * / % ( ) 數字 變量
 *
 * 例："{base} + {magic_damage} * 1.5"
 */
public class FormulaEvaluator {

    public static double eval(String expr, Map<String, Double> vars) {
        if (expr == null || expr.isEmpty()) return 0;
        try {
            String s = expr;
            for (Map.Entry<String, Double> e : vars.entrySet()) {
                s = s.replace("{" + e.getKey() + "}", String.valueOf(e.getValue()));
                s = s.replaceAll("\\b" + Pattern.quote(e.getKey()) + "\\b", String.valueOf(e.getValue()));
            }
            return new Parser(s).parse();
        } catch (Throwable t) {
            return 0;
        }
    }

    private static class Parser {
        private final String s;
        private int pos = 0;
        Parser(String s) { this.s = s; }

        double parse() { return parseExpr(); }

        double parseExpr() {
            double v = parseTerm();
            while (pos < s.length()) {
                skipSpace();
                if (pos >= s.length()) break;
                char c = s.charAt(pos);
                if (c == '+') { pos++; v += parseTerm(); }
                else if (c == '-') { pos++; v -= parseTerm(); }
                else break;
            }
            return v;
        }

        double parseTerm() {
            double v = parseFactor();
            while (pos < s.length()) {
                skipSpace();
                if (pos >= s.length()) break;
                char c = s.charAt(pos);
                if (c == '*') { pos++; v *= parseFactor(); }
                else if (c == '/') { pos++; double d = parseFactor(); v = d == 0 ? 0 : v / d; }
                else if (c == '%') { pos++; double d = parseFactor(); v = d == 0 ? 0 : v % d; }
                else break;
            }
            return v;
        }

        double parseFactor() {
            skipSpace();
            if (pos >= s.length()) return 0;
            char c = s.charAt(pos);
            if (c == '(') {
                pos++;
                double v = parseExpr();
                skipSpace();
                if (pos < s.length() && s.charAt(pos) == ')') pos++;
                return v;
            }
            if (c == '-') { pos++; return -parseFactor(); }
            if (c == '+') { pos++; return parseFactor(); }
            int start = pos;
            while (pos < s.length() && (Character.isDigit(s.charAt(pos)) || s.charAt(pos) == '.')) pos++;
            if (start == pos) { pos++; return 0; }
            try { return Double.parseDouble(s.substring(start, pos)); } catch (Exception e) { return 0; }
        }

        void skipSpace() { while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++; }
    }
}