package com.orb.hubble.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.serialization.json.Json
import kotlin.time.Instant

@Database(entities = [Post::class, Message::class], version = 5)
@TypeConverters(Converters::class)
abstract class PostDatabase : RoomDatabase() {

    abstract val postDao: PostDao
    abstract val messageDao: MessageDao

    companion object {
        val migration1to2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                CREATE TABLE post_new (
                id TEXT NOT NULL PRIMARY KEY,
                contentUrl TEXT,
                thumbnailUrl TEXT,
                description TEXT,
                contentText TEXT,
                thumbnailAt TEXT,
                type TEXT NOT NULL,
                tags TEXT,
                expiresAt TEXT NOT NULL,
                userId TEXT NOT NULL,
                insertedAt TEXT NOT NULL,
                viewed INTEGER NOT NULL DEFAULT 0,
                user TEXT NOT NULL
                )
            """.trimIndent()
                )

                db.execSQL(
                    """
                INSERT INTO post_new (
                id, contentUrl, thumbnailUrl, description, contentText,
                thumbnailAt, type, tags, expiresAt, userId, insertedAt, viewed, user
                )
                SELECT 
                CAST(id AS TEXT), contentUrl, thumbnailUrl, description, contentText,
                thumbnailAt, 'text', tags, expiresAt, CAST(userId AS TEXT), insertedAt, viewed, user
                FROM post
            """.trimIndent()
                )

                db.execSQL("DROP TABLE post")

                db.execSQL("ALTER TABLE post_new RENAME TO post")
            }
        }

        val migration2to3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
            UPDATE post 
            SET user = REPLACE(
                REPLACE(user, '"id":', '"id":"'), 
                ',"username"', '","username"'
            )
            WHERE user LIKE '%"id":%' AND user NOT LIKE '%"id":"%'
        """.trimIndent()
                )
            }
        }

        val migration3to4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `messages` (
                    `id` TEXT NOT NULL,
                    `content` TEXT NOT NULL,
                    `postId` TEXT NOT NULL,
                    `repliedTo` TEXT,
                    `insertedAt` TEXT NOT NULL,
                    `user` TEXT NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`postId`) REFERENCES `post`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent()
                )

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_postId` ON `messages` (`postId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_repliedTo` ON `messages` (`repliedTo`)")
            }
        }

        val migration4to5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_post_userId` ON `post` (`userId`)")
            }
        }
    }
}

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromUser(user: User): String = json.encodeToString(user)

    @TypeConverter
    fun toUser(jsonString: String): User = json.decodeFromString(jsonString)

    @TypeConverter
    fun fromList(tags: List<String>?): String? = tags?.let { json.encodeToString(it) }

    @TypeConverter
    fun toList(jsonString: String?): List<String>? = jsonString?.let { json.decodeFromString(it) }

    @TypeConverter
    fun fromInstant(value: Instant?): String? {
        return value?.toString()
    }

    @TypeConverter
    fun toInstant(value: String?): Instant? {
        return value?.let { Instant.parse(it) }
    }
}