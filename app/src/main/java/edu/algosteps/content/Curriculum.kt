package edu.algosteps.content

data class Case(val nums: List<Int>, val target: Int, val expected: Int)

data class TraceStep(
    val title: String,
    val detail: String,
    val active: Set<Int> = emptySet(),
    val searchRange: IntRange? = null
)

data class Lesson(
    val id: String,
    val title: String,
    val summary: String,
    val difficulty: String,
    val prompt: String,
    val starter: String,
    val reference: String,
    val notes: List<String>,
    val samples: List<Case>,
    val checks: List<Case>,
    val idea: String,
    val invariant: String,
    val complexity: String,
    val targetLabel: String = "Target",
    val trace: (Case) -> List<TraceStep>
)

/** All prompts, traces, explanations and examples are authored for this app. */
object Curriculum {
    val lessons = listOf(
        Lesson(
            id = "linear-search",
            title = "Linear search",
            summary = "Scan an unsorted list to find the index of the first match.",
            difficulty = "Easy",
            prompt = "Return the zero-based index of the first value equal to target in nums. Return -1 if there is no match. The list may be empty and values may repeat.",
            starter = """
                def solve(nums, target):
                    # Inspect values from left to right.
                    return -1
            """.trimIndent(),
            reference = """
                def solve(nums, target):
                    i = 0
                    while i < len(nums):
                        if nums[i] == target:
                            return i
                        i = i + 1
                    return -1
            """.trimIndent(),
            notes = listOf(
                "Lines 1–2 · The function receives a list and a target. Set i to 0, the index of the first item.",
                "Line 3 · Check i before reading nums[i]. An empty list skips the loop.",
                "Lines 4–5 · Return immediately on a match. Stopping here guarantees the first matching index.",
                "Line 6 · A mismatch advances i by one. Without this line the search would repeatedly inspect the same item.",
                "Line 7 · Reaching this return means every item was checked."
            ),
            samples = listOf(Case(listOf(6, 2, 8, 2), 2, 1), Case(listOf(7, 3, 5), 8, -1)),
            checks = listOf(
                Case(emptyList(), 9, -1), Case(listOf(-4), -4, 0), Case(listOf(0, -7, -3), -3, 2),
                Case(listOf(4, 4, 4), 4, 0), Case(listOf(1, 3, 1), 2, -1), Case(listOf(0, -2, 0, -2), -2, 1)
            ),
            idea = "Start at index 0 and inspect one value at a time. The values are unsorted, so a comparison only rules out the item you just checked.",
            invariant = "Before checking index i, every index smaller than i has already been checked and does not match. This is why returning i on the first match is correct.",
            complexity = "O(n) time in the worst case and O(1) extra space. A first-item match takes one comparison.",
            trace = ::linearTrace
        ),
        Lesson(
            id = "count-occurrences",
            title = "Count occurrences",
            summary = "Count how many times a target appears in one pass.",
            difficulty = "Easy",
            prompt = "Return how many values in nums equal target. Count every occurrence, including adjacent duplicates. An empty list has zero matches.",
            starter = """
                def solve(nums, target):
                    count = 0
                    # Update count each time an item matches.
                    return count
            """.trimIndent(),
            reference = """
                def solve(nums, target):
                    count = 0
                    i = 0
                    while i < len(nums):
                        if nums[i] == target:
                            count = count + 1
                        i = i + 1
                    return count
            """.trimIndent(),
            notes = listOf(
                "Lines 2–3 · An empty prefix contains zero matches. i marks the next item to read.",
                "Line 4 · Enter the loop only when i is a valid index.",
                "Lines 5–6 · A matching value contributes exactly one to count.",
                "Line 7 · Move to the next index on both matches and misses. This line is outside the if block.",
                "Line 8 · Return after the loop so later matches are included."
            ),
            samples = listOf(Case(listOf(3, 2, 3, 3, 1), 3, 3), Case(listOf(-1, 0, 2), 4, 0)),
            checks = listOf(
                Case(emptyList(), 0, 0), Case(listOf(6), 6, 1), Case(listOf(-2, -2, -2, -2), -2, 4),
                Case(listOf(1, 0, 1, 0, 1), 0, 2), Case(listOf(4, 5, 6), 2, 0), Case(listOf(9, 8, 9), 9, 2)
            ),
            idea = "Scan the list once. A match increases the count by one. A miss leaves it alone. Keep scanning after a match because the same value might appear again.",
            invariant = "At the start of each iteration, count equals the number of matches in the part of the list already scanned. Processing the next item preserves that statement.",
            complexity = "O(n) time and O(1) extra space. Even if the first item matches, the remaining items can change the answer.",
            trace = ::countTrace
        ),
        Lesson(
            id = "binary-search",
            title = "Binary search",
            summary = "Find a target in a sorted list by halving the search interval.",
            difficulty = "Medium",
            prompt = "nums contains distinct integers in increasing order. Return the zero-based index of target, or -1 if it is absent. nums may be empty.",
            starter = """
                def solve(nums, target):
                    low = 0
                    high = len(nums) - 1
                    # Narrow the interval until you find target or it is empty.
                    return -1
            """.trimIndent(),
            reference = """
                def solve(nums, target):
                    low = 0
                    high = len(nums) - 1
                    while low <= high:
                        mid = low + (high - low) // 2
                        if nums[mid] == target:
                            return mid
                        if nums[mid] < target:
                            low = mid + 1
                        else:
                            high = mid - 1
                    return -1
            """.trimIndent(),
            notes = listOf(
                "Lines 2–4 · low and high are inclusive. The last valid index is len(nums) - 1. A one-item interval must still be checked.",
                "Line 5 · Integer division (//) gives a whole-number offset from low toward high.",
                "Lines 6–7 · If the middle item equals target, its index is the answer.",
                "Lines 8–11 · Exclude mid after comparing. Smaller middle values move low right, larger ones move high left.",
                "Line 12 · The interval has become empty, so the target was absent."
            ),
            samples = listOf(Case(listOf(2, 4, 7, 11, 16, 23), 16, 4), Case(listOf(1, 5, 8, 12), 6, -1)),
            checks = listOf(
                Case(emptyList(), 1, -1), Case(listOf(5), 5, 0), Case(listOf(-9, -3, 2, 4), -9, 0),
                Case(listOf(-8, -7, -3, 0, 10), 10, 4), Case(listOf(1, 4, 8), 11, -1), Case(listOf(-6, 0, 2), -7, -1)
            ),
            idea = "Look at the middle of the remaining interval. If the middle value is too small, sorted order rules out everything to its left as well. If it is too large, rule out the right half.",
            invariant = "If the target exists and has not been found, its index lies between low and high inclusive. Each comparison removes only indices that cannot hold the target.",
            complexity = "O(log n) time and O(1) extra space. Each unsuccessful comparison leaves at most half the previous items to check.",
            trace = ::binaryTrace
        ),
        Lesson(
            id = "window-sum",
            title = "Fixed-size sliding window",
            summary = "Find the largest sum of k neighbouring values by updating a running sum.",
            difficulty = "Medium",
            prompt = "Here target is a window width k, with 1 ≤ k ≤ len(nums). Return the largest sum of any k consecutive values. Values may be negative. Return the sum itself.",
            starter = """
                def solve(nums, target):
                    # Sum the first window, then move it one position at a time.
                    return 0
            """.trimIndent(),
            reference = """
                def solve(nums, target):
                    total = 0
                    i = 0
                    while i < target:
                        total = total + nums[i]
                        i = i + 1
                    best = total
                    while i < len(nums):
                        total = total - nums[i - target] + nums[i]
                        if total > best:
                            best = total
                        i = i + 1
                    return best
            """.trimIndent(),
            notes = listOf(
                "Lines 2–6 · Build the first complete window. At loop exit i equals the width, so it points to the next incoming item.",
                "Line 7 · Start best at the first actual sum. Starting it at zero would break all-negative examples.",
                "Lines 8–9 · For incoming position i, position i - target leaves the window. Subtract its value and add nums[i].",
                "Lines 10–12 · Compare the new total with best and advance one index whether or not best changed.",
                "Line 13 · Return the largest complete window sum. When k equals the whole list length the second loop never runs."
            ),
            samples = listOf(Case(listOf(4, -2, 5, 1, -3), 2, 6), Case(listOf(-5, -2, -8), 2, -7)),
            checks = listOf(
                Case(listOf(-9), 1, -9), Case(listOf(1, -4, 2), 3, -1), Case(listOf(0, 7, -3, 4), 1, 7),
                Case(listOf(5, 4, -8, 1), 2, 9), Case(listOf(-1, -2, 5, 6), 2, 11), Case(listOf(1, 2, 1, 2), 2, 3)
            ),
            idea = "Two adjacent windows of the same width share all but one item. When moving right, subtract the value leaving on the left and add the value entering on the right.",
            invariant = "After each move, total is the sum of exactly the current k values and best is the largest sum of every complete window visited so far.",
            complexity = "O(n) time and O(1) extra space. The first window needs k additions. Each of the remaining n − k windows needs a constant number of operations.",
            targetLabel = "Window width",
            trace = ::windowTrace
        )
    )

