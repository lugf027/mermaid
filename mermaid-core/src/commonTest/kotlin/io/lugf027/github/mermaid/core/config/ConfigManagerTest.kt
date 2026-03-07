package io.lugf027.github.mermaid.core.config

import io.lugf027.github.mermaid.core.config.ThemeName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ConfigManagerTest {

    @Test
    fun defaultConfigHasDefaultTheme() {
        ConfigManager.resetAll()
        val config = ConfigManager.getConfig()
        assertEquals(ThemeName.DEFAULT, config.theme)
    }

    @Test
    fun setSiteConfigMerges() {
        ConfigManager.resetAll()
        ConfigManager.setSiteConfig(MermaidConfig(theme = ThemeName.DARK))
        val config = ConfigManager.getConfig()
        assertEquals(ThemeName.DARK, config.theme)
    }

    @Test
    fun resetRestoresToDefault() {
        ConfigManager.resetAll()
        ConfigManager.setSiteConfig(MermaidConfig(theme = ThemeName.FOREST))
        ConfigManager.resetAll()
        val config = ConfigManager.getConfig()
        assertEquals(ThemeName.DEFAULT, config.theme)
    }

    @Test
    fun updateConfigWorks() {
        ConfigManager.resetAll()
        ConfigManager.updateConfig { it.copy(theme = ThemeName.NEUTRAL) }
        val config = ConfigManager.getConfig()
        assertEquals(ThemeName.NEUTRAL, config.theme)
    }

    @Test
    fun configIsNotNull() {
        val config = ConfigManager.getConfig()
        assertNotNull(config)
    }
}
