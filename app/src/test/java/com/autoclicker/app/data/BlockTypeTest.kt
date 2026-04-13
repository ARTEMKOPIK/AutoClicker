package com.autoclicker.app.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for BlockType sealed interface.
 */
class BlockTypeTest {

    @Test
    fun `fromString returns correct block type for valid names`() {
        assertThat(BlockType.fromString("click")).isEqualTo(BlockType.CLICK)
        assertThat(BlockType.fromString("CLICK")).isEqualTo(BlockType.CLICK)
        assertThat(BlockType.fromString("Click")).isEqualTo(BlockType.CLICK)
        
        assertThat(BlockType.fromString("sleep")).isEqualTo(BlockType.SLEEP)
        assertThat(BlockType.fromString("loop")).isEqualTo(BlockType.LOOP)
        assertThat(BlockType.fromString("log")).isEqualTo(BlockType.LOG)
    }

    @Test
    fun `fromString returns null for invalid names`() {
        assertThat(BlockType.fromString("invalid")).isNull()
        assertThat(BlockType.fromString("")).isNull()
        assertThat(BlockType.fromString(null)).isNull()
    }

    @Test
    fun `getAllTypes returns all block types`() {
        val allTypes = BlockType.getAllTypes()
        
        assertThat(allTypes).contains(BlockType.CLICK)
        assertThat(allTypes).contains(BlockType.SLEEP)
        assertThat(allTypes).contains(BlockType.LOOP)
        assertThat(allTypes).contains(BlockType.LOG)
        assertThat(allTypes).contains(BlockType.BREAK)
        
        // Should contain 28 block types
        assertThat(allTypes).hasSize(28)
    }

    @Test
    fun `getActionBlocks returns only action blocks`() {
        val actionBlocks = BlockType.getActionBlocks()
        
        assertThat(actionBlocks).containsExactly(
            BlockType.CLICK,
            BlockType.LONG_CLICK,
            BlockType.SWIPE,
            BlockType.TAP
        )
    }

    @Test
    fun `getWaitBlocks returns only wait blocks`() {
        val waitBlocks = BlockType.getWaitBlocks()
        
        assertThat(waitBlocks).containsExactly(
            BlockType.SLEEP,
            BlockType.WAIT_COLOR,
            BlockType.WAIT_TEXT
        )
    }

    @Test
    fun `getConditionBlocks returns only condition blocks`() {
        val conditionBlocks = BlockType.getConditionBlocks()
        
        assertThat(conditionBlocks).containsExactly(
            BlockType.IF_COLOR,
            BlockType.IF_TEXT,
            BlockType.IF_IMAGE
        )
    }

    @Test
    fun `getLoopBlocks returns only loop blocks`() {
        val loopBlocks = BlockType.getLoopBlocks()
        
        assertThat(loopBlocks).containsExactly(
            BlockType.LOOP,
            BlockType.LOOP_COUNT
        )
    }

    @Test
    fun `getVariableBlocks returns only variable blocks`() {
        val variableBlocks = BlockType.getVariableBlocks()
        
        assertThat(variableBlocks).containsExactly(
            BlockType.SET_VAR,
            BlockType.INC_VAR,
            BlockType.DEC_VAR
        )
    }

    @Test
    fun `getOutputBlocks returns only output blocks`() {
        val outputBlocks = BlockType.getOutputBlocks()
        
        assertThat(outputBlocks).containsExactly(
            BlockType.LOG,
            BlockType.TOAST,
            BlockType.TELEGRAM
        )
    }

    @Test
    fun `getSystemBlocks returns only system blocks`() {
        val systemBlocks = BlockType.getSystemBlocks()
        
        assertThat(systemBlocks).containsExactly(
            BlockType.BACK,
            BlockType.HOME,
            BlockType.RECENTS
        )
    }

    @Test
    fun `getSpecialBlocks returns only special blocks`() {
        val specialBlocks = BlockType.getSpecialBlocks()
        
        assertThat(specialBlocks).containsExactly(
            BlockType.COMMENT,
            BlockType.BREAK
        )
    }
}
