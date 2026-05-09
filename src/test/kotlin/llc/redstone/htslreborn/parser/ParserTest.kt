package llc.redstone.htslreborn.parser

import llc.redstone.htslreborn.tokenizer.Tokenizer
import llc.redstone.systemsdata.Action
import llc.redstone.systemsdata.Comparison
import llc.redstone.systemsdata.Condition
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
    fun testNestedConditionalsKeepTheirOwnConditions() {
        val input = """
            if (isSneaking) {
                if (isFlying) {
                    chat nested
                }
            }
        """.trimIndent()

        val tokens = Tokenizer.tokenize(input)
        val preProcessedTokens = PreProcess.preProcess(tokens)
        val actions = Parser.parse(preProcessedTokens, Path("test.htsl")).toMap()["base"] ?: emptyList()

        val outer = actions.single() as Action.Conditional
        val inner = outer.ifActions.single() as Action.Conditional

        assertTrue(outer.conditions.single() is Condition.PlayerSneaking)
        assertTrue(inner.conditions.single() is Condition.PlayerFlying)
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
}
