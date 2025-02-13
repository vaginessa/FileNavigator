package com.w2sv.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.w2sv.database.dao.MoveEntryDao
import com.w2sv.database.model.MoveEntryEntity
import com.w2sv.database.typeconverter.FileTypeConverter
import com.w2sv.database.typeconverter.LocalDateTimeConverter
import com.w2sv.database.typeconverter.UriConverter

@Database(
    entities = [MoveEntryEntity::class],
    version = 3,
    exportSchema = true,
)
@TypeConverters(
    LocalDateTimeConverter::class,
    UriConverter::class,
    FileTypeConverter::class
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getMoveEntryDao(): MoveEntryDao
}