package lama.fuzzer.mutaturs

import lama.fuzzer.ast.ArrayExpr
import lama.fuzzer.ast.ArrayIndexExpr
import lama.fuzzer.ast.ArrayPat
import lama.fuzzer.ast.BasicExpr
import lama.fuzzer.ast.BinOp
import lama.fuzzer.ast.BinaryExpr
import lama.fuzzer.ast.BoolLiteralExpr
import lama.fuzzer.ast.BoolPat
import lama.fuzzer.ast.CaseExpr
import lama.fuzzer.ast.CharLiteralExpr
import lama.fuzzer.ast.CharPat
import lama.fuzzer.ast.ConsListPat
import lama.fuzzer.ast.Definition
import lama.fuzzer.ast.DoWhileExpr
import lama.fuzzer.ast.Expr
import lama.fuzzer.ast.ForExpr
import lama.fuzzer.ast.FunApplicationExpr
import lama.fuzzer.ast.FunctionDefinition
import lama.fuzzer.ast.Identifier
import lama.fuzzer.ast.IdentifierExpr
import lama.fuzzer.ast.IfExpr
import lama.fuzzer.ast.InfixExpr
import lama.fuzzer.ast.IntLiteralExpr
import lama.fuzzer.ast.IntLiteralPat
import lama.fuzzer.ast.Kind
import lama.fuzzer.ast.KindPat
import lama.fuzzer.ast.LambdaExpr
import lama.fuzzer.ast.ListExpr
import lama.fuzzer.ast.ListPat
import lama.fuzzer.ast.NamedPat
import lama.fuzzer.ast.Pattern
import lama.fuzzer.ast.SConstructor
import lama.fuzzer.ast.SExpr
import lama.fuzzer.ast.SExprPat
import lama.fuzzer.ast.Scope
import lama.fuzzer.ast.ScopeExpr
import lama.fuzzer.ast.ScopedExpr
import lama.fuzzer.ast.ScopedPat
import lama.fuzzer.ast.SeqExpr
import lama.fuzzer.ast.SkipExpr
import lama.fuzzer.ast.StringLiteralExpr
import lama.fuzzer.ast.StringLiteralPat
import lama.fuzzer.ast.VariableDefinition
import lama.fuzzer.ast.VariablePat
import lama.fuzzer.ast.WhileDoExpr
import lama.fuzzer.ast.WildcardPat
import lama.fuzzer.ast.addFrom
import lama.fuzzer.ast.withLevel
import lama.fuzzer.ast.withSnapshot
import lama.fuzzer.mutaturs.Expectation.*
import kotlin.collections.plus
import kotlin.random.Random

enum class Expectation {
    LValue, Value, NoExpect
}

