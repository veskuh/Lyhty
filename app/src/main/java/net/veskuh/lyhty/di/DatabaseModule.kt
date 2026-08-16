package net.veskuh.lyhty.di

import android.content.Context
import androidx.room.Room
import net.veskuh.lyhty.data.local.LyhtyDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): LyhtyDatabase {
        return Room.databaseBuilder(
            context,
            LyhtyDatabase::class.java,
            "lyhty_miniflux.db"
        )
            .addMigrations(net.veskuh.lyhty.data.local.RoomMigrations.MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()
    }
}
