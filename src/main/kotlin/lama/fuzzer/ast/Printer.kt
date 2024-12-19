package lama.fuzzer.ast

fun printExpr(expr: Expr): String {
    return when (expr) {
        is SeqExpr -> {
            val leftStr = printExpr(expr.left)
            val rightStr = printExpr(expr.right)
            "($leftStr; $rightStr)"
        }

        is BinaryExpr -> {
            val leftStr = printExpr(expr.left)
            val rightStr = printExpr(expr.right)
            val opStr = expr.op.str
            "($leftStr $opStr $rightStr)"
        }

        is FunApplicationExpr -> {
            val funStr = printExpr(expr.funExpr)
            val argsStr = expr.arguments.joinToString(", ") { printExpr(it) }
            "$funStr ($argsStr)"
        }

        is ArrayIndexExpr -> {
            val arrayStr = printExpr(expr.arrayExpr)
            val indexStr = printExpr(expr.indexExpr)
            "($arrayStr)[$indexStr]"
        }

        is IntLiteralExpr -> expr.value.toString()
        is StringLiteralExpr -> "\"${expr.value.replace("\"", "\"\"")}\""
        is CharLiteralExpr -> if (expr.value == '\'') "''''" else "'${expr.value}'"
        is BoolLiteralExpr -> expr.value.str
        is IdentifierExpr -> expr.id
        is InfixExpr -> "(infix ${expr.op.str})"

        is LambdaExpr -> {
            val bodyStr = printExpr(expr.body)
            val argsStr = expr.arguments.joinToString(", ") { printPattern(it) }
            "fun ($argsStr) { $bodyStr }"
        }

        SkipExpr -> "skip"

        is ScopedExpr -> {
            val scopeStr = printScopeExpr(expr.expr)
            "($scopeStr)"
        }

        is ListExpr -> {
            val elementsStr = expr.elements.joinToString(", ") { printExpr(it) }
            "{ $elementsStr }"
        }

        is ArrayExpr -> {
            val elementsStr = expr.elements.joinToString(", ") { printExpr(it) }
            "[ $elementsStr ]"
        }

        is SExpr -> {
            if (expr.elements.isEmpty()) {
                expr.constr
            } else {
                val elementsStr = expr.elements.joinToString(", ") { printExpr(it) }
                "${expr.constr} ($elementsStr)"
            }
        }

        is IfExpr -> buildString {
            with(expr.branches.first()) {
                append("if ${printExpr(first)} then ${printScopeExpr(second)}")
            }
            for ((cond, scope) in expr.branches.drop(1)) {
                append(" elif ${printExpr(cond)} then ${printScopeExpr(scope)}")
            }
            expr.elseBranch?.let {
                append(" else ${printScopeExpr(it)}")
            }
            append(" fi")
        }

        is WhileDoExpr -> {
            val condStr = printExpr(expr.cond)
            val bodyStr = printScopeExpr(expr.expr)
            "while $condStr do $bodyStr od"
        }

        is DoWhileExpr -> {
            val bodyStr = printScopeExpr(expr.expr)
            val condStr = printExpr(expr.cond)
            "do $bodyStr while $condStr od"
        }

        is ForExpr -> {
            val initStr = printScopeExpr(expr.initExpr)
            val condStr = printExpr(expr.cond)
            val iterStr = printExpr(expr.iterExpr)
            val bodyStr = printScopeExpr(expr.body)
            "for $initStr, $condStr, $iterStr do $bodyStr od"
        }

        is CaseExpr -> {
            val scrutStr = printExpr(expr.scrut)
            val branchesStr = expr.branches.joinToString(" | ") { (pat, scope) ->
                "${printPattern(pat)} -> ${printScopeExpr(scope)}"
            }
            "case $scrutStr of $branchesStr esac"
        }
    }
}

private val BinOp.str
    get() = when (this) {
        BinOp.ASSIGN -> ":="
        BinOp.LIST_CONS -> ":"
        BinOp.DISJUNCTION -> "!!"
        BinOp.CONJUNCTION -> "&&"
        BinOp.CMP_EQ -> "=="
        BinOp.CMP_NEQ -> "!="
        BinOp.CMP_LE -> "<"
        BinOp.CMP_LEQ -> "<="
        BinOp.CMP_GE -> ">"
        BinOp.CMP_GEQ -> ">="
        BinOp.PLUS -> "+"
        BinOp.MINUS -> "-"
        BinOp.MULTIPLY -> "*"
        BinOp.DIVIDE -> "/"
        BinOp.REMAINDER -> "%"
    }

private val Boolean.str
    get() = if (this) "true" else "false"

private val Kind.str
    get() = when (this) {
        Kind.VAL -> "#val"
        Kind.BOX -> "#box"
        Kind.STR -> "#str"
        Kind.ARRAY -> "#array"
        Kind.SEXP -> "#sexp"
        Kind.FUN -> "#fun"
    }

fun printPattern(pat: Pattern): String {
    return when (pat) {
        is ConsListPat -> pat.patterns.joinToString(" : ") { printPattern(it) }
        WildcardPat -> "_"
        is SExprPat -> {
            if (pat.elements.isEmpty()) {
                pat.constructor
            } else {
                val elementsStr = pat.elements.joinToString(", ") { printPattern(it) }
                "${pat.constructor} ($elementsStr)"
            }
        }

        is ArrayPat -> {
            val elementsStr = pat.elements.joinToString(", ") { printPattern(it) }
            "[ $elementsStr ]"
        }

        is ListPat -> {
            val elementsStr = pat.elements.joinToString(", ") { printPattern(it) }
            "{ $elementsStr }"
        }

        is NamedPat -> "${pat.id}@${printPattern(pat.pat)}"
        is VariablePat -> pat.id
        is IntLiteralPat -> pat.value.toString()
        is StringLiteralPat -> "\"${pat.value.replace("\"", "\"\"")}\""
        is CharPat -> if (pat.value == '\'') "''''" else "'${pat.value}'"
        is BoolPat -> pat.value.str
        is KindPat -> pat.kind.str
        is ScopedPat -> "(${printPattern(pat.pat)})"
    }
}

fun printDefinition(def: Definition): String {
    return when (def) {
        is VariableDefinition -> {
            val defsStr = def.defs.joinToString(", ") { (id, expr) ->
                val exprStr = expr?.let { " = ${printExpr(it)}" } ?: ""
                "$id$exprStr"
            }
            "var $defsStr;"
        }

        is FunctionDefinition -> {
            val argsStr = def.args.joinToString(", ") { printPattern(it) }
            val bodyStr = printExpr(def.body)
            "fun ${def.id} ($argsStr) { $bodyStr }"
        }
    }
}

fun printScopeExpr(scope: ScopeExpr): String {
    val defsStr = scope.definitions.joinToString(" ") { printDefinition(it) }
    val exprStr = printExpr(scope.expr)
    return "$defsStr $exprStr"
}
