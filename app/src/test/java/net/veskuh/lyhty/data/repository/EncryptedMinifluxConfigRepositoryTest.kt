package net.veskuh.lyhty.data.repository

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EncryptedMinifluxConfigRepositoryTest {

    @Test
    fun `EncryptedMinifluxConfigRepository saves and retrieves encrypted credentials`() = runTest {
        val repo = EncryptedMinifluxConfigRepositoryImpl(
            ApplicationProvider.getApplicationContext()
        )

        repo.saveConfig("https://reader.example.com", "secure-api-key-999")

        assertEquals("https://reader.example.com", repo.getServerUrlSync())
        assertEquals("secure-api-key-999", repo.getApiKeySync())

        val config = repo.config.first()
        assertEquals("https://reader.example.com", config.serverUrl)
        assertEquals("secure-api-key-999", config.apiKey)
    }
}
