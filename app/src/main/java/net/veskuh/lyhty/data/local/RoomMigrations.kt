package net.veskuh.lyhty.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object RoomMigrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Explicit Room Migration script handling database schema updates safely
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_entries_categoryId` ON `entries` (`categoryId`)")
            db.execSQL("DROP TABLE IF EXISTS `sync_queue`")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `entries` ADD COLUMN `starred` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_entries_starred` ON `entries` (`starred`)")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `reading_history` (
                    `entryId` INTEGER PRIMARY KEY NOT NULL,
                    `readAt` INTEGER NOT NULL,
                    FOREIGN KEY(`entryId`) REFERENCES `entries`(`id`) ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_reading_history_readAt` ON `reading_history`(`readAt`)")
        }
    }
}
