package com.attentionbeeper

import org.junit.Assert.*
import org.junit.Test

class SoundSynthesizerTest {

    private val soundIds = listOf(
        "digital", "ping", "pluck", "ding", "danger", "chime",
        "bell", "alert", "drop", "bubble", "woodblock", "chord",
        "blip", "whoosh", "click"
    )

    @Test
    fun `each sound generator returns non-empty ShortArray`() {
        for (id in soundIds) {
            val samples = SoundSynthesizer.generate(id)
            assertTrue("Sound '$id' returned empty array", samples.isNotEmpty())
        }
    }

    @Test
    fun `each sound generator returns non-zero samples`() {
        for (id in soundIds) {
            val samples = SoundSynthesizer.generate(id)
            val hasNonZero = samples.any { it != 0.toShort() }
            assertTrue("Sound '$id' returned all-zero samples", hasNonZero)
        }
    }

    @Test
    fun `unknown sound id falls back to digital`() {
        val fallback = SoundSynthesizer.generate("unknown_sound_xyz")
        val digital = SoundSynthesizer.generate("digital")
        assertArrayEquals(digital, fallback)
    }
}
