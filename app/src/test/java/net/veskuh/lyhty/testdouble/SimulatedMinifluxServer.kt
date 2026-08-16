package net.veskuh.lyhty.testdouble

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import java.util.concurrent.ConcurrentLinkedQueue

class SimulatedMinifluxServer {

    private val mockWebServer = MockWebServer()
    private val forcedResponses = ConcurrentLinkedQueue<MockResponse>()

    val baseUrl: String get() = mockWebServer.url("/").toString()

    fun enqueueError(code: Int, message: String = "Server Error") {
        forcedResponses.add(
            MockResponse()
                .setResponseCode(code)
                .setBody("""{"error_message": "$message"}""")
        )
    }

    fun start() {
        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val forced = forcedResponses.poll()
                if (forced != null) return forced

                val token = request.getHeader("X-Auth-Token")

                if (token == "invalid_token") {
                    return MockResponse()
                        .setResponseCode(401)
                        .setBody("""{"error_message": "Unauthorized"}""")
                }

                val path = request.path ?: ""
                val method = request.method ?: "GET"

                return when {
                    // Import / Export OPML
                    path.contains("v1/import") && method == "POST" -> {
                        MockResponse().setResponseCode(201).setBody("""{"message": "Imported"}""")
                    }
                    path.contains("v1/export") && method == "GET" -> {
                        MockResponse().setResponseCode(200).setBody("""<opml version="2.0"><body></body></opml>""")
                    }

                    // Feeds
                    path.contains("v1/discover") && method == "POST" -> {
                        MockResponse().setResponseCode(200).setBody("""[{"title": "Discovered", "type": "rss", "url": "https://example.com/rss"}]""")
                    }
                    path.contains("v1/feeds/refresh") && method == "PUT" -> {
                        MockResponse().setResponseCode(204)
                    }
                    path.matches(Regex(".*/v1/feeds/\\d+/refresh.*")) && method == "PUT" -> {
                        MockResponse().setResponseCode(204)
                    }
                    path.matches(Regex(".*/v1/feeds/\\d+/icon.*")) && method == "GET" -> {
                        MockResponse().setResponseCode(200).setBody("""{"id": 1, "mime_type": "image/png", "data": "base64data"}""")
                    }
                    path.matches(Regex(".*/v1/feeds/\\d+/entries.*")) && method == "GET" -> {
                        MockResponse().setResponseCode(200).setBody(MOCK_ENTRIES_JSON)
                    }
                    path.matches(Regex(".*/v1/feeds/\\d+.*")) && method == "GET" -> {
                        MockResponse().setResponseCode(200).setBody(MOCK_SINGLE_FEED_JSON)
                    }
                    path.matches(Regex(".*/v1/feeds/\\d+.*")) && method == "PUT" -> {
                        MockResponse().setResponseCode(200).setBody(MOCK_SINGLE_FEED_JSON)
                    }
                    path.matches(Regex(".*/v1/feeds/\\d+.*")) && method == "DELETE" -> {
                        MockResponse().setResponseCode(204)
                    }
                    path.contains("v1/feeds") && method == "POST" -> {
                        MockResponse().setResponseCode(201).setBody(MOCK_SINGLE_FEED_JSON)
                    }
                    path.contains("v1/feeds") && method == "GET" -> {
                        MockResponse().setResponseCode(200).setBody(MOCK_FEEDS_JSON)
                    }

                    // Entries
                    path.contains("fetch-content") -> {
                        MockResponse().setResponseCode(200).setBody(MOCK_FETCH_CONTENT_JSON)
                    }
                    path.matches(Regex(".*/v1/entries/\\d+.*")) && method == "GET" -> {
                        MockResponse().setResponseCode(200).setBody(MOCK_SINGLE_ENTRY_JSON)
                    }
                    path.contains("v1/entries") && method == "GET" -> {
                        MockResponse().setResponseCode(200).setBody(MOCK_ENTRIES_JSON)
                    }
                    path.contains("v1/entries") && method == "PUT" -> {
                        MockResponse().setResponseCode(204)
                    }

                    // Categories
                    path.matches(Regex(".*/v1/categories/\\d+/mark-all-as-read.*")) && method == "PUT" -> {
                        MockResponse().setResponseCode(204)
                    }
                    path.matches(Regex(".*/v1/categories/\\d+/entries.*")) && method == "GET" -> {
                        MockResponse().setResponseCode(200).setBody(MOCK_ENTRIES_JSON)
                    }
                    path.matches(Regex(".*/v1/categories/\\d+/feeds.*")) && method == "GET" -> {
                        MockResponse().setResponseCode(200).setBody(MOCK_FEEDS_JSON)
                    }
                    path.matches(Regex(".*/v1/categories/\\d+.*")) && method == "PUT" -> {
                        MockResponse().setResponseCode(200).setBody(MOCK_SINGLE_CATEGORY_JSON)
                    }
                    path.matches(Regex(".*/v1/categories/\\d+.*")) && method == "DELETE" -> {
                        MockResponse().setResponseCode(204)
                    }
                    path.contains("v1/categories") && method == "POST" -> {
                        MockResponse().setResponseCode(201).setBody(MOCK_SINGLE_CATEGORY_JSON)
                    }
                    path.contains("v1/categories") && method == "GET" -> {
                        MockResponse().setResponseCode(200).setBody(MOCK_CATEGORIES_JSON)
                    }

                    // User
                    path.contains("v1/me") -> {
                        MockResponse().setResponseCode(200).setBody(MOCK_USER_JSON)
                    }
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        mockWebServer.start()
    }

    fun shutdown() {
        mockWebServer.shutdown()
    }

    companion object {
        const val VALID_TEST_API_KEY = "test_miniflux_api_key_12345"

        val MOCK_USER_JSON = """
            {
                "id": 1,
                "username": "testuser",
                "is_admin": true,
                "theme": "dark",
                "language": "en_US",
                "timezone": "UTC"
            }
        """.trimIndent()

        val MOCK_SINGLE_CATEGORY_JSON = """
            {
                "id": 1,
                "title": "Tech",
                "user_id": 1
            }
        """.trimIndent()

        val MOCK_CATEGORIES_JSON = """
            [
                {
                    "id": 1,
                    "title": "Tech",
                    "user_id": 1
                },
                {
                    "id": 2,
                    "title": "Design",
                    "user_id": 1
                }
            ]
        """.trimIndent()

        val MOCK_SINGLE_FEED_JSON = """
            {
                "id": 10,
                "title": "TechCrunch",
                "site_url": "https://techcrunch.com",
                "feed_url": "https://techcrunch.com/feed/",
                "category": { "id": 1, "title": "Tech", "user_id": 1 },
                "parsing_error_count": 0
            }
        """.trimIndent()

        val MOCK_FEEDS_JSON = """
            [
                {
                    "id": 10,
                    "title": "TechCrunch",
                    "site_url": "https://techcrunch.com",
                    "feed_url": "https://techcrunch.com/feed/",
                    "category": { "id": 1, "title": "Tech", "user_id": 1 },
                    "parsing_error_count": 0
                },
                {
                    "id": 20,
                    "title": "Smashing Magazine",
                    "site_url": "https://smashingmagazine.com",
                    "feed_url": "https://smashingmagazine.com/feed/",
                    "category": { "id": 2, "title": "Design", "user_id": 1 },
                    "parsing_error_count": 0
                }
            ]
        """.trimIndent()

        val MOCK_SINGLE_ENTRY_JSON = """
            {
                "id": 101,
                "user_id": 1,
                "feed_id": 10,
                "status": "unread",
                "title": "Android 15 Released with Foldable Enhancements",
                "url": "https://techcrunch.com/android-15-foldables",
                "comments_url": "",
                "author": "Sarah Connor",
                "content": "<p>Google released Android 15 bringing brand new Material 3 Adaptive scaffolds and enhanced foldable posture support for dual-screen devices.</p>",
                "published_at": "2026-08-16T12:00:00Z",
                "created_at": "2026-08-16T12:05:00Z",
                "feed": {
                    "id": 10,
                    "title": "TechCrunch",
                    "site_url": "https://techcrunch.com",
                    "feed_url": "https://techcrunch.com/feed/",
                    "category": { "id": 1, "title": "Tech", "user_id": 1 }
                }
            }
        """.trimIndent()

        val MOCK_ENTRIES_JSON = """
            {
                "total": 2,
                "entries": [
                    {
                        "id": 101,
                        "user_id": 1,
                        "feed_id": 10,
                        "status": "unread",
                        "title": "Android 15 Released with Foldable Enhancements",
                        "url": "https://techcrunch.com/android-15-foldables",
                        "comments_url": "",
                        "author": "Sarah Connor",
                        "content": "<p>Google released Android 15 bringing brand new Material 3 Adaptive scaffolds and enhanced foldable posture support for dual-screen devices.</p>",
                        "published_at": "2026-08-16T12:00:00Z",
                        "created_at": "2026-08-16T12:05:00Z",
                        "feed": {
                            "id": 10,
                            "title": "TechCrunch",
                            "site_url": "https://techcrunch.com",
                            "feed_url": "https://techcrunch.com/feed/",
                            "category": { "id": 1, "title": "Tech", "user_id": 1 }
                        }
                    },
                    {
                        "id": 102,
                        "user_id": 1,
                        "feed_id": 20,
                        "status": "unread",
                        "title": "Designing for Dual Display & Foldable Screens",
                        "url": "https://smashingmagazine.com/foldable-ui-design",
                        "comments_url": "",
                        "author": "Vitaly Friedman",
                        "content": "<p>Learn how to design multi-pane desktop layouts using ThreePaneScaffold and ListDetailPaneScaffold in Compose M3.</p>",
                        "published_at": "2026-08-16T10:00:00Z",
                        "created_at": "2026-08-16T10:05:00Z",
                        "feed": {
                            "id": 20,
                            "title": "Smashing Magazine",
                            "site_url": "https://smashingmagazine.com",
                            "feed_url": "https://smashingmagazine.com/feed/",
                            "category": { "id": 2, "title": "Design", "user_id": 1 }
                        }
                    }
                ]
            }
        """.trimIndent()

        val MOCK_FETCH_CONTENT_JSON = """
            {
                "content": "<article><h1>Full Extracted Article Content</h1><p>This is full extracted text fetched directly from the Miniflux server readability engine.</p></article>"
            }
        """.trimIndent()
    }
}
