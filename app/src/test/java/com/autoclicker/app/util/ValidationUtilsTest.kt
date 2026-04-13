package com.autoclicker.app.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for coordinate and parameter validation utilities.
 */
class ValidationUtilsTest {

    @Test
    fun `isValidCoordinate returns true for valid screen coordinates`() {
        // Assuming typical screen bounds
        assertThat(ValidationUtils.isValidCoordinate(0, 0, 1920, 1080)).isTrue()
        assertThat(ValidationUtils.isValidCoordinate(100, 200, 1920, 1080)).isTrue()
        assertThat(ValidationUtils.isValidCoordinate(1919, 1079, 1920, 1080)).isTrue()
    }

    @Test
    fun `isValidCoordinate returns false for negative coordinates`() {
        assertThat(ValidationUtils.isValidCoordinate(-1, 100, 1920, 1080)).isFalse()
        assertThat(ValidationUtils.isValidCoordinate(100, -1, 1920, 1080)).isFalse()
        assertThat(ValidationUtils.isValidCoordinate(-1, -1, 1920, 1080)).isFalse()
    }

    @Test
    fun `isValidCoordinate returns false for out-of-bounds coordinates`() {
        assertThat(ValidationUtils.isValidCoordinate(1920, 100, 1920, 1080)).isFalse()
        assertThat(ValidationUtils.isValidCoordinate(100, 1080, 1920, 1080)).isFalse()
        assertThat(ValidationUtils.isValidCoordinate(2000, 2000, 1920, 1080)).isFalse()
    }

    @Test
    fun `isValidSleepDuration returns true for non-negative values`() {
        assertThat(ValidationUtils.isValidSleepDuration(0)).isTrue()
        assertThat(ValidationUtils.isValidSleepDuration(100)).isTrue()
        assertThat(ValidationUtils.isValidSleepDuration(1000)).isTrue()
    }

    @Test
    fun `isValidSleepDuration returns false for negative values`() {
        assertThat(ValidationUtils.isValidSleepDuration(-1)).isFalse()
        assertThat(ValidationUtils.isValidSleepDuration(-100)).isFalse()
    }

    @Test
    fun `clampCoordinate returns value within bounds`() {
        assertThat(ValidationUtils.clampCoordinate(-10, 0, 1920)).isEqualTo(0)
        assertThat(ValidationUtils.clampCoordinate(2000, 0, 1920)).isEqualTo(1920)
        assertThat(ValidationUtils.clampCoordinate(500, 0, 1920)).isEqualTo(500)
    }

    @Test
    fun `parseColor handles hex color formats`() {
        // Test with #RRGGBB format
        val color1 = ValidationUtils.parseColor("#FF0000")
        assertThat(color1).isNotNull()
        
        // Test with RRGGBB format (without #)
        val color2 = ValidationUtils.parseColor("00FF00")
        assertThat(color2).isNotNull()
        
        // Test with named colors
        val color3 = ValidationUtils.parseColor("red")
        assertThat(color3).isNotNull()
    }

    @Test
    fun `parseColor returns null for invalid formats`() {
        assertThat(ValidationUtils.parseColor("invalid")).isNull()
        assertThat(ValidationUtils.parseColor("#GGHHII")).isNull()
        assertThat(ValidationUtils.parseColor("")).isNull()
    }
}
