package edu.algosteps.judge;

/** A student-facing syntax or execution error from the practice language. */
public final class JudgeException extends RuntimeException {
    JudgeException(String message) {
        super(message);
    }

    JudgeException(int line, String message) {
        this("Line " + line + ": " + message);
    }
}
