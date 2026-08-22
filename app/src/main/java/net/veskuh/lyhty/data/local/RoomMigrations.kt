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
}
