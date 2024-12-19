package lama.fuzzer.executors

import kotlin.io.path.absolute
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteExisting
import kotlin.io.path.writeText

class DockerExecutor(val imageName: String) : Executor {
    private fun createInputFile(inputFile: String) {
        val tempFile = createTempFile()

        try {
            tempFile.writeText(inputFile)

            val process = ProcessBuilder(
                "docker", "cp", tempFile.absolute().toString(), "$imageName:/home/opam/Lama/$inputFileName"
            ).start()

            val stdOut = process.inputStream.bufferedReader().readText()
            val stdErr = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode != 0) {
                throw RuntimeException(buildString {
                    appendLine("Failed to create input file. Exit code: $exitCode")
                    appendLine("Stdout:")
                    appendLine(stdOut)
                    appendLine("Stderr:")
                    appendLine(stdErr)
                })
            }
        } finally {
            tempFile.deleteExisting()
        }
    }

    private fun getCoverage(): Int {
        val coverageProcess = ProcessBuilder(
            "docker", "exec", imageName, "bash", "-c", "eval \$(opam env); bisect-ppx-report summary"
        ).start()

        val coverageStdOut = coverageProcess.inputStream.bufferedReader().readText()
        if (coverageProcess.waitFor() != 0) {
            throw RuntimeException("Failed to get coverage report")
        }

        val coverageOutputRegex = """Coverage: (\d+)/(\d+) \(([\d.]+)%\)""".toRegex()

        val matchResult = coverageOutputRegex.find(coverageStdOut)
        if (matchResult != null) {
            val (covered, total, percentage) = matchResult.destructured
            return covered.toInt()
        } else {
            throw RuntimeException("Failed to parse coverage report")
        }
    }

    private fun clearCoverage() {
        val process = ProcessBuilder(
            "docker", "exec", imageName, "bash", "-c", "find . -name '*.coverage' | xargs rm -f"
        ).start()

        if (process.waitFor() != 0) {
            throw RuntimeException("Failed to clear coverage report")
        }
    }

    override fun execute(inputFile: String): Executor.Result {
        createInputFile(inputFile)

        val process = ProcessBuilder(
            "docker", "exec", imageName, "bash", "-c",
            "LAMA=runtime ./_build/install/default/bin/lamac -I runtime -o out $inputFileName"
        ).start()

        val stdOut = process.inputStream.bufferedReader().readText()
        val stdErr = process.errorStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        val coverage = getCoverage()
        clearCoverage()

        return Executor.Result(exitCode, stdOut, stdErr, coverage)
    }

    private companion object {
        const val inputFileName = "input.lama"
    }
}
