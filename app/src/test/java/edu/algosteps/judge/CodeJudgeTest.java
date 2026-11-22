package edu.algosteps.judge;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

public class CodeJudgeTest {
    private static final String HEADER = "def solve(nums, target):\n";

    private static int run(String body, int target, Integer... nums) {
        return MiniPython.run(MiniPython.compile(HEADER + body), Arrays.asList(nums), target);
    }

    private static void rejects(String body, String reason) {
        try {
            run(body, 1, -3, 4);
            fail("Expected a judge error.");
        } catch (JudgeException error) {
            assertTrue(error.getMessage(), error.getMessage().contains(reason));
        }
    }

    @Test public void linearSearchReturnsFirstDuplicateAndHandlesEmpty() {
        String program = "    for i in range(len(nums)):\n" +
                         "        if nums[i] == target:\n" +
                         "            return i\n" +
                         "    return -1";
        assertEquals(1, run(program, 7, 3, 7, 7));
        assertEquals(-1, run(program, 7));
    }

    @Test public void binarySearchWorksAcrossSmallSortedInputs() {
        String program = "    low = 0\n" +
                         "    high = len(nums) - 1\n" +
                         "    while low <= high:\n" +
                         "        middle = (low + high) // 2\n" +
                         "        if nums[middle] == target:\n" +
                         "            return middle\n" +
                         "        elif nums[middle] < target:\n" +
                         "            low = middle + 1\n" +
                         "        else:\n" +
                         "            high = middle - 1\n" +
                         "    return -1";
        List<Integer> values = Arrays.asList(-8, -5, -1, 3, 7, 11);
        for (int target = -10; target <= 12; target++) {
            assertEquals(values.indexOf(target), MiniPython.run(MiniPython.compile(HEADER + program), values, target));
        }
    }

    @Test public void forwardBackwardAndNestedLoopsRespectLoopControl() {
        String reverse = "    for i in range(len(nums) - 1, -1, -1):\n" +
                         "        if nums[i] == target:\n" +
                         "            return i\n" +
                         "    return -1";
        assertEquals(3, run(reverse, 2, 2, 2, 4, 2));
        String nested = "    count = 0\n" +
                        "    for x in nums:\n" +
                        "        if x < 0:\n" +
                        "            continue\n" +
                        "        for i in range(x):\n" +
                        "            if i == target:\n" +
                        "                break\n" +
                        "            count += 1\n" +
                        "    return count";
        assertEquals(5, run(nested, 3, 2, -1, 5));
    }

    @Test public void arithmeticAndShortCircuitMatchDocumentedSemantics() {
        assertEquals(-3, run("    return -7 // 3", 0));
        assertEquals(-1, run("    return 5 % -3", 0));
        assertEquals(1, run("    return -4 < nums[-1] <= 0", 0, -3));
        assertEquals(0, run("    return 3 < 2 < missing", 0));
        assertEquals(9, run("    return target or nums[999]", 9));
        assertEquals(0, run("    return target and 1 // 0", 0));
        assertEquals(Integer.MIN_VALUE, run("    return -2147483648", 0));
        assertEquals(17, run("    return 2 + 3 * 5   # inline comment", 0));
    }

    @Test public void invalidStudentInputReportsLineAndReason() {
        rejects("    return nums[2]", "Line 2: Index 2");
        rejects("    if True:\n    return 0", "Line 2: Expected an indented block");
        rejects("    return missing", "Line 2: Unknown variable");
        rejects("    return 2147483647 + 1", "32-bit");
        rejects("    return (5 // 0)", "Division by zero");
        rejects("    x = 3", "without returning");
        rejects("    break", "inside a loop");
        rejects("    for i in range(0, 2, 0):\n        pass\n    return 1", "step cannot be zero");
        rejects("    return 1\n    import os", "Line 3: Unsupported");
    }

    @Test public void statementKeywordsAcceptParenthesesAndAdjacentOperators() {
        String program = "    i = 0\n" +
                         "    while(i < len(nums)):\n" +
                         "        if(nums[i] < target):\n" +
                         "            i += 1\n" +
                         "        elif(nums[i] == target):\n" +
                         "            return(i)\n" +
                         "        else :\n" +
                         "            return-1\n" +
                         "    return-1";
        assertEquals(1, run(program, 4, -3, 4));
        assertEquals(-1, run(program, 3, -3, 4));
        assertEquals(2, run("    return_value = 2\n    return(return_value)", 0));
    }

    @Test public void pythonKeywordsCannotBecomeNamesEvenInSkippedExpressions() {
        rejects("    class = 4\n    return class", "Line 2: Cannot use 'class'");
        rejects("    for None in nums:\n        pass\n    return 2", "Line 2: Cannot use 'None'");
        rejects("    return True or else", "Line 2: Unexpected keyword 'else'");
        assertEquals(2, run("    classified = 2\n    return classified", 0));
    }

    @Test public void augmentedAssignmentChecksCurrentValueBeforeEvaluatingItsRightSide() {
        rejects("    count += nums[99]\n    return count", "Line 2: Unknown variable 'count'");
        rejects("    count = 0\n    count += nums[99]\n    return count", "Line 3: Index 99");
    }

    @Test public void nonzeroDecimalLiteralsRejectLeadingZerosDuringParsing() {
        rejects("    return 010", "Line 2: Nonzero decimal integers cannot start with zero");
        rejects("    return True or -007", "Line 2: Nonzero decimal integers cannot start with zero");
        assertEquals(0, run("    return 000", 0));
        assertEquals(10, run("    return 10", 0));
    }

    @Test public void returnDetectionCountsParsedStatementsAcrossAllBranches() {
        assertFalse(MiniPython.compile(HEADER + "    return_value = 2  # return 0").hasReturnStatement);
        assertTrue(MiniPython.compile(HEADER + "    if False:\n        return(1)").hasReturnStatement);
    }

    @Test public void infiniteLoopAndDeepParsingHaveBoundedCost() {
        rejects("    while True:\n        pass", "Execution limit");
        rejects("    return (((((((((((((((((1)))))))))))))))))", "16 nested");
        rejects("    for i in range(2147483647):\n        pass\n    return 0", "Execution limit");
    }
}
