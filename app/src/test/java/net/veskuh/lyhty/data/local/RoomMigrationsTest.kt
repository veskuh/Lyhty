package net.veskuh.lyhty.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class RoomMigrationsTest {

    @Test
    fun `MIGRATION_1_2 executes valid DDL statement`() {
        val mockDb: SupportSQLiteDatabase = mockk(relaxed = true)

        RoomMigrations.MIGRATION_1_2.migrate(mockDb)

        verify {
            mockDb.execSQL("CREATE INDEX IF NOT EXISTS `index_entries_categoryId` ON `entries` (`categoryId`)")
        }
    }
}
