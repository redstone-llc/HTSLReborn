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
        val compiledActions = mutableListOf<Action>()
        val gotoCompiled = mutableListOf<Pair<String, List<Action>?>>()

        var conditions = mutableListOf<Condition>()
        var conditional: String? = null
        var isRandom = false
        val depth = mutableMapOf<Int, Pair<MutableList<Action>, MutableList<Action>>>(
            0 to Pair(mutableListOf(), mutableListOf()),
        )
        var isElse = false

        val iterator = tokens.listIterator()

        while (iterator.hasNext()) {
            val token = iterator.next()

            when (token.tokenType) {
                Tokens.GOTO_KEYWORD -> {
                    val previous = gotoCompiled.indexOfLast { it.second == null }
                    if (previous != -1) {
                        gotoCompiled[previous] = Pair(gotoCompiled[previous].first, compiledActions.toList())
                        compiledActions.clear()
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
                        gotoCompiled.add(Pair("base", compiledActions.toList()))
                        compiledActions.clear()
                    }

                    gotoCompiled.add(Pair(args, null))
                }

                Tokens.RANDOM_KEYWORD -> {
                    isRandom = true
                }

                Tokens.IF_OR_CONDITION_START -> {
                    conditional = "or"
                    conditions = mutableListOf()
                }

                Tokens.IF_AND_CONDITION_START -> {
                    conditional = "and"
                    conditions = mutableListOf()
                }

                Tokens.CONDITION_KEYWORD -> {
                    if (conditional != null) {
                        val index = tokens.indexOf(token)
                        if (index > 0 && tokens[index - 1].tokenType == Tokens.INVERTED) {
                            conditions.add(ConditionParser.createCondition(token.string, iterator, path, true) ?: htslCompileError("A condition failed to parse", token))
                            continue
                        }

                        conditions.add(ConditionParser.createCondition(token.string, iterator, path) ?: htslCompileError("A condition failed to parse", token))
                    }
                }

                Tokens.COMMA -> {
                    if (conditional != null && iterator.hasNext()) {
                        val token = iterator.next()
                        val index = tokens.indexOf(token)
                        if (index > 0 && tokens[index].tokenType == Tokens.INVERTED) {
                            conditions.add(ConditionParser.createCondition(iterator.next().string, iterator, path, true) ?: htslCompileError("A condition failed to parse", token))
                            continue
                        }
                        conditions.add(ConditionParser.createCondition(token.string, iterator, path) ?: htslCompileError("A condition failed to parse", token))
                    }
                }

                Tokens.DEPTH_ADD -> {
                    depth[depth.size] = Pair(mutableListOf(), mutableListOf())
                }

                Tokens.ELSE_KEYWORD -> {
                    val actions = depth[depth.size - 1]!!
                    depth[depth.size - 1] = Pair(actions.first, mutableListOf())
                    isElse = true
                }

                Tokens.DEPTH_SUBTRACT -> {
                    if (isRandom) {
                        val actions = depth[depth.size - 1]!!
                        compiledActions.add(RandomAction(actions.first))
                        isRandom = false
                        depth.remove(depth.size - 1)
                        continue
                    }

                    if (isElse) {
                        isElse = false
                    }

                    val actions = depth[depth.size - 1]!!
                    compiledActions.add(Conditional(conditions, conditional == "or", actions.first, actions.second))
                    depth.remove(depth.size - 1)
                }

                Tokens.ACTION_KEYWORD -> {
                    val action = ActionParser.createAction(token.string, iterator, path) ?: htslCompileError("An action failed to parse", token)

                    if (depth.size - 1 == 0) {
                        compiledActions.add(action)
                        continue
                    } else {
                        if (isElse) {
                            depth[depth.size - 1]!!.second.add(action)
                        } else {
                            depth[depth.size - 1]!!.first.add(action)
                        }
                    }
                }

                Tokens.COPY_KEYWORD -> {
                    compiledActions.add(Action.CustomAction("COPY", function = {
                        SystemsAPI.launch {
                            SystemsAPI.getHousingImporter().getOpenActionContainer()?.copyToHousingClipboard()
                        }
                    }))
                }
                Tokens.PASTE_KEYWORD -> {
                    compiledActions.add(Action.CustomAction("PASTE", function = {
                        SystemsAPI.launch {
                            SystemsAPI.getHousingImporter().getOpenActionContainer()?.pasteFromHousingClipboard()
                        }
                    }))
                }
            }
        }

        if (gotoCompiled.isEmpty()) {
            gotoCompiled.add(Pair("base", compiledActions.toList()))
        } else {
            val previous = gotoCompiled.indexOfLast { it.second == null }
            if (previous != -1) {
                gotoCompiled[previous] = Pair(gotoCompiled[previous].first, compiledActions.toList())
            }
        }

        val returned = mutableListOf<Pair<String, List<Action>>>()
        for (entry in gotoCompiled) {
            returned.add(Pair(entry.first, entry.second ?: error("Goto '${entry.first}' is missing a body")))
        }

        return returned
    }
}