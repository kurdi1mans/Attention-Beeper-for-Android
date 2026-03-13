package com.attentionbeeper

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SessionViewModelTest {

    private lateinit var viewModel: SessionViewModel

    @Before
    fun setup() {
        viewModel = SessionViewModel()
    }

    @Test
    fun `defaults match spec`() {
        assertEquals(60, viewModel.intervalValue.value)
        assertEquals(IntervalUnit.SECONDS, viewModel.intervalUnit.value)
        assertEquals(IntervalMode.RANDOM, viewModel.intervalMode.value)
        assertEquals("digital", viewModel.selectedSound.value)
        assertFalse(viewModel.sessionRunning.value)
        assertEquals(-1L, viewModel.countdown.value)
    }

    @Test
    fun `intervalMs seconds`() {
        viewModel.setIntervalValue(30)
        viewModel.setIntervalUnit(IntervalUnit.SECONDS)
        assertEquals(30_000L, viewModel.intervalMs())
    }

    @Test
    fun `intervalMs minutes`() {
        viewModel.setIntervalValue(2)
        viewModel.setIntervalUnit(IntervalUnit.MINUTES)
        assertEquals(120_000L, viewModel.intervalMs())
    }

    @Test
    fun `setRunning true sets sessionRunning`() {
        viewModel.setRunning(true)
        assertTrue(viewModel.sessionRunning.value)
    }

    @Test
    fun `setRunning false resets countdown to -1`() {
        viewModel.setCountdown(5000L)
        viewModel.setRunning(false)
        assertEquals(-1L, viewModel.countdown.value)
        assertFalse(viewModel.sessionRunning.value)
    }

    @Test
    fun `setCountdown updates value`() {
        viewModel.setCountdown(12345L)
        assertEquals(12345L, viewModel.countdown.value)
    }

    @Test
    fun `setIntervalValue updates value`() {
        viewModel.setIntervalValue(15)
        assertEquals(15, viewModel.intervalValue.value)
    }

    @Test
    fun `setIntervalMode updates mode`() {
        viewModel.setIntervalMode(IntervalMode.FIXED)
        assertEquals(IntervalMode.FIXED, viewModel.intervalMode.value)
    }

    @Test
    fun `setSelectedSound updates sound`() {
        viewModel.setSelectedSound("ping")
        assertEquals("ping", viewModel.selectedSound.value)
    }
}
