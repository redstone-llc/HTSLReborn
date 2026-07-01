package llc.redstone.htslreborn.parser

import llc.redstone.htslreborn.tokenizer.Tokenizer
import llc.redstone.systemsdata.Action
import llc.redstone.systemsdata.Comparison
import llc.redstone.systemsdata.Condition
import llc.redstone.systemsdata.StatOp
import llc.redstone.systemsdata.StatValue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.io.path.Path

class ParserTest {

    @Test
    fun testChangeVelocity() {
        val input = """
            changeVelocity %var.player/t% 1.0 0.0
        """.trimIndent()

        val tokens = Tokenizer.tokenize(input)
        val preProcessedTokens = PreProcess.preProcess(tokens)
        val actions = Parser.parse(preProcessedTokens, Path("test.htsl")).toMap()["base"] ?: emptyList()

        assertEquals(1, actions.size, "Should have parsed exactly one action")

        val action = actions[0]
        // Assuming the toString representation is as expected by the user
        // Or we can check properties if we know the class structure
        assertEquals("ChangeVelocity(x=%var.player/t%, y=1.0, z=0.0)", action.toString())
    }

    @Test
    fun testMultipleActions() {
        val input = """
            changeVelocity 1.0 2.0 3.0
            changeVelocity %var.player/x% %var.player/y% %var.player/z%
        """.trimIndent()

        val tokens = Tokenizer.tokenize(input)
        val preProcessedTokens = PreProcess.preProcess(tokens)
        val actions = Parser.parse(preProcessedTokens, Path("test.htsl")).toMap()["base"] ?: emptyList()

        assertEquals(2, actions.size)
        assertEquals("ChangeVelocity(x=1.0, y=2.0, z=3.0)", actions[0].toString())
        assertEquals("ChangeVelocity(x=%var.player/x%, y=%var.player/y%, z=%var.player/z%)", actions[1].toString())
    }

    @Test
    fun testVariableActionAliasesImport() {
        val input = """
            var Kills = 1
            stat Kills = 1
            globalvar Total += 2
            globalstat Total += 2
        """.trimIndent()

        val tokens = Tokenizer.tokenize(input)
        val preProcessedTokens = PreProcess.preProcess(tokens)
        val actions = Parser.parse(preProcessedTokens, Path("test.htsl")).toMap()["base"] ?: emptyList()

        assertEquals(
            listOf(
                Action.PlayerVariable("Kills", StatOp.Set, StatValue.I32(1)),
                Action.PlayerVariable("Kills", StatOp.Set, StatValue.I32(1)),
                Action.GlobalVariable("Total", StatOp.Inc, StatValue.I32(2)),
                Action.GlobalVariable("Total", StatOp.Inc, StatValue.I32(2)),
            ),
            actions
        )
    }

    @Test
    fun testVariableConditionAliasesImport() {
        val input = """
            if (var Kills == 1, stat Kills == 1, globalvar Total >= 2, globalstat Total >= 2) {
                chat ok
            }
        """.trimIndent()

        val tokens = Tokenizer.tokenize(input)
        val preProcessedTokens = PreProcess.preProcess(tokens)
        val actions = Parser.parse(preProcessedTokens, Path("test.htsl")).toMap()["base"] ?: emptyList()

        val conditional = actions.single() as Action.Conditional

        assertEquals(
            listOf(
                Condition.PlayerVariableRequirement("Kills", Comparison.Eq, StatValue.I32(1)),
                Condition.PlayerVariableRequirement("Kills", Comparison.Eq, StatValue.I32(1)),
                Condition.GlobalVariableRequirement("Total", Comparison.Ge, StatValue.I32(2)),
                Condition.GlobalVariableRequirement("Total", Comparison.Ge, StatValue.I32(2)),
            ),
            conditional.conditions
        )
    }

    @Test
    fun testInvertedConditionComparatorImport() {
        val input = """
            if (var Kills != 1, !var Kills != 1) {
                chat ok
            }
        """.trimIndent()

        val tokens = Tokenizer.tokenize(input)
        val preProcessedTokens = PreProcess.preProcess(tokens)
        val actions = Parser.parse(preProcessedTokens, Path("test.htsl")).toMap()["base"] ?: emptyList()

        val conditional = actions.single() as Action.Conditional

        val condition1 = Condition.PlayerVariableRequirement("Kills", Comparison.Eq, StatValue.I32(1))
        condition1.inverted = true

        assertEquals(
            listOf(
                condition1,
                Condition.PlayerVariableRequirement("Kills", Comparison.Eq, StatValue.I32(1)),
            ),
            conditional.conditions
        )
    }

    @Test
    fun testReadableConditionAliasesImport() {
        val input = """
            if (hasGroup default true, inGroup default true, hasTeam red, inTeam red) {
                chat ok
            }
        """.trimIndent()

        val tokens = Tokenizer.tokenize(input)
        val preProcessedTokens = PreProcess.preProcess(tokens)
        val actions = Parser.parse(preProcessedTokens, Path("test.htsl")).toMap()["base"] ?: emptyList()

        val conditional = actions.single() as Action.Conditional

        assertEquals(
            listOf(
                Condition.RequiredGroup("default", true),
                Condition.RequiredGroup("default", true),
                Condition.RequiredTeam("red"),
                Condition.RequiredTeam("red"),
            ),
            conditional.conditions
        )
    }

    @Test
    fun testActionBarKeepsStringValue() {
        val input = """
            actionBar "Kills: %stat.player/Kills%"
        """.trimIndent()

        val tokens = Tokenizer.tokenize(input)
        val preProcessedTokens = PreProcess.preProcess(tokens)
        val actions = Parser.parse(preProcessedTokens, Path("test.htsl")).toMap()["base"] ?: emptyList()

        assertEquals(Action.DisplayActionBar("Kills: %stat.player/Kills%"), actions.single())
    }

