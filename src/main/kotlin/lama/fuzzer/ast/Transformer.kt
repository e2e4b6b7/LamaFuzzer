package lama.fuzzer.ast

import lama.fuzzer.ast.Cmd.Continue
import lama.fuzzer.ast.Cmd.Replace
import lama.fuzzer.mutaturs.Expectation
import lama.fuzzer.mutaturs.Expectation.*

abstract class Transformer {
    abstract fun transformScopeExpr(scope: Scope, scopeExpr: ScopeExpr, expectation: Expectation): Cmd<ScopeExpr>
    abstract fun transformExpr(scope: Scope, expr: Expr, expectation: Expectation): Cmd<Expr>
    abstract fun transformBasicExpr(scope: Scope, expr: BasicExpr, expectation: Expectation): Cmd<BasicExpr>
    abstract fun transformDefinition(scope: Scope, definition: Definition): Cmd<Definition>
    abstract fun transformPattern(scope: Scope, pattern: Pattern): Cmd<Pattern>
}

sealed class Cmd<out T> {
    /**
     * Replace the current value with a new one.
     * @param newValue The new value.
     */
    class Replace<T>(val newValue: T) : Cmd<T>()

    /**
     * Continue the transformation.
     */
    object Continue : Cmd<Nothing>()
}

fun Transformer.transform(scope: Scope, scopeExpr: ScopeExpr, expectation: Expectation): ScopeExpr =
    when (val cmd = transformScopeExpr(scope, scopeExpr, expectation)) {
        is Replace -> cmd.newValue
        is Continue -> scope.withLevel {
            val newDefinitions = scopeExpr.definitions.map { definition ->
                transform(scope, definition).also { scope.addFrom(it) }
            }
            val newExpr = transform(scope, scopeExpr.expr, expectation)
            ScopeExpr(newDefinitions, newExpr)
        }
    }

fun Transformer.transform(scope: Scope, definition: Definition): Definition {
    return when (val cmd = transformDefinition(scope, definition)) {
        is Replace -> cmd.newValue
        is Continue -> when (definition) {
            is FunctionDefinition -> scope.withLevel {
                val newArgs = definition.args.map { transform(scope, it).also { scope.addFrom(it) } }
                val newBody = transform(scope, definition.body, NoExpect)
                FunctionDefinition(definition.id, newArgs, newBody)
            }

            is VariableDefinition -> scope.withSnapshot {
                val newDefs = definition.defs.map { (id, expr) ->
                    val newExpr = expr?.let { transform(scope, it, Value) }
                    scope.add(id)
                    id to newExpr
                }
                VariableDefinition(newDefs)
            }
        }
    }
}

fun Transformer.transform(scope: Scope, expr: Expr, expectation: Expectation): Expr {
    return when (expr) {
        is SeqExpr -> when (val cmd = transformExpr(scope, expr, expectation)) {
            is Replace -> cmd.newValue
            is Continue -> {
                val newLeft = transform(scope, expr.left, NoExpect)
                val newRight = transform(scope, expr.right, expectation)
                SeqExpr(newLeft, newRight)
            }
        }

        is BasicExpr -> transform(scope, expr, expectation)
    }
}


fun Transformer.transform(scope: Scope, pat: Pattern): Pattern {
    return when (val cmd = transformPattern(scope, pat)) {
        is Replace -> cmd.newValue
        is Continue -> when (pat) {
            is ArrayPat -> scope.withSnapshot {
                val newElements = pat.elements.map { transform(scope, it).also { scope.addFrom(it) } }
                ArrayPat(newElements)
            }

            is BoolPat -> pat
            is CharPat -> pat
            is ConsListPat -> scope.withSnapshot {
                val newPatterns = pat.patterns.map { transform(scope, it).also { scope.addFrom(it) } }
                ConsListPat(newPatterns)
            }

            is IntLiteralPat -> pat
            is KindPat -> pat
            is ListPat -> scope.withSnapshot {
                val newElements = pat.elements.map { transform(scope, it).also { scope.addFrom(it) } }
                ListPat(newElements)
            }

            is NamedPat -> {
                val newPattern = transform(scope, pat.pat)
                NamedPat(pat.id, newPattern)
            }

            is SExprPat -> scope.withSnapshot {
                val newElements = pat.elements.map { transform(scope, it).also { scope.addFrom(it) } }
                SExprPat(pat.constructor, newElements)
            }

            is ScopedPat -> {
                val newPattern = transform(scope, pat.pat)
                ScopedPat(newPattern)
            }

            is StringLiteralPat -> pat
            is VariablePat -> pat
            WildcardPat -> pat
        }
    }
}

