package net.veskuh.lyhty.util

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NetworkMonitorTest {

    @Test
    fun `NetworkMonitorImpl initializes and exposes isOnline flow`() = runTest {
        val monitor = NetworkMonitorImpl(ApplicationProvider.getApplicationContext())
        val isOnline = monitor.isOnline.first()

        assertNotNull(isOnline)
    }
}
