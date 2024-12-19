package lama.fuzzer.ast

enum class BinOp {
    ASSIGN,
    LIST_CONS,
    DISJUNCTION,
    CONJUNCTION,
    CMP_EQ, CMP_NEQ, CMP_LE, CMP_LEQ, CMP_GE, CMP_GEQ,
    PLUS, MINUS,
    MULTIPLY, DIVIDE, REMAINDER
}

enum class Kind {
    VAL,

    BOX, // BOX == STR | ARRAY | SEXP | FUN
    STR,
    ARRAY,
    SEXP,
    FUN
}

/**
 * [a-z][a-zA-Z_0-9]*
 */
typealias Identifier = String

/**
 * [A-Z][a-zA-Z_0-9]*
 */
typealias SConstructor = String

sealed interface Definition

data class VariableDefinition(val defs: List<Pair<Identifier, BasicExpr?>>) : Definition
data class FunctionDefinition(val id: Identifier, val args: List<Pattern>, val body: Expr) : Definition

data class ScopeExpr(val definitions: List<Definition>, val expr: Expr)

sealed interface Expr
sealed interface BasicExpr : Expr

data class SeqExpr(val left: Expr, val right: Expr) : Expr
data class BinaryExpr(val left: Expr, val op: BinOp, val right: Expr) : BasicExpr
data class FunApplicationExpr(val funExpr: Expr, val arguments: List<Expr>) : BasicExpr
data class ArrayIndexExpr(val arrayExpr: Expr, val indexExpr: Expr) : BasicExpr
data class IntLiteralExpr(val value: Long) : BasicExpr
data class StringLiteralExpr(val value: String) : BasicExpr
data class CharLiteralExpr(val value: Char) : BasicExpr
data class BoolLiteralExpr(val value: Boolean) : BasicExpr
data class IdentifierExpr(val id: Identifier) : BasicExpr
data class InfixExpr(val op: BinOp) : BasicExpr // "infix ${op}"
data class LambdaExpr(val arguments: List<Pattern>, val body: Expr) : BasicExpr
data object SkipExpr : BasicExpr
data class ScopedExpr(val expr: ScopeExpr) : BasicExpr
data class ListExpr(val elements: List<Expr>) : BasicExpr
data class ArrayExpr(val elements: List<Expr>) : BasicExpr
data class SExpr(val constr: SConstructor, val elements: List<Expr>) : BasicExpr
data class IfExpr(val branches: List<Pair<Expr, ScopeExpr>>, val elseBranch: ScopeExpr?) : BasicExpr
data class WhileDoExpr(val cond: Expr, val expr: ScopeExpr) : BasicExpr
data class DoWhileExpr(val expr: ScopeExpr, val cond: Expr) : BasicExpr
data class ForExpr(val initExpr: ScopeExpr, val cond: Expr, val iterExpr: Expr, val body: ScopeExpr) : BasicExpr
data class CaseExpr(val scrut: Expr, val branches: List<Pair<Pattern, ScopeExpr>>) : BasicExpr

sealed interface Pattern

data class ConsListPat(val patterns: List<Pattern>) : Pattern
data object WildcardPat : Pattern
data class SExprPat(val constructor: SConstructor, val elements: List<Pattern>) : Pattern
data class ArrayPat(val elements: List<Pattern>) : Pattern
data class ListPat(val elements: List<Pattern>) : Pattern
data class VariablePat(val id: Identifier) : Pattern
data class NamedPat(val id: Identifier, val pat: Pattern) : Pattern
data class IntLiteralPat(val value: Long) : Pattern
data class StringLiteralPat(val value: String) : Pattern
data class CharPat(val value: Char) : Pattern
data class BoolPat(val value: Boolean) : Pattern
data class KindPat(val kind: Kind) : Pattern
data class ScopedPat(val pat: Pattern) : Pattern
