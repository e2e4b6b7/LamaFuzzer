package lama.fuzzer.ast

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class Scope {
    val tower: MutableList<MutableList<MutableSet<Identifier>>> = ArrayList()
    val set: MutableSet<Identifier> = HashSet()

    fun pushLevel() {
        tower.add(mutableListOf(mutableSetOf()))
    }

    fun popLevel() {
        val toRemoveSets = tower.last()
        tower.removeLast()
        for (toRemoveSet in toRemoveSets) {
            for (toRemove in toRemoveSet) {
                if (tower.all { it.all { !it.contains(toRemove) } }) {
                    set.remove(toRemove)
                }
            }
        }
    }

    fun pushSnapshot() {
        tower.last().add(HashSet())
    }

    fun popSnapshot() {
        val toRemoveSet = tower.last().last()
        tower.last().removeLast()
        for (toRemove in toRemoveSet) {
            if (tower.all { it.all { !it.contains(toRemove) } }) {
                set.remove(toRemove)
            }
        }
    }

    fun add(id: Identifier) {
        tower.last().last().add(id)
    }

    fun containsInCurrentLevel(id: Identifier): Boolean {
        return tower.last().any { it.contains(id) }
    }

    fun asSet(): Set<Identifier> = set
}

fun Scope.addFrom(pat: Pattern): Unit = when (pat) {
    is ArrayPat -> pat.elements.forEach { addFrom(it) }
    is BoolPat -> {}
    is CharPat -> {}
    is ConsListPat -> pat.patterns.forEach { addFrom(it) }
    is IntLiteralPat -> {}
    is KindPat -> {}
    is ListPat -> pat.elements.forEach { addFrom(it) }
    is NamedPat -> {
        add(pat.id)
        addFrom(pat.pat)
    }

    is SExprPat -> pat.elements.forEach { addFrom(it) }
    is ScopedPat -> addFrom(pat.pat)
    is StringLiteralPat -> {}
    is VariablePat -> add(pat.id)
    WildcardPat -> {}
}

fun Scope.addFrom(def: Definition): Unit = when (def) {
    is FunctionDefinition -> add(def.id)
    is VariableDefinition -> def.defs.forEach { add(it.first) }
}

@OptIn(ExperimentalContracts::class)
inline fun <T> Scope.withLevel(block: () -> T): T {
    contract { callsInPlace(block, kotlin.contracts.InvocationKind.EXACTLY_ONCE) }
    pushLevel()
    val result = block()
    popLevel()
    return result
}

@OptIn(ExperimentalContracts::class)
inline fun <T> Scope.withSnapshot(block: () -> T): T {
    contract { callsInPlace(block, kotlin.contracts.InvocationKind.EXACTLY_ONCE) }
    pushSnapshot()
    val result = block()
    popSnapshot()
    return result
}