private val exprConstructors = listOf<(Int, Scope, Expectation) -> Expr?>(
   { depth, scope, expectation ->
    if (scope.asSet().isEmpty()) null else IdentifierExpr(scope.asSet().random())
}, { depth, scope, expectation ->
    SeqExpr(
        left = generateExpr(depth - 1, scope, NoExpect),
        right = generateExpr(depth - 1, scope, expectation)
    )
}, { depth, scope, expectation ->
    if (expectation == LValue) return@listOf null
    val op = randomBinOp()
    val left = if (op == BinOp.ASSIGN) {
        generateExpr(depth - 1, scope, LValue)
    } else {
        generateExpr(depth - 1, scope, Value)
    }
    val right = generateExpr(depth - 1, scope, Value)
    BinaryExpr(left, op, right)
}, { depth, scope, expectation ->
    if (expectation == LValue) return@listOf null
    if (scope.asSet().isEmpty()) return@listOf null
    FunApplicationExpr(
        funExpr = IdentifierExpr(id = scope.asSet().random()),
        arguments = generateExprList(depth - 1, scope)
    )
}, { depth, scope, expectation ->
    ArrayIndexExpr(
        arrayExpr = generateExpr(depth - 1, scope, Value),
        indexExpr = generateExpr(depth - 1, scope, Value)
    )
}, { depth, scope, expectation ->
    if (expectation == LValue) return@listOf null
    IntLiteralExpr(value = allowedIntRange.random())
}, { depth, scope, expectation ->
    if (expectation == LValue) return@listOf null
    StringLiteralExpr(value = randomString())
}, { depth, scope, expectation ->
    if (expectation == LValue) return@listOf null
    CharLiteralExpr(value = randomPrintableChar())
}, { depth, scope, expectation ->
    if (expectation == LValue) return@listOf null
    BoolLiteralExpr(value = Random.nextBoolean())
}, { depth, scope, expectation ->
    if (expectation == LValue) return@listOf null
    InfixExpr(op = randomInfixableBinop())
}, { depth, scope, expectation ->
    if (expectation == LValue) return@listOf null
    val args = generateListOfPatterns(depth - 1, scope)
    val body = scope.withLevel {
        args.forEach { scope.addFrom(it) }
        generateExpr(depth - 1, scope, NoExpect)
    }
    LambdaExpr(arguments = args, body = body)
}, { depth, scope, expectation ->
    ScopedExpr(expr = generateScopeExpr(depth - 1, scope, expectation))
}, { depth, scope, expectation ->
    if (expectation == LValue) return@listOf null
    ListExpr(elements = generateExprList(depth - 1, scope))
}, { depth, scope, expectation ->
    if (expectation == LValue) return@listOf null
    ArrayExpr(elements = generateExprList(depth - 1, scope))
}, { depth, scope, expectation ->
    if (expectation == LValue) return@listOf null
    SExpr(
        constr = randomSConstr(),
        elements = generateExprList(depth - 1, scope)
    )
}, { depth, scope, expectation ->
    IfExpr(
        branches = listOf(
            Pair(
                generateExpr(depth - 1, scope, Value), generateScopeExpr(depth - 1, scope, expectation)
            )
        ), elseBranch = generateScopeExpr(depth - 1, scope, expectation)
    )
}, { depth, scope, expectation ->
    if (expectation != NoExpect) return@listOf null
    WhileDoExpr(
        cond = generateExpr(depth - 1, scope, Value), expr = generateScopeExpr(depth - 1, scope, NoExpect)
    )
}, { depth, scope, expectation ->
    if (expectation != NoExpect) return@listOf null
    DoWhileExpr(
        expr = generateScopeExpr(depth - 1, scope, NoExpect), cond = generateExpr(depth - 1, scope, Value)
    )
}, { depth, scope, expectation ->
    if (expectation != NoExpect) return@listOf null
    ForExpr(
        initExpr = generateScopeExpr(depth - 1, scope, NoExpect),
        cond = generateExpr(depth - 1, scope, Value),
        iterExpr = generateExpr(depth - 1, scope, NoExpect),
        body = generateScopeExpr(depth - 1, scope, NoExpect)
    )
}, { depth, scope, expectation ->
    val scrutinee = generateExpr(depth - 1, scope, Value)
    val branches = List(Random.nextInt(1, 5)) {
        val pat = generatePattern(depth - 1, scope)
        val body = scope.withLevel {
            scope.addFrom(pat)
            generateScopeExpr(depth - 1, scope, expectation)
        }
        Pair(pat, body)
    }
    CaseExpr(scrutinee, branches)
})

fun generateExpr(depth: Int, scope: Scope, expectation: Expectation): Expr {
    if (depth <= 0) return when (expectation) {
        Value -> IntLiteralExpr(0)
        NoExpect -> SkipExpr
        LValue -> ArrayIndexExpr(IntLiteralExpr(0), IntLiteralExpr(0))
    }
    repeat(100) {
        exprConstructors.random()(depth, scope, expectation)?.let { return it }
    }
    error { "Too many attempts to generate an expression. Looks suspicious." }
}

fun generateExprList(depth: Int, scope: Scope): List<Expr> {
    if (depth <= 0) return emptyList()
    val listSize = Random.nextInt(0, 10)
    return List(listSize) { generateExpr(depth - 1, scope, Value) }
}

fun generateListOfPatterns(depth: Int, scope: Scope, minSize: Int = 0): List<Pattern> {
    if (depth <= 0) return listOf(WildcardPat)
    val listSize = Random.nextInt(minSize, 10)
    return scope.withLevel {
        List(listSize) {
            generatePattern(depth - 1, scope).also { scope.addFrom(it) }
        }
    }
}

private val patternConstructors = listOf<(Int, Scope) -> Pattern>({ depth, scope ->
    val patterns = generateListOfPatterns(depth - 1, scope, 2)
    ConsListPat(patterns)
}, { depth, scope ->
    WildcardPat
}, { depth, scope ->
    val elements = generateListOfPatterns(depth - 1, scope)
    SExprPat(constructor = randomSConstr(), elements = elements)
}, { depth, scope ->
    val elements = generateListOfPatterns(depth - 1, scope)
    ArrayPat(elements)
}, { depth, scope ->
    val elements = generateListOfPatterns(depth - 1, scope)
    ListPat(elements)
}, { depth, scope ->
    val id = randomNewIdentifier(scope)
    val pat = generatePattern(depth - 1, scope)
    NamedPat(id = id, pat = pat)
}, { depth, scope ->
    val id = randomNewIdentifier(scope)
    VariablePat(id = id)
}, { depth, scope ->
    IntLiteralPat(value = allowedIntRange.random())
}, { depth, scope ->
    StringLiteralPat(value = randomString())
}, { depth, scope ->
    CharPat(value = randomPrintableChar())
}, { depth, scope ->
    BoolPat(value = Random.nextBoolean())
}, { depth, scope ->
    KindPat(kind = randomKind())
}, { depth, scope ->
    val pat = generatePattern(depth - 1, scope)
    ScopedPat(pat = pat)
})

