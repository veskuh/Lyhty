package net.veskuh.lyhty.data.local.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class EntryFtsEntityTest {

    @Test
    fun `EntryFtsEntity data class instantiation and properties`() {
        val ftsEntity = EntryFtsEntity(
            rowid = 101,
            title = "Test FTS Title",
            content = "Test FTS Content",
            author = "Author Name",
            feedTitle = "Feed Name"
        )

        assertEquals(101, ftsEntity.rowid)
        assertEquals("Test FTS Title", ftsEntity.title)
        assertEquals("Test FTS Content", ftsEntity.content)
        assertEquals("Author Name", ftsEntity.author)
        assertEquals("Feed Name", ftsEntity.feedTitle)

        val copy = ftsEntity.copy(title = "New Title")
        assertEquals("New Title", copy.title)
        assertNotNull(ftsEntity.toString())
    }
}
