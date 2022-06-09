package piacenti.typescript2kotlin

import org.antlr.v4.kotlinruntime.*
import org.antlr.v4.kotlinruntime.tree.ParseTreeWalker
import piacenti.dslmaker.dsl.InsertionType
import piacenti.dslmaker.dsl.antlr.FastTokenStreamRewriter
import piacenti.dslmaker.dsl.antlr.StreamReWriter
import piacenti.dslmaker.forEachRecursive
import piacenti.typescript2kotlin.antlr.generated.TypeDefinitionsBaseListener
import piacenti.typescript2kotlin.antlr.generated.TypeDefinitionsLexer
import piacenti.typescript2kotlin.antlr.generated.TypeDefinitionsParser

data class TranslationResult(val code: String, val fileName: String)
class Translator {
    class TypeDefinitionToKotlin(private val tokenStream: CommonTokenStream, private val moduleName: String) :
        TypeDefinitionsBaseListener(), StreamReWriter {
        override val rewriter = FastTokenStreamRewriter(tokenStream)
        private val genericTypes = mutableListOf<String>()
        private val typeAliases = mutableMapOf<String,ParserRuleContext>()
        private val allTypes = mutableMapOf<String, ParserRuleContext>()
        private var additionalFile: String = ""
        val result: List<TranslationResult>
            get() {
                return buildList {
                    val fileName = moduleName.normalizedModuleName() + "External"
                    add(TranslationResult(rewriter.text.replace("(?m)^\\s*?\\n\\s*?\\n".toRegex(), ""), "$fileName.kt"))
                    if (additionalFile.isNotBlank()) {
                        add(
                            TranslationResult(
                                additionalFile,
                                fileName + "Support.kt"
                            )
                        )
                    }
                }
            }

        private fun String.normalizedModuleName() =
            this.replace("@", "").replace("[/-](\\w)".toRegex()) { it.groupValues[1].uppercase() }

        companion object {
            val PRIMITIVES = mapOf(
                "string" to "String",
                "number" to "Number",
                "boolean" to "Boolean",
                "any" to "Any",
                "void" to "Unit"
            )
            val NULL_TYPES = setOf("null", "undefined")
            val KOTLIN_KEYWORDS = setOf("data", "false", "true")
        }

        override fun enterMain(ctx: TypeDefinitionsParser.MainContext) {
            ctx.forEachRecursive { it ->
                if (it is TypeDefinitionsParser.TypeAliasContext) {
                    val name = it.typeDefinition?.findTypeName()?.text
                    if (name != null) {
                        typeAliases[name] = it
                    }
                }
                if (it is TypeDefinitionsParser.RestrictedValuesContext) {
                    val name = it.findIdentifier()?.text
                    if (name != null) {
                        it.findIdentifier()?.let{identifierContext ->
                            typeAliases[name] = identifierContext
                        }
                    }
                }
                if (it is TypeDefinitionsParser.NamedTypeContext) {
                    if (it.findGeneric() != null) {
                        genericTypes.add(it.findTypeName()?.text ?: error("Type name must be present"))
                    }
                    it.findTypeName()?.text?.let { name ->
                        allTypes[name] = it
                    }
                }
                true
            }
        }

        override fun exitMain(ctx: TypeDefinitionsParser.MainContext) {
            rewriter.insertBefore(
                ctx.start,
                "@file:JsModule(\"$moduleName\")\n@file:Suppress(\"unused\", \"PropertyName\", \"FunctionName\")\n\n",
                InsertionType.FORMAT
            )
//            this step is better done manually if needed
//            val typesNotDefinedInFile = allTypes.keys - typeAliases.keys
//            typesNotDefinedInFile.filter { it.length > 2 }.forEach {
//                rewriter.replace(allTypes[it], "dynamic")
//            }
        }

        override fun exitImportDeclaration(ctx: TypeDefinitionsParser.ImportDeclarationContext) {
            rewriter.delete(ctx)
        }

        override fun exitTypeAlias(ctx: TypeDefinitionsParser.TypeAliasContext) {
            if (ctx.findClassContext().isNotEmpty()) {
                rewriter.replace(ctx.DECLARE(), ctx.TYPE(), "external interface")
                if (ctx.AMPERSAND().isNotEmpty()) {
                    rewriter.replace(ctx.EQUALS(), ":")
                    ctx.AMPERSAND().forEach {
                        rewriter.delete(it)
                    }
                } else {
                    rewriter.delete(ctx.EQUALS())
                }
                val interfaces = ctx.findType().joinToString(",") { it.text }
                rewriter.insertAfter(ctx.EQUALS(), interfaces, InsertionType.FORMAT)
                ctx.findType().forEach {
                    rewriter.delete(it)
                }
            } else {
                val commentToken = getPrecedingCommentToken(ctx)
                val comment = if (commentToken != null) commentToken.text + "\n" else ""
                val rightSideGeneric = ctx.findType().lastOrNull()?.findNamedType()?.findGeneric()
                val type = if (rightSideGeneric?.findGenericParameter()?.firstOrNull()?.findClassContext() != null) {
                    rewriter.replace(ctx.EQUALS(), ":")
                    rewriter.delete(rightSideGeneric.LESS_THAN())
                    rewriter.delete(rightSideGeneric.GREATER_THAN())
                    rewriter.replace(ctx.DECLARE(), ctx.TYPE(), "external interface")
                } else {
                    commentToken?.text = ""
                    rewriter.replace(ctx.DECLARE(), ctx.TYPE(), "typealias")
                    val unionType = ctx.findUnionType()
                    additionalFile += comment
                    if (unionType != null) {
                        additionalFile += "/** Any = ${unionType.rewriterText()}*/\n"
                        rewriter.replace(unionType, "Any")
                    }
                    additionalFile += ctx.rewriterText() + "\n"
                    rewriter.delete(ctx)
                }
            }
        }

        override fun exitNamedType(ctx: TypeDefinitionsParser.NamedTypeContext) {
            if (ctx.OPEN_BRACKET() != null) {
                rewriter.delete(ctx.OPEN_BRACKET())
                rewriter.delete(ctx.CLOSE_BRACKET())
                rewriter.insertBefore(ctx, "Array<", InsertionType.FORMAT)
                rewriter.insertAfter(ctx, ">", InsertionType.FORMAT)
            }
        }

        override fun exitVariableDefinition(ctx: TypeDefinitionsParser.VariableDefinitionContext) {
            rewriter.insertBefore(ctx.findIdentifier()?.start, "var ", InsertionType.FORMAT)
            if (ctx.QUESTION() != null) {
                rewriter.delete(ctx.QUESTION())
                rewriter.insertAfter(ctx.findType()?.stop, "?", InsertionType.FORMAT)
            }
        }

        override fun exitGenericParameter(ctx: TypeDefinitionsParser.GenericParameterContext) {
            val equals = ctx.EQUALS()
            if (equals != null) {
                rewriter.delete(equals, ctx)
            }
            rewriter.replace(ctx.EXTENDS(), ":")
            ctx.findType().mapNotNull { it.findNamedType() }.forEach {
                val typeName = it.findTypeName()
                if (typeName != null && genericTypes.contains(typeName.text) && it.findGeneric() == null) {
                    rewriter.replace(typeName, "${typeName.rewriterText()}<*>")
                }
            }
        }

        override fun exitUnionType(ctx: TypeDefinitionsParser.UnionTypeContext) {
            if (ctx.findType().size == 2 && ctx.findType().any { NULL_TYPES.contains(it.text) }) {
                val nonNullType = ctx.findType().find { !NULL_TYPES.contains(it.text) }
                if (nonNullType != null)
                    rewriter.replace(ctx, nonNullType.rewriterText() + "?")
            }
        }

        override fun exitIdentifier(ctx: TypeDefinitionsParser.IdentifierContext) {
            val text = ctx.text
            if (text in KOTLIN_KEYWORDS)
                rewriter.replace(ctx, "`${ctx.text}`")
            val associatedPrimitive = PRIMITIVES[text]
            if (associatedPrimitive != null) {
                rewriter.replace(ctx, associatedPrimitive)
            }
        }

        override fun exitReference(ctx: TypeDefinitionsParser.ReferenceContext) {
            val lambdaType = ctx.findType()?.findLambdaType()
            if (lambdaType != null) {
                rewriter.replace(ctx.DECLARE(), ctx.CONST(), "external fun")
                rewriter.delete(ctx.COLON())
                rewriter.delete(lambdaType.LAMBDA_OPERATOR())
                rewriter.insertAfter(lambdaType.LAMBDA_OPERATOR(), ":", InsertionType.FORMAT)
                val generic = lambdaType.findGeneric()
                if (generic != null) {
                    rewriter.insertBefore(
                        ctx.findIdentifier()?.start,
                        generic.rewriterText() + " ",
                        InsertionType.FORMAT
                    )
                    rewriter.delete(generic)
                }
            } else {
                rewriter.replace(ctx.DECLARE(), ctx.CONST(), "external val")
            }
        }

        override fun exitLambdaType(ctx: TypeDefinitionsParser.LambdaTypeContext) {
            rewriter.replace(ctx.LAMBDA_OPERATOR(), text = "->")
        }

        override fun exitFunctionDefinition(ctx: TypeDefinitionsParser.FunctionDefinitionContext) {
            rewriter.replace(ctx.DECLARE(), ctx.FUNCTION(), "external fun")
            val findGeneric = ctx.findGeneric()
            if (findGeneric != null) {
                rewriter.insertBefore(
                    ctx.functionName,
                    findGeneric.rewriterText() + " ",
                    InsertionType.FORMAT
                )
                rewriter.delete(findGeneric)
            }
            val unionType = ctx.findUnionType()
            if (unionType != null) {
                val modifier = if (unionType.text.contains(NULL_TYPES.joinToString("|").toRegex())) "?" else ""
                rewriter.insertBefore(ctx, "/** Any = ${unionType.rewriterText()} */\n", InsertionType.FORMAT)
                rewriter.replace(unionType, "Any$modifier")
            }
        }

        override fun exitDestructuringParameters(ctx: TypeDefinitionsParser.DestructuringParametersContext) {
            val classContext = ctx.findClassContext()

            if (classContext != null) {
                classContext.findVariableDefinition().forEachIndexed { index, variableDefinitionContext ->
                    rewriter.insertAfter(
                        ctx.findIdentifier(index),
                        ":" + variableDefinitionContext.let { it.findType() ?: it.findUnionType() }?.rewriterText(),
                        InsertionType.FORMAT
                    )
                }
            } else {
                val name = ctx.findType()?.findNamedType()?.findTypeName()?.text
                val typeDefinition = typeAliases[name]
                processParentHierarchy(typeDefinition, ctx)
                rewriter.delete(ctx.findType())
            }
            rewriter.delete(ctx.OPEN_BRACE())
            rewriter.delete(ctx.CLOSE_BRACE())
            rewriter.delete(classContext)
            rewriter.delete(ctx.COMMA().last())
            rewriter.delete(ctx.COLON())
        }

        private fun processParentHierarchy(
            typeDefinition: ParserRuleContext?,
            ctx: TypeDefinitionsParser.DestructuringParametersContext
        ) {
            if (typeDefinition is TypeDefinitionsParser.TypeAliasContext) {
                val variableDefinitionContexts =
                    typeDefinition.findClassContext().flatMap { it.findVariableDefinition() }
                ctx.findIdentifier().forEach { parameter ->
                    val parameterType =
                        variableDefinitionContexts.find { it.findIdentifier()?.text == parameter.text }?.let {
                            it.findType() ?: it.findUnionType()
                        }
                    if (parameterType != null) {
                        rewriter.insertAfter(parameter, ":" + parameterType?.rewriterText(), InsertionType.FORMAT)
                    } else {
                        typeDefinition.extensions.forEach {
                            processParentHierarchy(typeAliases[it.findNamedType()?.findTypeName()?.text], ctx)
                        }
                    }
                }
            }
        }

        override fun exitRestrictedValues(ctx: TypeDefinitionsParser.RestrictedValuesContext) {
            rewriter.replace(ctx.DECLARE(), ctx.TYPE(), "enum class")
            rewriter.replace(ctx.EQUALS(), "{")
            val strings = mutableListOf<String>()
            ctx.findString().forEach {
                var stringContent = it.text.removeSurrounding("\"")
                if (KOTLIN_KEYWORDS.contains(stringContent)) {
                    stringContent = "`$stringContent`"
                }
                rewriter.replace(it, stringContent)
                strings.add(stringContent)
            }
            ctx.PIPE().forEach {
                rewriter.replace(it, ",")
            }
            val unionContent = strings.joinToString(",") { "$it: '$it'" }
            val comment = getPrecedingCommentToken(ctx)
            additionalFile += """
                    |${comment?.text ?: ""}
                    |@Suppress("NAME_CONTAINS_ILLEGAL_CHARS")
                    |// language=JavaScript
                    |@JsName(""${'"'}(/*union*/{$unionContent}/*union*/)""${'"'})
                    |
""".trimMargin() + ctx.rewriterText() + "}\n"
            comment?.text = ""
            rewriter.delete(ctx)
        }

        private fun getPrecedingCommentToken(ctx: ParserRuleContext) =
            tokenStream.getHiddenTokensToLeft(ctx.start!!.tokenIndex)
                ?.firstOrNull() { it.type == TypeDefinitionsParser.Tokens.COMMENT.id } as? CommonToken

        override fun exitExport(ctx: TypeDefinitionsParser.ExportContext) {
            rewriter.delete(ctx)
        }

        override fun exitSemiColon(ctx: TypeDefinitionsParser.SemiColonContext) {
            rewriter.delete(ctx)
        }
    }

    fun translate(code: String, moduleName: String): List<TranslationResult> {
        @Suppress("DEPRECATION") // old is faster
        val typeScriptLexer = TypeDefinitionsLexer(ANTLRInputStream(code))
        val commonTokenStream = CommonTokenStream(typeScriptLexer)
        val typeScriptParser = TypeDefinitionsParser(commonTokenStream)
        typeScriptParser.errorHandler = BailErrorStrategy()
        val main = typeScriptParser.main()
        val listener = TypeDefinitionToKotlin(commonTokenStream, moduleName)
        ParseTreeWalker().walk(listener, main)
        return listener.result
    }
}