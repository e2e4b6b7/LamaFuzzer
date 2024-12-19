package lama.fuzzer.executors

interface Executor {
    fun execute(inputFile: String): Result

    data class Result(val exitCode: Int, val stdOut: String, val stdErr: String, val coverage: Int)
}