fun generatePattern(depth: Int, scope: Scope): Pattern {
    if (depth <= 0) return WildcardPat
    return patternConstructors.random()(depth, scope)
}

fun generateScopeExpr(depth: Int, scope: Scope, expectation: Expectation): ScopeExpr {
    if (depth <= 0) return ScopeExpr(definitions = emptyList(), expr = generateExpr(depth, scope, expectation))
    val definitions = generateDefinitionList(depth - 1, scope)
    val expr = scope.withLevel {
        definitions.forEach { scope.addFrom(it) }
        generateExpr(depth - 1, scope, expectation)
    }
    return ScopeExpr(definitions, expr)
}

fun generateDefinitionList(depth: Int, scope: Scope): List<Definition> {
    if (depth <= 0) return emptyList()
    val listSize = Random.nextInt(0, 10)
    return scope.withLevel {
        List(listSize) {
            generateDefinition(depth - 1, scope).also { scope.addFrom(it) }
        }
    }
}

val definitionConstructors: List<(Int, Scope) -> Definition> = listOf({ depth, scope ->
    val defs = generateVariableDefs(depth - 1, scope)
    VariableDefinition(defs)
}, { depth, scope ->
    val id = randomNewIdentifier(scope)
    val args = generateListOfPatterns(depth - 1, scope)
    val body = scope.withLevel {
        scope.add(id)
        args.forEach { scope.addFrom(it) }
        generateExpr(depth - 1, scope, NoExpect)
    }
    FunctionDefinition(id, args, body)
})

fun generateDefinition(depth: Int, scope: Scope): Definition {
    if (depth <= 0) {
        val id = randomNewIdentifier(scope)
        return FunctionDefinition(id, emptyList(), SkipExpr)
    }
    return definitionConstructors.random()(depth, scope)
}

fun generateVariableDefs(
    depth: Int, scope: Scope,
): List<Pair<Identifier, BasicExpr?>> {
    val listSize = Random.nextInt(1, 10)
    return scope.withSnapshot {
        List(listSize) {
            val id = randomNewIdentifier(scope)
            val expr = if (Random.nextBoolean()) {
                generateBasicExpr(depth - 1, scope, Value)
            } else null
            scope.add(id)
            Pair(id, expr)
        }
    }
}

fun generateBasicExpr(depth: Int, scope: Scope, expectation: Expectation): BasicExpr {
    val expr = generateExpr(depth, scope, expectation)
    return if (expr is BasicExpr) expr else ScopedExpr(ScopeExpr(emptyList(), expr))
}

fun randomBinOp() = BinOp.entries.random()

fun randomInfixableBinop() = BinOp.entries.filter { it != BinOp.ASSIGN }.random()

fun randomKind() = Kind.entries.random()

fun randomIdentifier(): Identifier {
    val length = Random.nextInt(1, 6)
    val firstChar = ('a'..'z').random()
    val otherChars = (1 until length).map {
        (('a'..'z') + ('A'..'Z') + ('0'..'9') + '_').random()
    }
    return firstChar + otherChars.joinToString("")
}

private val keywords = setOf(
    "import",
    "var",
    "public",
    "external",
    "fun",
    "infix",
    "infixl",
    "infixr",
    "if",
    "then",
    "elif",
    "else",
    "fi",
    "skip",
    "while",
    "do",
    "od",
    "for",
    "let",
    "in",
    "case",
    "of",
    "esac",
    "lazy",
    "eta",
    "syntax",
    "before",
    "after",
    "at",
    "true",
    "false",
    "box"
)

fun randomNewIdentifier(scope: Scope): Identifier {
    repeat(100) {
        val id = randomIdentifier()
        if (!scope.containsInCurrentLevel(id) && id !in keywords) {
            return id
        }
    }
    error("Too many attempts to generate a new identifier. Looks suspicious.")
}

fun randomSConstr(): SConstructor {
    val length = Random.nextInt(1, 6)
    val firstChar = ('A'..'Z').random()
    val otherChars = (1 until length).map {
        (('a'..'z') + ('A'..'Z') + ('0'..'9') + '_').random()
    }
    return firstChar + otherChars.joinToString("")
}

fun randomString(): String {
    val length = Random.nextInt(0, 10)
    val chars = List(length) { randomPrintableChar() }
    return chars.joinToString("")
}

val printableChars = (' '..'~')
fun randomPrintableChar() = printableChars.random()

val allowedIntRange = Long.MIN_VALUE / 2 until Long.MAX_VALUE / 2
