package lama.fuzzer

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import lama.fuzzer.ast.ScopeExpr
import lama.fuzzer.ast.printScopeExpr
import lama.fuzzer.executors.Executor
import lama.fuzzer.mutaturs.Mutator
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import lama.fuzzer.ast.mutationPointsNumber

class EvolutionaryFuzzer(
    private val executor: Executor,
    private val mutator: Mutator,
    private val seeds: List<ScopeExpr>,
    private val populationSize: Int = 100,
    private val tournamentSize: Int = 10,
) {
    private val executorMutex = Mutex()
    private val printMutex = Mutex()
    private val aggregationMutex = Mutex()

    init {
        File("reports").deleteRecursively()
        Files.createDirectories(Path.of("reports"))
    }

    private fun report(input: String, result: Executor.Result) {
        val reporter = reporters.find { it.matchResult(result) }
        if (reporter != null) {
            reporter.report(input, result)
            return
        }
        println("Input: $input")
        println("Exit code: ${result.exitCode}")
        println("Stdout: ${result.stdOut}")
        println("Stderr: ${result.stdErr}")
        println("Coverage: ${result.coverage}")
        println()
    }

    fun evolve(timeout: Long = 1000) {
        val end = System.currentTimeMillis() + timeout
        val firstPopulation = buildList {
            addAll(seeds)
            repeat(populationSize - seeds.size) { add(mutator.mutate(seeds.random())) }
        }
        var currentGeneration = Generation(firstPopulation.map { runBlocking { it.evaluate() } })
        var generationCount = 0
        while (System.currentTimeMillis() < end) {
            val best = currentGeneration.individuals.maxBy { it.fitness }
            println("Generation $generationCount: best fitness ${best.fitness}")
            currentGeneration = buildNextGeneration(currentGeneration)
            generationCount++
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun buildNextGeneration(currentGeneration: Generation): Generation {
        val generation = mutableListOf<Individual>()
        runBlocking(newFixedThreadPoolContext(5, "parallel")) {
            repeat(populationSize) {
                launch {
                    val champion = currentGeneration.individuals
                        .shuffled().subList(0, tournamentSize)
                        .maxBy { it.fitness }.node
                    val mutant = mutator.mutate(champion)
                    val individual = mutant.evaluate()
                    aggregationMutex.withLock {
                        generation.add(individual)
                    }
                }
            }
        }
        return Generation(generation)
    }

    private suspend fun ScopeExpr.evaluate() = Individual(this, Fitness(execute(), mutationPointsNumber()))

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun ScopeExpr.execute(): Int {
        val printed = printScopeExpr(this)
        val result: Executor.Result
        executorMutex.withLock {
            result = executor.execute(printed)
        }
        if (result.exitCode != 0) {
            printMutex.withLock {
                report(printed, result)
            }
        }
        return result.coverage
    }

    private data class Fitness(val coverage: Int, val size: Int) : Comparable<Fitness> {
        override fun compareTo(other: Fitness): Int =
            (coverage - size / sizeWeight).compareTo(other.coverage - other.size / sizeWeight)

        companion object {
            const val sizeWeight = 32
        }
    }

    private data class Individual(val node: ScopeExpr, val fitness: Fitness)
    private data class Generation(val individuals: List<Individual>)
}