fun Transformer.transform(scope: Scope, expr: BasicExpr, expectation: Expectation): BasicExpr {
    return when (val cmd = transformBasicExpr(scope, expr, expectation)) {
        is Replace -> cmd.newValue
        is Continue -> when (expr) {
            is ArrayExpr -> {
                val newElements = expr.elements.map { transform(scope, it, Value) }
                ArrayExpr(newElements)
            }

            is ArrayIndexExpr -> {
                val newArrayExpr = transform(scope, expr.arrayExpr, Value)
                val newIndexExpr = transform(scope, expr.indexExpr, Value)
                ArrayIndexExpr(newArrayExpr, newIndexExpr)
            }

            is BinaryExpr -> {
                val leftExpectation = if (expr.op == BinOp.ASSIGN) LValue else Value
                val newLeft = transform(scope, expr.left, leftExpectation)
                val newRight = transform(scope, expr.right, Value)
                BinaryExpr(newLeft, expr.op, newRight)
            }

            is BoolLiteralExpr -> expr

            is CaseExpr -> {
                val newScrut = transform(scope, expr.scrut, Value)
                val newBranches = expr.branches.map { (pattern, scopeExpr) ->
                    scope.withLevel {
                        val newPattern = transform(scope, pattern).also { scope.addFrom(it) }
                        val newScopeExpr = transform(scope, scopeExpr, expectation)
                        newPattern to newScopeExpr
                    }
                }
                CaseExpr(newScrut, newBranches)
            }

            is CharLiteralExpr -> expr
            is DoWhileExpr -> {
                val newExpr = transform(scope, expr.expr, NoExpect)
                val newCond = transform(scope, expr.cond, Value)
                DoWhileExpr(newExpr, newCond)
            }

            is ForExpr -> scope.withLevel {
                val newInitExpr = transform(scope, expr.initExpr, NoExpect)
                    .also { it.definitions.forEach { scope.addFrom(it) } }
                val newCond = transform(scope, expr.cond, Value)
                val newIterExpr = transform(scope, expr.iterExpr, NoExpect)
                val newBody = transform(scope, expr.body, NoExpect)
                ForExpr(newInitExpr, newCond, newIterExpr, newBody)
            }

            is FunApplicationExpr -> {
                val newFunExpr = transform(scope, expr.funExpr, Value)
                val newArguments = expr.arguments.map { transform(scope, it, Value) }
                FunApplicationExpr(newFunExpr, newArguments)
            }

            is IdentifierExpr -> expr
            is IfExpr -> {
                val newBranches = expr.branches.map { (cond, branchExpr) ->
                    val newCond = transform(scope, cond, Value)
                    val newBranchExpr = transform(scope, branchExpr, expectation)
                    newCond to newBranchExpr
                }
                val newElseBranch = expr.elseBranch?.let { transform(scope, it, expectation) }
                IfExpr(newBranches, newElseBranch)
            }

            is InfixExpr -> expr
            is IntLiteralExpr -> expr
            is LambdaExpr -> scope.withLevel {
                val newArgs = expr.arguments.map { transform(scope, it).also { scope.addFrom(it) } }
                val newBody = transform(scope, expr.body, NoExpect)
                LambdaExpr(newArgs, newBody)
            }

            is ListExpr -> {
                val newElements = expr.elements.map { transform(scope, it, Value) }
                ListExpr(newElements)
            }

            is SExpr -> {
                val newElements = expr.elements.map { transform(scope, it, Value) }
                SExpr(expr.constr, newElements)
            }

            is ScopedExpr -> {
                val newExpr = transform(scope, expr.expr, expectation)
                ScopedExpr(newExpr)
            }

            SkipExpr -> expr
            is StringLiteralExpr -> expr
            is WhileDoExpr -> {
                val newCond = transform(scope, expr.cond, Value)
                val newExpr = transform(scope, expr.expr, NoExpect)
                WhileDoExpr(newCond, newExpr)
            }
        }
    }
}
