package lama.fuzzer.mutaturs

import lama.fuzzer.ast.*
import lama.fuzzer.ast.Cmd.Continue
import lama.fuzzer.ast.Cmd.Replace
import lama.fuzzer.mutaturs.Expectation.NoExpect
import kotlin.random.Random

class SimpleMutator : Mutator {
    override fun mutate(input: ScopeExpr): ScopeExpr {
        val toMutateIdx = Random.nextInt(input.mutationPointsNumber())
        val transformer = SimpleMutationTransformer(toMutateIdx)
        return transformer.transform(Scope(), input, NoExpect)
    }
}

private class SimpleMutationTransformer(val mutationIndex: Int) : Transformer() {
    var currentIndex = 0

    override fun transformScopeExpr(
        scope: Scope,
        scopeExpr: ScopeExpr,
        expectation: Expectation,
    ): Cmd<ScopeExpr> {
        if (currentIndex == mutationIndex) {
            return Replace(generateScopeExpr(3, scope, expectation))
        }
        currentIndex++
        return Continue
    }

    override fun transformExpr(
        scope: Scope,
        expr: Expr,
        expectation: Expectation,
    ): Cmd<Expr> {
        if (currentIndex == mutationIndex) {
            return Replace(generateExpr(3, scope, expectation))
        }
        currentIndex++
        return Continue
    }

    override fun transformBasicExpr(
        scope: Scope,
        expr: BasicExpr,
        expectation: Expectation,
    ): Cmd<BasicExpr> {
        if (currentIndex == mutationIndex) {
            return Replace(generateBasicExpr(3, scope, expectation))
        }
        currentIndex++
        return Continue
    }

    override fun transformDefinition(
        scope: Scope,
        definition: Definition,
    ): Cmd<Definition> {
        if (currentIndex == mutationIndex) {
            return Replace(generateDefinition(3, scope))
        }
        currentIndex++
        return Continue
    }

    override fun transformPattern(
        scope: Scope,
        pattern: Pattern,
    ): Cmd<Pattern> {
        if (currentIndex == mutationIndex) {
            return Replace(generatePattern(3, scope))
        }
        currentIndex++
        return Continue
    }
}
