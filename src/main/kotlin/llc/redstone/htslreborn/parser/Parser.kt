package llc.redstone.htslreborn.parser

import dev.wekend.housingtoolbox.feature.data.Action
import dev.wekend.housingtoolbox.feature.data.Action.Conditional
import dev.wekend.housingtoolbox.feature.data.Action.RandomAction
import dev.wekend.housingtoolbox.feature.data.Condition
import guru.zoroark.tegral.niwen.lexer.Token
import llc.redstone.htslreborn.tokenizer.Tokens

object Parser {
    fun parse(tokens: List<Token>): List<Action> {
        val compiledActions = mutableListOf<Action>()

        var conditions = mutableListOf<Condition>()
        var conditional: String? = null
        var isRandom = false
        val depth = mutableMapOf<Int, Pair<MutableList<Action>, MutableList<Action>>>(
            0 to Pair(mutableListOf(), mutableListOf()),
        )
        var isElse = false
        val iterator = tokens.iterator()
        while (iterator.hasNext()) {
            val token = iterator.next()

            when (token.tokenType) {
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
                            println("Found inverted condition")
                            conditions.add(ConditionParser.createCondition(token.string, iterator, true) ?: run {
                                println("Did not expect null condition")
                                continue
                            })
                            continue
                        }
                        conditions.add(ConditionParser.createCondition(token.string, iterator) ?: run {
                            println("Did not expect null condition")
                            continue
                        })
                    }
                }

                Tokens.COMMA -> {
                    if (conditional != null && iterator.hasNext()) {
                        conditions.add(ConditionParser.createCondition(iterator.next().string, iterator) ?: run {
                            println("Did not expect null condition")
                            continue
                        })
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
                    val action = ActionParser.createAction(token.string, iterator) ?: run {
                        println("Did not expect null")
                        continue
                    }
                    println("Created action: $action")
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

            }
        }

        return compiledActions
    }
}