    fun byId(id: String): Lesson = lessons.first { it.id == id }
}

private fun linearTrace(case: Case): List<TraceStep> = buildList {
    for ((index, value) in case.nums.withIndex()) {
        if (value == case.target) {
            add(TraceStep("Return index $index", "$value matches the target. Every earlier value was different.", setOf(index)))
            return@buildList
        }
        add(TraceStep("Inspect index $index", "$value differs from ${case.target}. Continue to the next index.", setOf(index)))
    }
    add(TraceStep("Return -1", "The list is exhausted without a match."))
}

private fun countTrace(case: Case): List<TraceStep> = buildList {
    var count = 0
    for ((index, value) in case.nums.withIndex()) {
        val matches = value == case.target
        if (matches) count += 1
        val action = if (matches) "matches, so increase the count to" else "does not match, so keep the count at"
        add(TraceStep("Inspect index $index", "$value $action $count.", setOf(index)))
    }
    add(TraceStep("Return $count", "Every item has now contributed either one match or zero."))
}

private fun binaryTrace(case: Case): List<TraceStep> = buildList {
    var low = 0
    var high = case.nums.lastIndex
    while (low <= high) {
        val mid = low + (high - low) / 2
        val value = case.nums[mid]
        val range = low..high
        val title = "Search indices $low through $high"
        when {
            value == case.target -> {
                add(TraceStep(title, "Middle index $mid contains $value, the target. Return $mid.", setOf(mid), range))
                return@buildList
            }
            value < case.target -> {
                add(TraceStep(title, "Index $mid holds $value, below ${case.target}. Discard indices $low through $mid and set low to ${mid + 1}.", setOf(mid), range))
                low = mid + 1
            }
            else -> {
                add(TraceStep(title, "Index $mid holds $value, above ${case.target}. Discard indices $mid through $high and set high to ${mid - 1}.", setOf(mid), range))
                high = mid - 1
            }
        }
    }
    add(TraceStep("Return -1", "low is $low and high is $high. No indices remain between them.", searchRange = IntRange.EMPTY))
}

private fun windowTrace(case: Case): List<TraceStep> = buildList {
    val width = case.target
    var total = case.nums.take(width).sum()
    var best = total
    add(TraceStep("Build the first window", "The first $width values total $total. Start the best sum at $best.", (0 until width).toSet()))
    for (right in width until case.nums.size) {
        val outgoing = case.nums[right - width]
        val incoming = case.nums[right]
        val previous = total
        total = total - outgoing + incoming
        best = maxOf(best, total)
        val left = right - width + 1
        add(TraceStep("Move to indices $left through $right", "Start from $previous, subtract ($outgoing) and add ($incoming) to get $total. The best sum is $best.", (left..right).toSet()))
    }
    add(TraceStep("Return $best", "Every complete window was compared once."))
}
