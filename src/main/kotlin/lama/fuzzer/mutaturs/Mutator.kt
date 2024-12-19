package lama.fuzzer.mutaturs

import lama.fuzzer.ast.ScopeExpr

interface Mutator {
    fun mutate(input: ScopeExpr): ScopeExpr
}
