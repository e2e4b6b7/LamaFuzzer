package lama.fuzzer.ast

import lama.fuzzer.ast.Cmd.Continue
import lama.fuzzer.mutaturs.Expectation
import lama.fuzzer.mutaturs.Expectation.NoExpect

/**
 * Calculate the number of mutation points in the given AST.
 */
fun ScopeExpr.mutationPointsNumber(): Int {
    val transformer = CountingTransformer()
    transformer.transform(Scope(), this, NoExpect)
    return transformer.count
}

private class CountingTransformer : Transformer() {
    var count = 0
    override fun transformScopeExpr(
        scope: Scope,
        scopeExpr: ScopeExpr,
        expectation: Expectation,
    ): Cmd<ScopeExpr> {
        count++
        return Continue
    }

    override fun transformExpr(
        scope: Scope,
        expr: Expr,
        expectation: Expectation,
    ): Cmd<Expr> {
        count++
        return Continue
    }

    override fun transformBasicExpr(
        scope: Scope,
        expr: BasicExpr,
        expectation: Expectation,
    ): Cmd<BasicExpr> {
        count++
        return Continue
    }

    override fun transformDefinition(
        scope: Scope,
        definition: Definition,
    ): Cmd<Definition> {
        count++
        return Continue
    }

    override fun transformPattern(
        scope: Scope,
        pattern: Pattern,
    ): Cmd<Pattern> {
        count++
        return Continue
    }
}
