package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.CustomToolRepository
import com.example.model.JobStatus
import com.example.network.sse.JobEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Termux CommandDeck", appName)
    }

    @Test
    fun `test job event progress parsing`() {
        val json = """{"percent": 42.5, "speed": "5.2MiB/s", "eta": "00:15"}"""
        val event = JobEvent.parse("progress", json)
        assertTrue(event is JobEvent.Progress)
        val progressEvent = event as JobEvent.Progress
        assertEquals(42.5f, progressEvent.percent)
        assertEquals("5.2MiB/s", progressEvent.speed)
        assertEquals("00:15", progressEvent.eta)
    }

    @Test
    fun `test structural argument interpolation`() {
        // Test custom tool template resolution
        val template = listOf("-i", "{input}", "-c", "copy", "{output}")
        val inputs = mapOf("input" to "/storage/video.mp4", "output" to "/storage/output.mp4")

        val resolved = template.map { arg ->
            var r = arg
            for ((k, v) in inputs) {
                r = r.replace("{$k}", v)
            }
            r
        }

        assertEquals(listOf("-i", "/storage/video.mp4", "-c", "copy", "/storage/output.mp4"), resolved)
    }
}

