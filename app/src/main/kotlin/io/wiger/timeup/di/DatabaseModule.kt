package io.wiger.timeup.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.wiger.timeup.data.local.TimeUpDatabase
import io.wiger.timeup.data.local.dao.AppInfoDao
import io.wiger.timeup.data.local.dao.TimeUpDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideTimeUpDatabase(
        @ApplicationContext context: Context,
    ): TimeUpDatabase = Room.databaseBuilder(
        context,
        TimeUpDatabase::class.java,
        "time_up_database",
    ).build()

    @Provides
    fun provideTimeUpDao(db: TimeUpDatabase): TimeUpDao = db.timeUpDao()

    @Provides
    fun provideAppInfoDao(db: TimeUpDatabase): AppInfoDao = db.appInfoDao()
}
