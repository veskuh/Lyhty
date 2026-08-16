package net.veskuh.lyhty.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MinifluxConfigRepositoryTest {

    private lateinit var repository: MinifluxConfigRepository

    @Before
    fun setUp() {
        repository = FakeMinifluxConfigRepository()
    }

    @Test
    fun `default config returns empty server url and api key`() = runTest {
        val config = repository.config.first()
        assertEquals("", config.serverUrl)
        assertEquals("", config.apiKey)
    }

    @Test
    fun `saveConfig updates server url and api key`() = runTest {
        repository.saveConfig("https://miniflux.my-domain.com", "secret_token_abc")

        val updated = repository.config.first()
        assertEquals("https://miniflux.my-domain.com", updated.serverUrl)
        assertEquals("secret_token_abc", updated.apiKey)
    }

    @Test
    fun `saveConfig automatically trims extra leading and trailing whitespace`() = runTest {
        repository.saveConfig("  https://miniflux.my-domain.com/   ", "  secret_token_with_spaces   ")

        val updated = repository.config.first()
        assertEquals("https://miniflux.my-domain.com/", updated.serverUrl)
        assertEquals("secret_token_with_spaces", updated.apiKey)
        assertEquals("secret_token_with_spaces", repository.getApiKeySync())
    }
}
