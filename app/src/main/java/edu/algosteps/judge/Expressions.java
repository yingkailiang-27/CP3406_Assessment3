package edu.algosteps.judge;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.algosteps.judge.MiniPython.Expr;

/** Recursive descent parser. Syntax is checked even when a branch is never executed. */
final class Expressions {
    private static final Pattern TOKEN = Pattern.compile(
        "\\s+|[A-Za-z_][A-Za-z_0-9]*|[0-9]+|//|==|!=|<=|>=|[+*%()\\[\\],<>-]");
    private final List<String> tokens = new ArrayList<>();
    private final int line;
    private int current;
    private int depth;

    Expressions(String source, int line) {
        this.line = line;
        Matcher matcher = TOKEN.matcher(source);
        int index = 0;
        while (index < source.length()) {
            matcher.region(index, source.length());
            if (!matcher.lookingAt()) {
                throw error("Unsupported character '" + source.charAt(index) + "'.");
            }
            String token = matcher.group();
            if (!token.trim().isEmpty()) tokens.add(token);
            if (tokens.size() > 96) throw error("Split this expression into shorter assignments.");
            index = matcher.end();
        }
    }

    Expr parse() {
        Expr value = or();
        end();
        return value;
    }

    List<Expr> rangeArguments() {
        if (!accept("range")) throw error("Iterate using nums or range(...).");
        expect("(");
        List<Expr> args = new ArrayList<>();
        do {
            if (args.size() == 3) throw error("range accepts 1 to 3 arguments.");
            args.add(or());
        } while (accept(","));
        expect(")");
        end();
        return args;
    }

    private Expr or() {
        Expr value = and();
        while (accept("or")) {
            Expr left = value;
            Expr right = and();
            value = state -> {
                int result = left.eval(state);
                return result != 0 ? result : right.eval(state);
            };
        }
        return value;
    }

    private Expr and() {
        Expr value = not();
        while (accept("and")) {
            Expr left = value;
            Expr right = not();
            value = state -> {
                int result = left.eval(state);
                return result == 0 ? result : right.eval(state);
            };
        }
        return value;
    }

    private Expr not() {
        if (!accept("not")) return comparison();
        Expr value = not();
        return state -> value.eval(state) == 0 ? 1 : 0;
    }

    private Expr comparison() {
        Expr first = sum();
        List<Expr> following = new ArrayList<>();
        List<String> symbols = new ArrayList<>();
        while (oneOf("==", "!=", "<=", ">=", "<", ">")) {
            symbols.add(take());
            following.add(sum());
        }
        if (following.isEmpty()) return first;
        return state -> {
            int previous = first.eval(state);
            for (int i = 0; i < following.size(); i++) {
                int next = following.get(i).eval(state);
                if (!compare(symbols.get(i), previous, next)) return 0;
                previous = next;
            }
            return 1;
        };
    }

    private boolean compare(String symbol, int a, int b) {
        switch (symbol) {
            case "==": return a == b;
            case "!=": return a != b;
            case "<": return a < b;
            case ">": return a > b;
            case "<=": return a <= b;
            case ">=": return a >= b;
            default: throw new AssertionError(symbol);
        }
    }

    private Expr sum() {
        Expr value = product();
        while (oneOf("+", "-")) {
            Expr left = value;
            String symbol = take();
            Expr right = product();
            value = state -> arithmetic(symbol, left.eval(state), right.eval(state), line);
        }
        return value;
    }

    private Expr product() {
        Expr value = unary();
        while (oneOf("*", "//", "%")) {
            Expr left = value;
            String symbol = take();
            Expr right = unary();
            value = state -> arithmetic(symbol, left.eval(state), right.eval(state), line);
        }
        return value;
    }

    private Expr unary() {
        if (accept("+")) return unary();
        if (accept("-")) {
            if (accept("2147483648")) return state -> Integer.MIN_VALUE;
            Expr value = unary();
            return state -> checked(-(long)value.eval(state), line);
        }
        return atom();
    }

    private Expr atom() {
        if (accept("(")) {
            Expr value = nested();
            expect(")");
            return value;
        }
        if (accept("len")) {
            expect("(");
            expect("nums");
            expect(")");
            return state -> state.nums.size();
        }
        if (accept("nums")) {
            expect("[");
            Expr index = nested();
            expect("]");
            return state -> state.index(index.eval(state), line);
        }
        if (accept("True")) return state -> 1;
        if (accept("False")) return state -> 0;
        if (current == tokens.size()) throw error("Expected an integer, variable or expression.");
        String token = take();
        if (Character.isDigit(token.charAt(0))) {
            if (token.charAt(0) == '0') {
                for (int i = 1; i < token.length(); i++) {
                    if (token.charAt(i) != '0') {
                        throw error("Nonzero decimal integers cannot start with zero.");
                    }
                }
                return state -> 0;
            }
            try {
                int n = Integer.parseInt(token);
                return state -> n;
            } catch (NumberFormatException invalid) {
                throw error("Integer outside the signed 32-bit range.");
            }
        }
        if (MiniPython.KEYWORDS.contains(token)) throw error("Unexpected keyword '" + token + "'.");
        if (MiniPython.IDENTIFIER.matcher(token).matches()) return state -> state.read(token, line);
        throw error("Unexpected token '" + token + "'.");
    }

    private Expr nested() {
        if (++depth > 16) throw error("Use at most 16 nested expressions.");
        Expr value = or();
        depth--;
        return value;
    }

    static int arithmetic(String symbol, int a, int b, int line) {
        switch (symbol) {
            case "+": return checked((long)a + b, line);
            case "-": return checked((long)a - b, line);
            case "*": return checked((long)a * b, line);
            case "//":
            case "%":
                if (b == 0) throw new JudgeException(line, "Division by zero.");
                long quotient = (long)a / b;
                if (a % b != 0 && (a < 0) != (b < 0)) quotient--;
                return checked(symbol.equals("//") ? quotient : (long)a - quotient * b, line);
            default: throw new AssertionError(symbol);
        }
    }

    private static int checked(long n, int line) {
        if (n < Integer.MIN_VALUE || n > Integer.MAX_VALUE) {
            throw new JudgeException(line, "Result outside the signed 32-bit range.");
        }
        return (int)n;
    }

    private boolean oneOf(String... options) {
        if (current >= tokens.size()) return false;
        String token = tokens.get(current);
        for (String option : options) if (option.equals(token)) return true;
        return false;
    }

    private boolean accept(String token) {
        if (current >= tokens.size() || !tokens.get(current).equals(token)) return false;
        current++;
        return true;
    }

    private void expect(String token) {
        if (!accept(token)) throw error("Expected '" + token + "'.");
    }

    private void end() {
        if (current != tokens.size()) throw error("Unexpected token '" + tokens.get(current) + "'.");
    }

    private String take() { return tokens.get(current++); }
    private JudgeException error(String text) { return new JudgeException(line, text); }
}
