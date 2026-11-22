package edu.algosteps.judge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A small, closed language for integer-array exercises. Student text is parsed as
 * statements and expressions; it never reaches a platform compiler or shell.
 */
final class MiniPython {
    static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z_0-9]*");
    // Python's hard keywords cannot become variables, even if the related statement is outside our subset.
    static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList((
        "False None True and as assert async await break class continue def del elif else except finally for " +
        "from global if import in is lambda nonlocal not or pass raise return try while with yield"
    ).split(" ")));
    private static final Pattern HEADER = Pattern.compile(
        "def +solve\\s*\\(\\s*nums\\s*,\\s*target\\s*\\)\\s*:");
    private static final Pattern FOR = Pattern.compile(
        "for +([A-Za-z_][A-Za-z_0-9]*) +in +(.+)\\s*:");
    private static final Pattern ASSIGNMENT = Pattern.compile(
        "([A-Za-z_][A-Za-z_0-9]*)\\s*(\\+=|-=|\\*=|//=|%=|=(?!=))\\s*(.+)");
    private static final int MAX_SOURCE_LENGTH = 8192;
    private static final int MAX_LINES = 160;

    private MiniPython() {}

    static Block compile(String source) {
        if (source.length() > MAX_SOURCE_LENGTH) {
            throw new JudgeException("Keep your program under 8,193 characters.");
        }
        return new Parser(source).program();
    }

    static int run(Block program, List<Integer> nums, int target) {
        if (nums.size() > 1024) throw new JudgeException("Input exceeds the 1,024-item limit.");
        State state = new State(nums, target);
        try {
            program.execute(state);
        } catch (Returned result) {
            return result.value;
        }
        throw new JudgeException("solve finished without returning an integer.");
    }

    static final class State {
        final List<Integer> nums;
        private final Map<String, Integer> locals = new HashMap<>();
        private int steps = 30_000;
        private final long started = System.nanoTime();

        State(List<Integer> nums, int target) {
            this.nums = nums;
            locals.put("target", target);
        }

        void tick(int line) {
            if (--steps < 0 || ((steps & 63) == 0 && System.nanoTime() - started > 2_000_000_000L)) {
                throw new JudgeException(line,
                    "Execution limit reached. Check your loop conditions and updates.");
            }
        }

        int read(String name, int line) {
            Integer value = locals.get(name);
            if (value == null) throw new JudgeException(line, "Unknown variable '" + name + "'.");
            return value;
        }

        int index(int index, int line) {
            int actual = index < 0 ? nums.size() + index : index;
            if (actual < 0 || actual >= nums.size()) {
                throw new JudgeException(line, "Index " + index + " is outside nums (length " + nums.size() + ").");
            }
            return nums.get(actual);
        }
    }

    interface Expr { int eval(State state); }
    private interface Action { void run(State state); }

    private static final class Statement {
        final int line;
        final Action action;

        Statement(int line, Action action) {
            this.line = line;
            this.action = action;
        }
    }

    static final class Block {
        final List<Statement> statements = new ArrayList<>();
        boolean hasReturnStatement;

        void execute(State state) {
            for (Statement statement : statements) {
                state.tick(statement.line);
                statement.action.run(state);
            }
        }
    }

    private static final class Returned extends RuntimeException {
        final int value;

        Returned(int value) {
            super(null, null, false, false);
            this.value = value;
        }
    }

    private static final class LoopSignal extends RuntimeException {
        static final LoopSignal BREAK = new LoopSignal();
        static final LoopSignal CONTINUE = new LoopSignal();

        private LoopSignal() { super(null, null, false, false); }
    }

    private static final class Line {
        final int number;
        final int indent;
        final String text;

        Line(int number, int indent, String text) {
            this.number = number;
            this.indent = indent;
            this.text = text;
        }
    }

    private static final class Parser {
        private final List<Line> lines = new ArrayList<>();
        private int position;
        private boolean parsedReturn;

        Parser(String source) {
            String[] raw = source.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
            if (raw.length > MAX_LINES) throw new JudgeException("This runner supports up to 160 source lines.");
            for (int i = 0; i < raw.length; i++) {
                String line = raw[i];
                int comment = line.indexOf('#');
                if (comment >= 0) line = line.substring(0, comment);
                if (line.trim().isEmpty()) continue;
                if (line.indexOf('\t') >= 0) {
                    throw new JudgeException(i + 1, "Use spaces for indentation instead of tabs.");
                }
                int indent = 0;
                while (indent < line.length() && line.charAt(indent) == ' ') indent++;
                lines.add(new Line(i + 1, indent, line.trim()));
            }
        }

        Block program() {
            if (lines.isEmpty() || lines.get(0).indent != 0 || !HEADER.matcher(lines.get(0).text).matches()) {
                throw new JudgeException(lines.isEmpty() ? 1 : lines.get(0).number,
                    "Start with def solve(nums, target):");
            }
            Line header = lines.get(position++);
            Block body = child(header, 1, 0);
            if (position != lines.size()) throw error(lines.get(position), "Keep all statements inside solve.");
            body.hasReturnStatement = parsedReturn;
            return body;
        }

        private Block child(Line parent, int depth, int loopDepth) {
            if (depth > 16) throw error(parent, "Use at most 16 nested blocks.");
            if (position == lines.size() || lines.get(position).indent <= parent.indent) {
                throw error(parent, "Expected an indented block below this line.");
            }
            int indent = lines.get(position).indent;
            Block result = new Block();
            while (position < lines.size()) {
                Line line = lines.get(position);
                if (line.indent < indent) break;
                if (line.indent > indent) throw error(line, "Unexpected indentation.");
                position++;
                result.statements.add(new Statement(line.number, statement(line, depth, loopDepth)));
            }
            return result;
        }

        private Action statement(Line line, int depth, int loopDepth) {
            String text = line.text;
            if (text.equals("pass")) return state -> {};
            if (text.equals("break") || text.equals("continue")) {
                if (loopDepth == 0) throw error(line, text + " must be inside a loop.");
                return state -> { throw text.equals("break") ? LoopSignal.BREAK : LoopSignal.CONTINUE; };
            }
            if (startsKeyword(text, "return")) {
                Expr value = expr(line, text.substring("return".length()));
                parsedReturn = true;
                return state -> { throw new Returned(value.eval(state)); };
            }
            if (startsKeyword(text, "if")) {
                return conditional(line, depth, loopDepth);
            }
            if (startsKeyword(text, "while")) {
                Expr condition = condition(line, "while");
                Block body = child(line, depth + 1, loopDepth + 1);
                return state -> {
                    while (condition.eval(state) != 0) {
                        state.tick(line.number);
                        if (!iterate(body, state)) break;
                    }
                };
            }
            Matcher forLoop = FOR.matcher(text);
            if (forLoop.matches()) {
                String name = forLoop.group(1);
                checkName(line, name);
                String sequence = forLoop.group(2).trim();
                Block body = child(line, depth + 1, loopDepth + 1);
                if (sequence.equals("nums")) {
                    return state -> {
                        for (int n : state.nums) {
                            state.tick(line.number);
                            state.locals.put(name, n);
                            if (!iterate(body, state)) break;
                        }
                    };
                }
                List<Expr> arguments = new Expressions(sequence, line.number).rangeArguments();
                return state -> {
                    int first = arguments.get(0).eval(state);
                    int start = arguments.size() == 1 ? 0 : first;
                    int stop = arguments.size() == 1 ? first : arguments.get(1).eval(state);
                    int step = arguments.size() == 3 ? arguments.get(2).eval(state) : 1;
                    if (step == 0) throw error(line, "range step cannot be zero.");
                    // Long iteration prevents wrapping when the last step crosses an Int boundary.
                    for (long i = start; step > 0 ? i < stop : i > stop; i += step) {
                        state.tick(line.number);
                        state.locals.put(name, (int)i);
                        if (!iterate(body, state)) break;
                    }
                };
            }
            Matcher assignment = ASSIGNMENT.matcher(text);
            if (assignment.matches()) {
                String name = assignment.group(1);
                checkName(line, name);
                String operator = assignment.group(2);
                Expr value = expr(line, assignment.group(3));
                if (operator.equals("=")) return state -> state.locals.put(name, value.eval(state));
                String op = operator.substring(0, operator.length() - 1);
                return state -> {
                    int current = state.read(name, line.number);
                    int next = Expressions.arithmetic(op, current, value.eval(state), line.number);
                    state.locals.put(name, next);
                };
            }
            throw error(line, "Unsupported statement. Try an assignment, condition, loop or return.");
        }

        private Action conditional(Line first, int depth, int loopDepth) {
            List<Expr> conditions = new ArrayList<>();
            List<Block> branches = new ArrayList<>();
            conditions.add(condition(first, "if"));
            branches.add(child(first, depth + 1, loopDepth));
            while (position < lines.size()) {
                Line next = lines.get(position);
                if (next.indent != first.indent || !startsKeyword(next.text, "elif")) break;
                position++;
                conditions.add(condition(next, "elif"));
                branches.add(child(next, depth + 1, loopDepth));
            }
            Block fallback = new Block();
            if (position < lines.size()) {
                Line next = lines.get(position);
                if (next.indent == first.indent && startsKeyword(next.text, "else") &&
                    next.text.substring("else".length()).trim().equals(":")) {
                    position++;
                    fallback = child(next, depth + 1, loopDepth);
                }
            }
            final Block otherwise = fallback;
            return state -> {
                for (int i = 0; i < conditions.size(); i++) {
                    if (conditions.get(i).eval(state) != 0) {
                        branches.get(i).execute(state);
                        return;
                    }
                }
                otherwise.execute(state);
            };
        }

        private Expr condition(Line line, String keyword) {
            if (!line.text.endsWith(":")) throw error(line, "Finish this condition with a colon.");
            return expr(line, line.text.substring(keyword.length(), line.text.length() - 1));
        }

        private Expr expr(Line line, String source) {
            return new Expressions(source, line.number).parse();
        }

        private static boolean iterate(Block body, State state) {
            try {
                body.execute(state);
                return true;
            } catch (LoopSignal signal) {
                return signal == LoopSignal.CONTINUE;
            }
        }

        private static boolean startsKeyword(String text, String keyword) {
            if (!text.startsWith(keyword)) return false;
            if (text.length() == keyword.length()) return true;
            char next = text.charAt(keyword.length());
            return next != '_' && !Character.isLetterOrDigit(next);
        }

        private static void checkName(Line line, String name) {
            if (KEYWORDS.contains(name) || name.equals("nums") || name.equals("len") ||
                name.equals("range") || name.equals("solve")) {
                throw error(line, "Cannot use '" + name + "' as a variable name.");
            }
        }

        private static JudgeException error(Line line, String message) {
            return new JudgeException(line.number, message);
        }
    }
}
