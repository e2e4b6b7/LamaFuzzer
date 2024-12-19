package lama.fuzzer

import lama.fuzzer.ast.ScopeExpr
import lama.fuzzer.ast.SkipExpr
import lama.fuzzer.executors.DockerExecutor
import lama.fuzzer.mutaturs.SimpleMutator
import java.nio.file.Files
import java.nio.file.Path

fun readSeeds(): List<String> = Files.list(Path.of("seeds"))
    .filter { Files.isRegularFile(it) }
    .map { Files.readString(it) }
    .toList()

fun main(args: Array<String>) {
    val mutator = SimpleMutator()
    val executor = DockerExecutor(args[0])
    val seeds = listOf(ScopeExpr(emptyList(), SkipExpr))
    val fuzzer = EvolutionaryFuzzer(executor, mutator, seeds)
    fuzzer.evolve(10_000_000)
}
