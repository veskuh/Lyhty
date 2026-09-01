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

    @Test
    fun `MIGRATION_2_3 executes valid DDL statement for starred column and index`() {
        val mockDb: SupportSQLiteDatabase = mockk(relaxed = true)

        RoomMigrations.MIGRATION_2_3.migrate(mockDb)

        verify {
            mockDb.execSQL("ALTER TABLE `entries` ADD COLUMN `starred` INTEGER NOT NULL DEFAULT 0")
            mockDb.execSQL("CREATE INDEX IF NOT EXISTS `index_entries_starred` ON `entries` (`starred`)")
        }
    }

    @Test
    fun `MIGRATION_3_4 executes valid DDL statement for reading_history table and index`() {
        val mockDb: SupportSQLiteDatabase = mockk(relaxed = true)

        RoomMigrations.MIGRATION_3_4.migrate(mockDb)

        verify {
            mockDb.execSQL(match { it.contains("CREATE TABLE IF NOT EXISTS `reading_history`") })
            mockDb.execSQL("CREATE INDEX IF NOT EXISTS `index_reading_history_readAt` ON `reading_history`(`readAt`)")
        }
    }
}
