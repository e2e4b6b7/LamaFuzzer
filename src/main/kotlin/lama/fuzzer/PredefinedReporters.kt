package lama.fuzzer

import lama.fuzzer.executors.Executor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

interface ResultReporter {
    fun matchResult(result: Executor.Result): Boolean
    fun report(input: String, result: Executor.Result)
}

private object SyntaxErrorReporter : ResultReporter {
    override fun matchResult(result: Executor.Result) = result.exitCode == 255 && result.coverage == 70

    override fun report(input: String, result: Executor.Result) {
        val report = buildString {
            append("Input: $input\n")
            append("Stdout: ${result.stdOut}\n")
            append("Stderr: ${result.stdErr}\n\n")
        }
        Files.writeString(
            Path.of("reports", "syntax-error.txt"),
            report,
            StandardOpenOption.APPEND,
            StandardOpenOption.CREATE
        )
    }
}

private object AlreadyDefinedReporter : ResultReporter {
    override fun matchResult(result: Executor.Result) =
        result.exitCode == 255 && result.stdOut.contains("s already defined in the scope at")

    override fun report(input: String, result: Executor.Result) {
        val report = buildString {
            append("Input: $input\n")
            append("Stdout: ${result.stdOut}\n")
            append("Stderr: ${result.stdErr}\n\n")
        }
        Files.writeString(
            Path.of("reports", "already-defined.txt"),
            report,
            StandardOpenOption.APPEND,
            StandardOpenOption.CREATE
        )
    }
}

private object UndefinedNameReporter : ResultReporter {
    override fun matchResult(result: Executor.Result) =
        result.exitCode == 255 && result.stdOut.contains("Error: undefined name")

    override fun report(input: String, result: Executor.Result) {
        val report = buildString {
            append("Input: $input\n")
            append("Stdout: ${result.stdOut}\n")
            append("Stderr: ${result.stdErr}\n\n")
        }
        Files.writeString(
            Path.of("reports", "undefined-name.txt"),
            report,
            StandardOpenOption.APPEND,
            StandardOpenOption.CREATE
        )
    }
}

private object IndirectAssignmentReporter : ResultReporter {
    override fun matchResult(result: Executor.Result) =
        result.stdErr.contains("Indirect assignment is not supported yet:")

    override fun report(input: String, result: Executor.Result) {
        val report = buildString {
            append("Input: $input\n")
            append("Stdout: ${result.stdOut}\n")
            append("Stderr: ${result.stdErr}\n\n")
        }
        Files.writeString(
            Path.of("reports", "indirect-assignment.txt"),
            report,
            StandardOpenOption.APPEND,
            StandardOpenOption.CREATE
        )
    }
}

val reporters = listOf<ResultReporter>(
    SyntaxErrorReporter,
    AlreadyDefinedReporter,
    UndefinedNameReporter,
    IndirectAssignmentReporter,
)
