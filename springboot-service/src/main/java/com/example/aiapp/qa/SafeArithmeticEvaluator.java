package com.example.aiapp.qa;

/**
 * Evaluates a restricted arithmetic expression - numbers and + - * / ** % only, plus
 * parentheses and unary +/-. No identifiers or function calls are recognized, so this
 * cannot execute arbitrary code the way a general-purpose scripting engine could. Ported
 * from fastapi-service/qa_graph/tools.py's ast-restricted-operator evaluator.
 */
final class SafeArithmeticEvaluator {

    private final String expr;
    private int pos;

    SafeArithmeticEvaluator(String expression) {
        this.expr = expression;
        this.pos = 0;
    }

    double evaluate() {
        double result = parseExpr();
        skipWhitespace();
        if (pos != expr.length()) {
            throw new IllegalArgumentException("Unexpected character at position " + pos);
        }
        return result;
    }

    private double parseExpr() {
        double value = parseTerm();
        while (true) {
            skipWhitespace();
            if (peek('+')) {
                pos++;
                value += parseTerm();
            } else if (peek('-')) {
                pos++;
                value -= parseTerm();
            } else {
                break;
            }
        }
        return value;
    }

    private double parseTerm() {
        double value = parsePower();
        while (true) {
            skipWhitespace();
            if (peek('*') && !peekAhead("**")) {
                pos++;
                value *= parsePower();
            } else if (peek('/')) {
                pos++;
                value /= parsePower();
            } else if (peek('%')) {
                pos++;
                value %= parsePower();
            } else {
                break;
            }
        }
        return value;
    }

    private double parsePower() {
        double base = parseUnary();
        skipWhitespace();
        if (peekAhead("**")) {
            pos += 2;
            return Math.pow(base, parsePower());
        }
        return base;
    }

    private double parseUnary() {
        skipWhitespace();
        if (peek('+')) {
            pos++;
            return parseUnary();
        }
        if (peek('-')) {
            pos++;
            return -parseUnary();
        }
        return parsePrimary();
    }

    private double parsePrimary() {
        skipWhitespace();
        if (peek('(')) {
            pos++;
            double value = parseExpr();
            skipWhitespace();
            if (!peek(')')) {
                throw new IllegalArgumentException("Expected ')'");
            }
            pos++;
            return value;
        }
        int start = pos;
        while (pos < expr.length() && (Character.isDigit(expr.charAt(pos)) || expr.charAt(pos) == '.')) {
            pos++;
        }
        if (pos == start) {
            throw new IllegalArgumentException("Expected a number at position " + pos);
        }
        return Double.parseDouble(expr.substring(start, pos));
    }

    private boolean peek(char c) {
        return pos < expr.length() && expr.charAt(pos) == c;
    }

    private boolean peekAhead(String s) {
        return expr.regionMatches(pos, s, 0, s.length());
    }

    private void skipWhitespace() {
        while (pos < expr.length() && Character.isWhitespace(expr.charAt(pos))) {
            pos++;
        }
    }
}
