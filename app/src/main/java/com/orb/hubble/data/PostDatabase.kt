package com.orb.hubble.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.serialization.json.Json
import kotlin.time.Instant

@Database(entities = [Post::class], version = 1)
@TypeConverters(Converters::class)
abstract class PostDatabase: RoomDatabase() {

    abstract val postDao: PostDao
}

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromUser(user: User): String = json.encodeToString(user)

    @TypeConverter
    fun toUser(jsonString: String): User = json.decodeFromString(jsonString)

    @TypeConverter
    fun fromList(tags: List<String>?): String = json.encodeToString(tags)

    @TypeConverter
    fun toList(jsonString: String): List<String>? = json.decodeFromString(jsonString)

    @TypeConverter
    fun fromInstant(value: Instant?): String? {
        return value?.toString()
    }

    @TypeConverter
    fun toInstant(value: String?): Instant? {
        return value?.let { Instant.parse(it) }
    }
}