    @Test
    fun testActionBarKeepsColorCodeJoinedToPlaceholder() {
        val input = """
            actionBar &e%stat.player/tpsdistv2%
        """.trimIndent()

        val tokens = Tokenizer.tokenize(input)
        val preProcessedTokens = PreProcess.preProcess(tokens)
        val actions = Parser.parse(preProcessedTokens, Path("test.htsl")).toMap()["base"] ?: emptyList()

        assertEquals(Action.DisplayActionBar("&e%stat.player/tpsdistv2%"), actions.single())
    }

    @Test
    fun testActionBarCanImportLiteralNullText() {
        val input = """
            actionBar null
        """.trimIndent()

        val tokens = Tokenizer.tokenize(input)
        val preProcessedTokens = PreProcess.preProcess(tokens)
        val actions = Parser.parse(preProcessedTokens, Path("test.htsl")).toMap()["base"] ?: emptyList()

        assertEquals(Action.DisplayActionBar("null"), actions.single())
    }

    @Test
    fun testChatMessageKeepsJoinPayloadPlaceholders() {
        val input = """
            chat type=join;name=%var.global/join/name%;playerid=%var.global/join/playerid%
        """.trimIndent()

        val tokens = Tokenizer.tokenize(input)
        val preProcessedTokens = PreProcess.preProcess(tokens)
        val actions = Parser.parse(preProcessedTokens, Path("test.htsl")).toMap()["base"] ?: emptyList()

        assertEquals(
            Action.SendMessage("type=join;name=%var.global/join/name%;playerid=%var.global/join/playerid%"),
            actions.single()
        )
    }

    @Test
    fun testNumbersWithCommasParseAsNumbers() {
        val input = """
            pause 1,234
        """.trimIndent()

        val tokens = Tokenizer.tokenize(input)
        val preProcessedTokens = PreProcess.preProcess(tokens)
        val actions = Parser.parse(preProcessedTokens, Path("test.htsl")).toMap()["base"] ?: emptyList()

        assertEquals(Action.PauseExecution(1234), actions.single())
    }

    @Test
    fun testStatValuesWithCommasParseAsNumbers() {
        val input = """
            changeVelocity 1,234 2,345L 3,456.5
        """.trimIndent()

        val tokens = Tokenizer.tokenize(input)
        val preProcessedTokens = PreProcess.preProcess(tokens)
        val actions = Parser.parse(preProcessedTokens, Path("test.htsl")).toMap()["base"] ?: emptyList()

        assertEquals(
            Action.ChangeVelocity(
                StatValue.I32(1234),
                StatValue.Lng(2345),
                StatValue.Dbl(3456.5)
            ),
            actions.single()
        )
    }

    @Test
    fun testConditionKeepsColorCodeJoinedToPlaceholder() {
        val input = """
            if (placeholder &e%stat.player/tpsdistv2% >= 1,234) {
                chat ok
            }
        """.trimIndent()

        val tokens = Tokenizer.tokenize(input)
        val preProcessedTokens = PreProcess.preProcess(tokens)
        val actions = Parser.parse(preProcessedTokens, Path("test.htsl")).toMap()["base"] ?: emptyList()

        val conditional = actions.single() as Action.Conditional
        val condition = conditional.conditions.single() as Condition.RequiredPlaceholderNumber

        assertEquals("&e%stat.player/tpsdistv2%", condition.placeholder)
        assertEquals(Comparison.Ge, condition.mode)
        assertEquals(StatValue.I32(1234), condition.amount)
    }

    @Test
    fun testCustomLocationDoesNotConsumeDefaultStrength() {
        val input = """
            launchTarget "custom_coordinates" 1 2 3
        """.trimIndent()

        val tokens = Tokenizer.tokenize(input)
        val preProcessedTokens = PreProcess.preProcess(tokens)
        val actions = Parser.parse(preProcessedTokens, Path("test.htsl")).toMap()["base"] ?: emptyList()

        val action = actions.single() as Action.LaunchToTarget

        assertEquals("1 2 3", action.location.toString())
        assertEquals(2.0, action.strength)
    }

    @Test
    fun testInvertedConditionAfterComma() {
        val input = """
            if (isSneaking, !isFlying) {
                chat nope
            }
        """.trimIndent()

        val tokens = Tokenizer.tokenize(input)
        val preProcessedTokens = PreProcess.preProcess(tokens)
        val actions = Parser.parse(preProcessedTokens, Path("test.htsl")).toMap()["base"] ?: emptyList()

        val conditional = actions.single() as Action.Conditional

        assertTrue(conditional.conditions[0] is Condition.PlayerSneaking)
        assertTrue(conditional.conditions[1] is Condition.PlayerFlying)
        assertEquals(false, conditional.conditions[0].inverted)
        assertEquals(true, conditional.conditions[1].inverted)
    }

    @Test
    fun testConditionWithPlaceholderShortcut() {
        val input = """
            if and (globalvar test = var test) {
                chat "Hello World"
            }
        """.trimIndent()

        val tokens = Tokenizer.tokenize(input)
        val preProcessedTokens = PreProcess.preProcess(tokens)
        val actions = Parser.parse(preProcessedTokens, Path("test.htsl")).toMap()["base"] ?: emptyList()

        val conditional = actions.single() as Action.Conditional
        assertEquals(1, conditional.conditions.size)
        val condition = conditional.conditions.single() as Condition.GlobalVariableRequirement
        assertEquals("test", condition.variable)
        assertEquals(Comparison.Eq, condition.op)
        assertEquals(StatValue.UnquotedStr("%var.player/test%"), condition.value)
    }
}