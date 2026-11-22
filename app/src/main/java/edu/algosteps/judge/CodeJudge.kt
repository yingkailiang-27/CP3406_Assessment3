package edu.algosteps.judge

/** Runs a single test case. The practice UI calls this on a background dispatcher. */
object CodeJudge {
    fun run(source: String, nums: List<Int>, target: Int): Int =
        compile(source).run(nums, target)

    internal fun compile(source: String): Program = Program(MiniPython.compile(source))

    internal class Program(private val block: MiniPython.Block) {
        val hasReturnStatement: Boolean get() = block.hasReturnStatement

        fun run(nums: List<Int>, target: Int): Int = MiniPython.run(block, nums, target)
    }
}
