package llc.redstone.htslreborn.parser

import llc.redstone.htslreborn.tokenizer.Tokenizer.TokenWithPosition
import llc.redstone.htslreborn.tokenizer.Tokens
import llc.redstone.htslreborn.utils.ErrorUtils.htslCompileError
import llc.redstone.systemsapi.SystemsAPI
import llc.redstone.systemsdata.Action
import llc.redstone.systemsdata.Action.Conditional
import llc.redstone.systemsdata.Action.RandomAction
import llc.redstone.systemsdata.Condition
import java.nio.file.Path

object Parser {
    fun parse(tokens: List<TokenWithPosition>, path: Path?): MutableList<Pair<String, List<Action>>> {
        val gotoCompiled = mutableListOf<Pair<String, List<Action>?>>()

        data class Block(
            val ifActions: MutableList<Action> = mutableListOf(),
            val elseActions: MutableList<Action> = mutableListOf(),
            val conditions: List<Condition>? = null,
            val matchAnyCondition: Boolean = false,
            val random: Boolean = false,
            var inElse: Boolean = false,
        )

        val blocks = mutableListOf(Block())
        var pendingConditions: MutableList<Condition>? = null
        var pendingMatchAnyCondition = false
        var pendingRandom = false

        val iterator = tokens.listIterator()

        fun currentActions(): MutableList<Action> {
            val block = blocks.last()
            return if (block.inElse) block.elseActions else block.ifActions
        }

        fun rootActions(): MutableList<Action> = blocks.first().ifActions

        fun addCondition(token: TokenWithPosition, inverted: Boolean = false) {
            val conditions = pendingConditions ?: htslCompileError("Condition found outside of an if statement", token)
            conditions.add(
                ConditionParser.createCondition(token.string, iterator, path, inverted)
                    ?: htslCompileError("A condition failed to parse", token)
            )
        }

        while (iterator.hasNext()) {
            val token = iterator.next()

            when (token.tokenType) {
                Tokens.GOTO_KEYWORD -> {
                    if (blocks.size != 1) {
                        htslCompileError("Goto calls cannot be used inside blocks", token)
                    }

                    val previous = gotoCompiled.indexOfLast { it.second == null }
                    if (previous != -1) {
                        gotoCompiled[previous] = Pair(gotoCompiled[previous].first, rootActions().toList())
                        rootActions().clear()
                    }

                    val type = iterator.next()
                    val args = when(type.string.lowercase()) {
                        "function" -> {
                            val name = iterator.next().string
                            "function $name"
                        }
                        "event" -> {
                            val name = iterator.next().string
                            "event $name"
                        }
                        "gui" -> {
                            val name = iterator.next().string
                            val slot = iterator.next().string.toInt()
                            "gui $name $slot"
                        }
                        "command" -> {
                            val name = iterator.next().string
                            "command $name"
                        }
                        else -> error("Unexpected token type $type")
                    }

                    if (gotoCompiled.isEmpty()) {
                        gotoCompiled.add(Pair("base", rootActions().toList()))
                        rootActions().clear()
                    }

                    gotoCompiled.add(Pair(args, null))
                }

                Tokens.RANDOM_KEYWORD -> {
                    pendingRandom = true
                }

                Tokens.IF_OR_CONDITION_START -> {
                    pendingMatchAnyCondition = true
                    pendingConditions = mutableListOf()
                }

                Tokens.IF_AND_CONDITION_START -> {
                    pendingMatchAnyCondition = false
                    pendingConditions = mutableListOf()
                }

                Tokens.CONDITION_KEYWORD -> {
                    if (pendingConditions != null) {
                        val index = iterator.previousIndex()
                        addCondition(token, index > 0 && tokens[index - 1].tokenType == Tokens.INVERTED)
                    }
                }

                Tokens.COMMA -> {
                    if (pendingConditions != null && iterator.hasNext()) {
                        val nextToken = iterator.next()
                        if (nextToken.tokenType == Tokens.INVERTED) {
                            val conditionToken = iterator.takeIf { it.hasNext() }?.next()
                                ?: htslCompileError("Expected condition after '!'", nextToken)
                            addCondition(conditionToken, true)
                        } else {
                            addCondition(nextToken)
                        }
                    }
                }

                Tokens.DEPTH_ADD -> {
                    if (pendingRandom) {
                        blocks.add(Block(random = true))
                        pendingRandom = false
                    } else {
                        blocks.add(
                            Block(
                                conditions = pendingConditions?.toList() ?: emptyList(),
                                matchAnyCondition = pendingMatchAnyCondition,
                            )
                        )
                        pendingConditions = null
                        pendingMatchAnyCondition = false
                    }
                }

                Tokens.ELSE_KEYWORD -> {
                    blocks.last().inElse = true
                }

                Tokens.DEPTH_SUBTRACT -> {
                    if (blocks.size == 1) {
                        htslCompileError("Unexpected closing brace", token)
                    }

                    val block = blocks.removeLast()
                    val action = if (block.random) {
                        RandomAction(block.ifActions)
                    } else {
                        Conditional(
                            block.conditions ?: emptyList(),
                            block.matchAnyCondition,
                            block.ifActions,
                            block.elseActions
                        )
                    }
                    currentActions().add(action)
                }

                Tokens.ACTION_KEYWORD -> {
                    val action = ActionParser.createAction(token.string, iterator, path) ?: htslCompileError("An action failed to parse", token)
                    currentActions().add(action)
                }

                Tokens.COPY_KEYWORD -> {
                    currentActions().add(Action.CustomAction("COPY", function = {
                        SystemsAPI.launch {
                            SystemsAPI.getHousingImporter().getOpenActionContainer()?.copyToHousingClipboard()
                        }
                    }))
                }
                Tokens.PASTE_KEYWORD -> {
                    currentActions().add(Action.CustomAction("PASTE", function = {
                        SystemsAPI.launch {
                            SystemsAPI.getHousingImporter().getOpenActionContainer()?.pasteFromHousingClipboard()
                        }
                    }))
                }
            }
        }

        if (blocks.size != 1) {
            htslCompileError("Missing closing brace", tokens.lastOrNull() ?: return mutableListOf())
        }

        if (gotoCompiled.isEmpty()) {
            gotoCompiled.add(Pair("base", rootActions().toList()))
        } else {
            val previous = gotoCompiled.indexOfLast { it.second == null }
            if (previous != -1) {
                gotoCompiled[previous] = Pair(gotoCompiled[previous].first, rootActions().toList())
            }
        }

        val returned = mutableListOf<Pair<String, List<Action>>>()
        for (entry in gotoCompiled) {
            returned.add(Pair(entry.first, entry.second ?: error("Goto '${entry.first}' is missing a body")))
        }

        return returned
    }
}
