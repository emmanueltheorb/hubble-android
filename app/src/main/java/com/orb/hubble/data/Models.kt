package com.orb.hubble.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
data class Post @OptIn(ExperimentalTime::class) constructor(
    val id: Int,
    val contentUrl: String?,
    val description: String?,
    val contentText: String? = null,
    val tags: List<String>?,
    @Serializable(with = InstantSerializer::class)
    val expiresAt: Instant,
    val userId: Int,
    @Serializable(with = InstantSerializer::class)
    val insertedAt: Instant,
    val viewed: Boolean = false,
    val user: User
)

@Serializable
data class User(
    val id: Int,
    val username: String,
    val color: String,
    val phoneNumber: String
)

@Serializable
data class Message @OptIn(ExperimentalTime::class) constructor(
    val id: Int,
    val content: String,
    val postId: Int,
    val repliedTo: Int?,
    val repliedMessage: Message?,
    @Serializable(with = InstantSerializer::class)
    val insertedAt: Instant,
    val user: User
)

@Serializable
data class Contact(
    @SerialName("phone_number") val phoneNumber: String,
    val name: String
)

@Serializable
data class CreatePostRequest(
    @SerialName("content_url") val contentUrl: String? = null,
    val description: String? = null,
    @SerialName("content_text") val contentText: String? = null,
    val tags: List<String>? = null
)

@Serializable
data class PostMessageRequest(
    @SerialName("user_id") val userId: Int,
    @SerialName("post_id") val postId: Int,
    val content: String,
    @SerialName("replied_to") val repliedTo: Int? = null
)

@Serializable
data class FollowRequest(
    val user1: Int,
    val user2: Int
)

sealed class WebsocketEvent {
    @Serializable
    data class NewPost(val post: Post) : WebsocketEvent()

    @Serializable
    data class MissedPost(val post: Post) : WebsocketEvent()

    @Serializable
    data class PostMessage(val message: Message) : WebsocketEvent()

    @Serializable
    data class Connected(val message: String) : WebsocketEvent()

    @Serializable
    data class Error(val error: String) : WebsocketEvent()

    @Serializable
    object Disconnected : WebsocketEvent()
}

sealed class ClientEvent {
    @Serializable
    data class SyncContact(@SerialName("contact_list") val contactList: List<Contact>) :
        ClientEvent() {
        @Serializable
        private data class Wrapper(
            val type: String = "sync_contact",
            @SerialName("contact_list") val contactList: List<Contact>
        )

        fun toJson(json: Json): String =
            json.encodeToString(Wrapper.serializer(), Wrapper(contactList = contactList))
    }

    @Serializable
    data class JoinPost @OptIn(ExperimentalTime::class) constructor(
        @SerialName("post_id") val postId: Int,
        @Serializable(with = InstantSerializer::class)
        val from: Instant?
    ) : ClientEvent() {
        @Serializable
        private data class Wrapper @OptIn(ExperimentalTime::class) constructor(
            val type: String = "join_post",
            @SerialName("post_id") val postId: Int,
            @Serializable(with = InstantSerializer::class)
            val from: Instant?
        )

        @OptIn(ExperimentalTime::class)
        fun toJson(json: Json): String =
            json.encodeToString(Wrapper.serializer(), Wrapper(postId = postId, from = from))
    }

    @Serializable
    data class CreatePost(val post: CreatePostRequest) : ClientEvent() {
        @Serializable
        private data class Wrapper(
            val type: String = "create_post",
            val post: CreatePostRequest
        )

        fun toJson(json: Json): String =
            json.encodeToString(Wrapper.serializer(), Wrapper(post = post))
    }

    @Serializable
    data class PostMessage(val message: PostMessageRequest) : ClientEvent() {
        @Serializable
        private data class Wrapper(
            val type: String = "post_message",
            val message: PostMessageRequest
        )

        fun toJson(json: Json): String =
            json.encodeToString(Wrapper.serializer(), Wrapper(message = message))
    }

    @Serializable
    data class FollowUser(val users: FollowRequest) : ClientEvent() {
        @Serializable
        private data class Wrapper(
            val type: String = "follow_user",
            val users: FollowRequest
        )

        fun toJson(json: Json): String =
            json.encodeToString(Wrapper.serializer(), Wrapper(users = users))
    }
}

@OptIn(ExperimentalTime::class)
object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    @OptIn(ExperimentalTime::class)
    override fun deserialize(decoder: Decoder): Instant {
        return Instant.parse(decoder.decodeString())
    }

    @OptIn(ExperimentalTime::class)
    override fun serialize(
        encoder: Encoder,
        value: Instant
    ) {
        encoder.encodeString(value.toString())
    }
}

// List of 10 posts
@OptIn(ExperimentalTime::class)
val postsList10 = listOf(
    Post(
        id = 1,
        contentUrl = "https://example.com/content1.jpg",
        description = "Beautiful sunset at the beach",
        contentText = "Enjoying the peaceful evening waves",
        tags = listOf("sunset", "beach", "vacation"),
        expiresAt = Instant.fromEpochSeconds(1735689600), // Dec 31, 2024
        userId = 101,
        insertedAt = Instant.fromEpochSeconds(1635689600),
        viewed = true,
        user = User(101, "sunset_lover", "#FF6B6B", "+1234567890")
    ),
    Post(
        id = 2,
        contentUrl = "https://example.com/content2.jpg",
        description = "Morning coffee routine",
        contentText = "Best way to start the day",
        tags = listOf("coffee", "morning", "routine"),
        expiresAt = Instant.fromEpochSeconds(1735776000), // Jan 1, 2025
        userId = 102,
        insertedAt = Instant.fromEpochSeconds(1635776000),
        viewed = false,
        user = User(102, "coffee_master", "#4ECDC4", "+1234567891")
    ),
    Post(
        id = 3,
        contentUrl = "https://example.com/content3.mp4",
        description = "New recipe experiment",
        contentText = "Trying out vegan pasta carbonara",
        tags = listOf("cooking", "vegan", "recipe"),
        expiresAt = Instant.fromEpochSeconds(1735862400), // Jan 2, 2025
        userId = 103,
        insertedAt = Instant.fromEpochSeconds(1635862400),
        viewed = true,
        user = User(103, "chef_extraordinaire", "#45B7D1", "+1234567892")
    ),
    Post(
        id = 4,
        contentUrl = null,
        description = "Thoughts on productivity",
        contentText = "Deep work vs shallow work strategies",
        tags = listOf("productivity", "work", "focus"),
        expiresAt = Instant.fromEpochSeconds(1735948800), // Jan 3, 2025
        userId = 104,
        insertedAt = Instant.fromEpochSeconds(1635948800),
        viewed = false,
        user = User(104, "productive_mind", "#96CEB4", "+1234567893")
    ),
    Post(
        id = 5,
        contentUrl = "https://example.com/content5.jpg",
        description = "Weekend hiking adventure",
        contentText = "Reached the mountain peak today!",
        tags = listOf("hiking", "adventure", "nature"),
        expiresAt = Instant.fromEpochSeconds(1736035200), // Jan 4, 2025
        userId = 105,
        insertedAt = Instant.fromEpochSeconds(1636035200),
        viewed = false,
        user = User(105, "mountain_explorer", "#FECA57", "+1234567894")
    ),
    Post(
        id = 6,
        contentUrl = "https://example.com/content6.jpg",
        description = "New book acquisition",
        contentText = "Just got this amazing sci-fi novel",
        tags = listOf("books", "reading", "sci-fi"),
        expiresAt = Instant.fromEpochSeconds(1736121600), // Jan 5, 2025
        userId = 106,
        insertedAt = Instant.fromEpochSeconds(1636121600),
        viewed = true,
        user = User(106, "book_worm", "#FF9FF3", "+1234567895")
    ),
    Post(
        id = 7,
        contentUrl = null,
        description = "Programming insights",
        contentText = "Refactoring legacy code successfully",
        tags = listOf("programming", "coding", "refactoring"),
        expiresAt = Instant.fromEpochSeconds(1736208000), // Jan 6, 2025
        userId = 107,
        insertedAt = Instant.fromEpochSeconds(1636208000),
        viewed = false,
        user = User(107, "code_artist", "#54A0FF", "+1234567896")
    ),
    Post(
        id = 8,
        contentUrl = "https://example.com/content8.mp4",
        description = "Music practice session",
        contentText = "Learning the new guitar solo",
        tags = listOf("music", "guitar", "practice"),
        expiresAt = Instant.fromEpochSeconds(1736294400), // Jan 7, 2025
        userId = 108,
        insertedAt = Instant.fromEpochSeconds(1636294400),
        viewed = true,
        user = User(108, "guitar_hero", "#5F27CD", "+1234567897")
    ),
    Post(
        id = 9,
        contentUrl = "https://example.com/content9.jpg",
        description = "Art exhibition visit",
        contentText = "Impressed by the modern art collection",
        tags = listOf("art", "exhibition", "culture"),
        expiresAt = Instant.fromEpochSeconds(1736380800), // Jan 8, 2025
        userId = 109,
        insertedAt = Instant.fromEpochSeconds(1636380800),
        viewed = false,
        user = User(109, "art_lover", "#FF9F43", "+1234567898")
    ),
    Post(
        id = 10,
        contentUrl = "https://example.com/content10.jpg",
        description = "Family gathering memories",
        contentText = "Great time with everyone together",
        tags = listOf("family", "memories", "gathering"),
        expiresAt = Instant.fromEpochSeconds(1736467200), // Jan 9, 2025
        userId = 110,
        insertedAt = Instant.fromEpochSeconds(1636467200),
        viewed = true,
        user = User(110, "family_first", "#00D2D3", "+1234567899")
    )
)

// List of 4 posts
@OptIn(ExperimentalTime::class)
val postsList4 = listOf(
    Post(
        id = 11,
        contentUrl = "https://example.com/small1.jpg",
        description = "Quick morning workout",
        contentText = "15 minute routine to start the day",
        tags = listOf("fitness", "workout", "health"),
        expiresAt = Instant.fromEpochSeconds(1736553600), // Jan 10, 2025
        userId = 201,
        insertedAt = Instant.fromEpochSeconds(1636553600),
        viewed = false,
        user = User(201, "fitness_guru", "#FF6B6B", "+1234567800")
    ),
    Post(
        id = 12,
        contentUrl = null,
        description = "Meditation practice",
        contentText = "10 minutes of mindfulness meditation",
        tags = listOf("meditation", "mindfulness", "peace"),
        expiresAt = Instant.fromEpochSeconds(1736640000), // Jan 11, 2025
        userId = 202,
        insertedAt = Instant.fromEpochSeconds(1636640000),
        viewed = false,
        user = User(202, "zen_master", "#A55EEA", "+1234567801")
    ),
    Post(
        id = 13,
        contentUrl = "https://example.com/small3.mp4",
        description = "New plant in the collection",
        contentText = "Added a beautiful monstera plant",
        tags = listOf("plants", "gardening", "home"),
        expiresAt = Instant.fromEpochSeconds(1736726400), // Jan 12, 2025
        userId = 203,
        insertedAt = Instant.fromEpochSeconds(1636726400),
        viewed = false,
        user = User(203, "plant_parent", "#26DE81", "+1234567802")
    )
)

@OptIn(ExperimentalTime::class)
val messageList = listOf(
    Message(
        id = 0,
        content = "i mean i want sm1 1-2 years older than me",
        postId = 1,
        repliedTo = null,
        repliedMessage = null,
        insertedAt = Instant.fromEpochSeconds(1636726400),
        user = User(1, "3cstacy", "#26DE81", "+1234567802")
    ),
    Message(
        id = 1,
        content = "I just like food in general",
        postId = 1,
        repliedTo = 0,
        repliedMessage = Message(
            id = 0,
            content = "i mean i want sm1 1-2 years older than me",
            postId = 1,
            repliedTo = null,
            repliedMessage = null,
            insertedAt = Instant.fromEpochSeconds(1636726400),
            user = User(1, "3cstacy", "#26DE81", "+1234567802")
        ),
        insertedAt = Instant.fromEpochSeconds(1636726400),
        user = User(1, "7th prince", "#26DEBD", "+1234567802")
    ),
    Message(
        id = 2,
        content = "i mean i want sm1 1-2 years older than me",
        postId = 1,
        repliedTo = null,
        repliedMessage = null,
        insertedAt = Instant.fromEpochSeconds(1636726400),
        user = User(1, "3cstacy", "#26DE81", "+1234567802")
    ),
    Message(
        id = 3,
        content = "oh hi robotic LMAOAO",
        postId = 1,
        repliedTo = null,
        repliedMessage = null,
        insertedAt = Instant.fromEpochSeconds(1636726400),
        user = User(1, "shoo ree I make agent cats", "#DEB526", "+1234567802")
    ),
    Message(
        id = 4,
        content = "i mean i want sm1 1-2 years older than me",
        postId = 1,
        repliedTo = null,
        repliedMessage = null,
        insertedAt = Instant.fromEpochSeconds(1636726400),
        user = User(1, "3cstacy", "#26DE81", "+1234567802")
    ),
    Message(
        id = 5,
        content = "sure do hope this isn't the hey im 30yo female, looking for other female user that was removed days ago",
        postId = 1,
        repliedTo = null,
        repliedMessage = null,
        insertedAt = Instant.fromEpochSeconds(1636726400),
        user = User(1, "Azakiehl", "#DE267E", "+1234567802")
    ),
    Message(
        id = 6,
        content = "i do to minus the older part",
        postId = 1,
        repliedTo = null,
        repliedMessage = null,
        insertedAt = Instant.fromEpochSeconds(1636726400),
        user = User(1, "7th prince", "#26DEBD", "+1234567802")
    ),
    Message(
        id = 7,
        content = "i mean i want sm1 1-2 years older than me",
        postId = 1,
        repliedTo = null,
        repliedMessage = null,
        insertedAt = Instant.fromEpochSeconds(1636726400),
        user = User(1, "3cstacy", "#26DE81", "+1234567802")
    ),
    Message(
        id = 8,
        content = "idk i use jbl",
        postId = 1,
        repliedTo = null,
        repliedMessage = null,
        insertedAt = Instant.fromEpochSeconds(1636726400),
        user = User(1, "3cstacy", "#26DE81", "+1234567802")
    ),
    Message(
        id = 9,
        content = "Which one",
        postId = 1,
        repliedTo = null,
        repliedMessage = null,
        insertedAt = Instant.fromEpochSeconds(1636726400),
        user = User(1, "bruh", "#8126DE", "+1234567802")
    ),
    Message(
        id = 10,
        content = "ight i thought you meant like 40 years older",
        postId = 1,
        repliedTo = null,
        repliedMessage = null,
        insertedAt = Instant.fromEpochSeconds(1636726400),
        user = User(1, "7th prince", "#26DEBD", "+1234567802")
    )
)

@OptIn(ExperimentalTime::class)
val mockPosts: Map<Int, List<Post>> = mapOf(
    101 to listOf(
        Post(
            id = 1,
            contentUrl = "https://example.com/content1.jpg",
            description = "Beautiful sunset at the beach",
            contentText = "Enjoying the peaceful evening waves",
            tags = listOf("sunset", "beach", "vacation"),
            expiresAt = Instant.fromEpochSeconds(1735689600),
            userId = 101,
            insertedAt = Instant.fromEpochSeconds(1635689600),
            viewed = false,
            user = User(101, "sunset_lover", "#FF6B6B", "+1234567890")
        ),
        Post(
            id = 11,
            contentUrl = "https://example.com/sunrise1.jpg",
            description = "Early morning sunrise",
            contentText = "Waking up to this magnificent view",
            tags = listOf("sunrise", "morning", "nature"),
            expiresAt = Instant.fromEpochSeconds(1735776000),
            userId = 101,
            insertedAt = Instant.fromEpochSeconds(1635776000),
            viewed = false,
            user = User(101, "sunset_lover", "#FF6B6B", "+1234567890")
        ),
        Post(
            id = 12,
            contentUrl = "https://example.com/mountain_view.jpg",
            description = "Mountain hiking adventure",
            contentText = "Conquered the highest peak in the region!",
            tags = listOf("hiking", "mountains", "adventure"),
            expiresAt = Instant.fromEpochSeconds(1735862400),
            userId = 101,
            insertedAt = Instant.fromEpochSeconds(1635862400),
            viewed = false,
            user = User(101, "sunset_lover", "#FF6B6B", "+1234567890")
        ),
        Post(
            id = 13,
            contentUrl = null,
            description = "Reflections on life",
            contentText = "Sometimes the simplest moments are the most beautiful",
            tags = listOf("reflection", "life", "thoughts"),
            expiresAt = Instant.fromEpochSeconds(1735948800),
            userId = 101,
            insertedAt = Instant.fromEpochSeconds(1635948800),
            viewed = false,
            user = User(101, "sunset_lover", "#FF6B6B", "+1234567890")
        )
    ),
    102 to listOf(
        Post(
            id = 2,
            contentUrl = "https://example.com/content2.jpg",
            description = "Morning coffee routine",
            contentText = "Best way to start the day",
            tags = listOf("coffee", "morning", "routine"),
            expiresAt = Instant.fromEpochSeconds(1735776000),
            userId = 102,
            insertedAt = Instant.fromEpochSeconds(1635776000),
            viewed = false,
            user = User(102, "coffee_master", "#4ECDC4", "+1234567891")
        ),
        Post(
            id = 14,
            contentUrl = "https://example.com/latte_art.jpg",
            description = "New latte art technique",
            contentText = "Perfecting my heart-shaped latte art",
            tags = listOf("coffee", "latte", "art"),
            expiresAt = Instant.fromEpochSeconds(1736035200),
            userId = 102,
            insertedAt = Instant.fromEpochSeconds(1636035200),
            viewed = true,
            user = User(102, "coffee_master", "#4ECDC4", "+1234567891")
        ),
        Post(
            id = 15,
            contentUrl = "https://example.com/coffee_beans.jpg",
            description = "Fresh coffee beans from Ethiopia",
            contentText = "The aroma is absolutely incredible",
            tags = listOf("coffee", "beans", "ethiopia"),
            expiresAt = Instant.fromEpochSeconds(1736121600),
            userId = 102,
            insertedAt = Instant.fromEpochSeconds(1636121600),
            viewed = false,
            user = User(102, "coffee_master", "#4ECDC4", "+1234567891")
        ),
        Post(
            id = 16,
            contentUrl = null,
            description = "Coffee brewing methods",
            contentText = "Comparing French press vs pour over techniques",
            tags = listOf("coffee", "brewing", "methods"),
            expiresAt = Instant.fromEpochSeconds(1736208000),
            userId = 102,
            insertedAt = Instant.fromEpochSeconds(1636208000),
            viewed = true,
            user = User(102, "coffee_master", "#4ECDC4", "+1234567891")
        )
    ),
    103 to listOf(
        Post(
            id = 3,
            contentUrl = "https://example.com/content3.mp4",
            description = "New recipe experiment",
            contentText = "Trying out vegan pasta carbonara",
            tags = listOf("cooking", "vegan", "recipe"),
            expiresAt = Instant.fromEpochSeconds(1735862400),
            userId = 103,
            insertedAt = Instant.fromEpochSeconds(1635862400),
            viewed = true,
            user = User(103, "chef_extraordinaire", "#45B7D1", "+1234567892")
        ),
        Post(
            id = 17,
            contentUrl = "https://example.com/baking.jpg",
            description = "Sourdough bread success",
            contentText = "Finally got the perfect crust!",
            tags = listOf("baking", "bread", "sourdough"),
            expiresAt = Instant.fromEpochSeconds(1736294400),
            userId = 103,
            insertedAt = Instant.fromEpochSeconds(1636294400),
            viewed = false,
            user = User(103, "chef_extraordinaire", "#45B7D1", "+1234567892")
        ),
        Post(
            id = 18,
            contentUrl = "https://example.com/dessert.mp4",
            description = "Chocolate lava cake tutorial",
            contentText = "Step by step guide to perfect molten chocolate cake",
            tags = listOf("dessert", "chocolate", "baking"),
            expiresAt = Instant.fromEpochSeconds(1736380800),
            userId = 103,
            insertedAt = Instant.fromEpochSeconds(1636380800),
            viewed = true,
            user = User(103, "chef_extraordinaire", "#45B7D1", "+1234567892")
        )
    ),
    104 to listOf(
        Post(
            id = 4,
            contentUrl = null,
            description = "Thoughts on productivity",
            contentText = "Deep work vs shallow work strategies",
            tags = listOf("productivity", "work", "focus"),
            expiresAt = Instant.fromEpochSeconds(1735948800),
            userId = 104,
            insertedAt = Instant.fromEpochSeconds(1635948800),
            viewed = false,
            user = User(104, "productive_mind", "#96CEB4", "+1234567893")
        ),
        Post(
            id = 19,
            contentUrl = "https://example.com/workspace.jpg",
            description = "My productive workspace",
            contentText = "Minimalist setup for maximum focus",
            tags = listOf("productivity", "workspace", "minimalism"),
            expiresAt = Instant.fromEpochSeconds(1736467200),
            userId = 104,
            insertedAt = Instant.fromEpochSeconds(1636467200),
            viewed = true,
            user = User(104, "productive_mind", "#96CEB4", "+1234567893")
        ),
        Post(
            id = 20,
            contentUrl = null,
            description = "Time management techniques",
            contentText = "Pomodoro technique changed my life",
            tags = listOf("productivity", "time", "management"),
            expiresAt = Instant.fromEpochSeconds(1736553600),
            userId = 104,
            insertedAt = Instant.fromEpochSeconds(1636553600),
            viewed = false,
            user = User(104, "productive_mind", "#96CEB4", "+1234567893")
        )
    ),
    105 to listOf(
        Post(
            id = 5,
            contentUrl = "https://example.com/content5.jpg",
            description = "Weekend hiking adventure",
            contentText = "Reached the mountain peak today!",
            tags = listOf("hiking", "adventure", "nature"),
            expiresAt = Instant.fromEpochSeconds(1736035200),
            userId = 105,
            insertedAt = Instant.fromEpochSeconds(1636035200),
            viewed = false,
            user = User(105, "mountain_explorer", "#FECA57", "+1234567894")
        ),
        Post(
            id = 21,
            contentUrl = "https://example.com/camping.jpg",
            description = "Night under the stars",
            contentText = "Camping in the wilderness with amazing views",
            tags = listOf("camping", "stars", "wilderness"),
            expiresAt = Instant.fromEpochSeconds(1736640000),
            userId = 105,
            insertedAt = Instant.fromEpochSeconds(1636640000),
            viewed = true,
            user = User(105, "mountain_explorer", "#FECA57", "+1234567894")
        ),
        Post(
            id = 22,
            contentUrl = "https://example.com/waterfall.mp4",
            description = "Hidden waterfall discovered",
            contentText = "Found this beautiful hidden gem during today's hike",
            tags = listOf("hiking", "waterfall", "discovery"),
            expiresAt = Instant.fromEpochSeconds(1736726400),
            userId = 105,
            insertedAt = Instant.fromEpochSeconds(1636726400),
            viewed = false,
            user = User(105, "mountain_explorer", "#FECA57", "+1234567894")
        ),
        Post(
            id = 23,
            contentUrl = null,
            description = "Hiking gear recommendations",
            contentText = "My essential gear for multi-day hikes",
            tags = listOf("hiking", "gear", "recommendations"),
            expiresAt = Instant.fromEpochSeconds(1736812800),
            userId = 105,
            insertedAt = Instant.fromEpochSeconds(1636812800),
            viewed = true,
            user = User(105, "mountain_explorer", "#FECA57", "+1234567894")
        )
    ),
    106 to listOf(
        Post(
            id = 6,
            contentUrl = "https://example.com/content6.jpg",
            description = "New book acquisition",
            contentText = "Just got this amazing sci-fi novel",
            tags = listOf("books", "reading", "sci-fi"),
            expiresAt = Instant.fromEpochSeconds(1736121600),
            userId = 106,
            insertedAt = Instant.fromEpochSeconds(1636121600),
            viewed = true,
            user = User(106, "book_worm", "#FF9FF3", "+1234567895")
        ),
        Post(
            id = 24,
            contentUrl = "https://example.com/library.jpg",
            description = "My personal library",
            contentText = "Finally organized my book collection",
            tags = listOf("books", "library", "collection"),
            expiresAt = Instant.fromEpochSeconds(1736899200),
            userId = 106,
            insertedAt = Instant.fromEpochSeconds(1636899200),
            viewed = false,
            user = User(106, "book_worm", "#FF9FF3", "+1234567895")
        ),
        Post(
            id = 25,
            contentUrl = null,
            description = "Book review: Dune",
            contentText = "Frank Herbert's masterpiece still holds up",
            tags = listOf("books", "review", "sci-fi"),
            expiresAt = Instant.fromEpochSeconds(1736985600),
            userId = 106,
            insertedAt = Instant.fromEpochSeconds(1636985600),
            viewed = true,
            user = User(106, "book_worm", "#FF9FF3", "+1234567895")
        )
    ),
    107 to listOf(
        Post(
            id = 7,
            contentUrl = null,
            description = "Programming insights",
            contentText = "Refactoring legacy code successfully",
            tags = listOf("programming", "coding", "refactoring"),
            expiresAt = Instant.fromEpochSeconds(1736208000),
            userId = 107,
            insertedAt = Instant.fromEpochSeconds(1636208000),
            viewed = false,
            user = User(107, "code_artist", "#54A0FF", "+1234567896")
        ),
        Post(
            id = 26,
            contentUrl = "https://example.com/code_snippet.jpg",
            description = "Elegant algorithm solution",
            contentText = "Solved this complex problem with just a few lines",
            tags = listOf("programming", "algorithm", "solution"),
            expiresAt = Instant.fromEpochSeconds(1737072000),
            userId = 107,
            insertedAt = Instant.fromEpochSeconds(1637072000),
            viewed = true,
            user = User(107, "code_artist", "#54A0FF", "+1234567896")
        ),
        Post(
            id = 27,
            contentUrl = null,
            description = "New framework learned",
            contentText = "Just completed a project with the new framework",
            tags = listOf("programming", "framework", "learning"),
            expiresAt = Instant.fromEpochSeconds(1737158400),
            userId = 107,
            insertedAt = Instant.fromEpochSeconds(1637158400),
            viewed = false,
            user = User(107, "code_artist", "#54A0FF", "+1234567896")
        ),
        Post(
            id = 28,
            contentUrl = "https://example.com/team_coding.mp4",
            description = "Team coding session",
            contentText = "Great collaboration with the team today",
            tags = listOf("programming", "teamwork", "collaboration"),
            expiresAt = Instant.fromEpochSeconds(1737244800),
            userId = 107,
            insertedAt = Instant.fromEpochSeconds(1637244800),
            viewed = true,
            user = User(107, "code_artist", "#54A0FF", "+1234567896")
        )
    ),
    // New users added below
    108 to listOf(
        Post(
            id = 29,
            contentUrl = "https://example.com/guitar_solo.mp4",
            description = "New guitar solo mastered",
            contentText = "Finally nailed that complex solo I've been practicing for weeks",
            tags = listOf("music", "guitar", "practice"),
            expiresAt = Instant.fromEpochSeconds(1737331200),
            userId = 108,
            insertedAt = Instant.fromEpochSeconds(1637331200),
            viewed = true,
            user = User(108, "guitar_hero", "#5F27CD", "+1234567897")
        ),
        Post(
            id = 30,
            contentUrl = "https://example.com/concert.jpg",
            description = "Live performance tonight",
            contentText = "Amazing energy from the crowd at our gig",
            tags = listOf("music", "concert", "live"),
            expiresAt = Instant.fromEpochSeconds(1737417600),
            userId = 108,
            insertedAt = Instant.fromEpochSeconds(1637417600),
            viewed = false,
            user = User(108, "guitar_hero", "#5F27CD", "+1234567897")
        ),
        Post(
            id = 31,
            contentUrl = "https://example.com/new_guitar.jpg",
            description = "New guitar day!",
            contentText = "Just got this beautiful new electric guitar",
            tags = listOf("music", "guitar", "new"),
            expiresAt = Instant.fromEpochSeconds(1737504000),
            userId = 108,
            insertedAt = Instant.fromEpochSeconds(1637504000),
            viewed = true,
            user = User(108, "guitar_hero", "#5F27CD", "+1234567897")
        ),
        Post(
            id = 32,
            contentUrl = null,
            description = "Music theory insights",
            contentText = "Exploring modal interchange in modern music",
            tags = listOf("music", "theory", "learning"),
            expiresAt = Instant.fromEpochSeconds(1737590400),
            userId = 108,
            insertedAt = Instant.fromEpochSeconds(1637590400),
            viewed = false,
            user = User(108, "guitar_hero", "#5F27CD", "+1234567897")
        )
    ),
    109 to listOf(
        Post(
            id = 33,
            contentUrl = "https://example.com/art_gallery.jpg",
            description = "Modern art exhibition",
            contentText = "Inspired by the contemporary pieces at the gallery today",
            tags = listOf("art", "exhibition", "modern"),
            expiresAt = Instant.fromEpochSeconds(1737676800),
            userId = 109,
            insertedAt = Instant.fromEpochSeconds(1637676800),
            viewed = false,
            user = User(109, "art_lover", "#FF9F43", "+1234567898")
        ),
        Post(
            id = 34,
            contentUrl = "https://example.com/my_painting.jpg",
            description = "My latest painting",
            contentText = "Just finished this abstract piece inspired by nature",
            tags = listOf("art", "painting", "abstract"),
            expiresAt = Instant.fromEpochSeconds(1737763200),
            userId = 109,
            insertedAt = Instant.fromEpochSeconds(1637763200),
            viewed = true,
            user = User(109, "art_lover", "#FF9F43", "+1234567898")
        ),
        Post(
            id = 35,
            contentUrl = "https://example.com/art_supplies.jpg",
            description = "New art supplies haul",
            contentText = "Excited to try out these new high-quality paints and brushes",
            tags = listOf("art", "supplies", "painting"),
            expiresAt = Instant.fromEpochSeconds(1737849600),
            userId = 109,
            insertedAt = Instant.fromEpochSeconds(1637849600),
            viewed = false,
            user = User(109, "art_lover", "#FF9F43", "+1234567898")
        )
    ),
    110 to listOf(
        Post(
            id = 36,
            contentUrl = "https://example.com/family_dinner.jpg",
            description = "Family dinner night",
            contentText = "Nothing beats quality time with loved ones over good food",
            tags = listOf("family", "dinner", "together"),
            expiresAt = Instant.fromEpochSeconds(1737936000),
            userId = 110,
            insertedAt = Instant.fromEpochSeconds(1637936000),
            viewed = true,
            user = User(110, "family_first", "#00D2D3", "+1234567899")
        ),
        Post(
            id = 37,
            contentUrl = "https://example.com/family_vacation.mp4",
            description = "Family vacation memories",
            contentText = "Best family trip ever! The kids had so much fun",
            tags = listOf("family", "vacation", "memories"),
            expiresAt = Instant.fromEpochSeconds(1738022400),
            userId = 110,
            insertedAt = Instant.fromEpochSeconds(1638022400),
            viewed = false,
            user = User(110, "family_first", "#00D2D3", "+1234567899")
        ),
        Post(
            id = 38,
            contentUrl = null,
            description = "Family traditions",
            contentText = "Carrying on our family's holiday traditions this year",
            tags = listOf("family", "traditions", "holidays"),
            expiresAt = Instant.fromEpochSeconds(1738108800),
            userId = 110,
            insertedAt = Instant.fromEpochSeconds(1638108800),
            viewed = true,
            user = User(110, "family_first", "#00D2D3", "+1234567899")
        )
    ),
    111 to listOf(
        Post(
            id = 39,
            contentUrl = "https://example.com/fitness_workout.jpg",
            description = "Morning workout complete",
            contentText = "Crushed my personal best in deadlifts today!",
            tags = listOf("fitness", "workout", "gym"),
            expiresAt = Instant.fromEpochSeconds(1738195200),
            userId = 111,
            insertedAt = Instant.fromEpochSeconds(1638195200),
            viewed = false,
            user = User(111, "fitness_fanatic", "#FF6B9C", "+1234567900")
        ),
        Post(
            id = 40,
            contentUrl = "https://example.com/meal_prep.jpg",
            description = "Weekly meal prep",
            contentText = "Healthy eating made easy with proper planning",
            tags = listOf("fitness", "nutrition", "mealprep"),
            expiresAt = Instant.fromEpochSeconds(1738281600),
            userId = 111,
            insertedAt = Instant.fromEpochSeconds(1638281600),
            viewed = true,
            user = User(111, "fitness_fanatic", "#FF6B9C", "+1234567900")
        ),
        Post(
            id = 41,
            contentUrl = "https://example.com/running_trail.mp4",
            description = "Trail running adventure",
            contentText = "Beautiful 10k run through the forest trails this morning",
            tags = listOf("fitness", "running", "trails"),
            expiresAt = Instant.fromEpochSeconds(1738368000),
            userId = 111,
            insertedAt = Instant.fromEpochSeconds(1638368000),
            viewed = false,
            user = User(111, "fitness_fanatic", "#FF6B9C", "+1234567900")
        ),
        Post(
            id = 42,
            contentUrl = null,
            description = "Fitness journey reflections",
            contentText = "One year into my fitness journey and feeling amazing",
            tags = listOf("fitness", "journey", "progress"),
            expiresAt = Instant.fromEpochSeconds(1738454400),
            userId = 111,
            insertedAt = Instant.fromEpochSeconds(1638454400),
            viewed = true,
            user = User(111, "fitness_fanatic", "#FF6B9C", "+1234567900")
        )
    ),
    112 to listOf(
        Post(
            id = 43,
            contentUrl = "https://example.com/game_development.jpg",
            description = "New game prototype",
            contentText = "Just finished the core mechanics for my indie game",
            tags = listOf("gaming", "development", "indie"),
            expiresAt = Instant.fromEpochSeconds(1738540800),
            userId = 112,
            insertedAt = Instant.fromEpochSeconds(1638540800),
            viewed = false,
            user = User(112, "game_dev", "#A29BFE", "+1234567901")
        ),
        Post(
            id = 44,
            contentUrl = "https://example.com/gaming_setup.jpg",
            description = "Updated gaming setup",
            contentText = "New monitor and mechanical keyboard for the ultimate gaming experience",
            tags = listOf("gaming", "setup", "equipment"),
            expiresAt = Instant.fromEpochSeconds(1738627200),
            userId = 112,
            insertedAt = Instant.fromEpochSeconds(1638627200),
            viewed = true,
            user = User(112, "game_dev", "#A29BFE", "+1234567901")
        ),
        Post(
            id = 45,
            contentUrl = "https://example.com/game_review.mp4",
            description = "Latest game review",
            contentText = "My thoughts on the new RPG that just released",
            tags = listOf("gaming", "review", "rpg"),
            expiresAt = Instant.fromEpochSeconds(1738713600),
            userId = 112,
            insertedAt = Instant.fromEpochSeconds(1638713600),
            viewed = false,
            user = User(112, "game_dev", "#A29BFE", "+1234567901")
        )
    ),
    113 to listOf(
        Post(
            id = 46,
            contentUrl = "https://example.com/travel_destination.jpg",
            description = "Next travel destination",
            contentText = "Planning my next adventure to Southeast Asia",
            tags = listOf("travel", "adventure", "planning"),
            expiresAt = Instant.fromEpochSeconds(1738800000),
            userId = 113,
            insertedAt = Instant.fromEpochSeconds(1638800000),
            viewed = true,
            user = User(113, "wanderlust", "#55E6C1", "+1234567902")
        ),
        Post(
            id = 47,
            contentUrl = "https://example.com/travel_photos.jpg",
            description = "Travel photography",
            contentText = "Some of my favorite shots from last year's travels",
            tags = listOf("travel", "photography", "memories"),
            expiresAt = Instant.fromEpochSeconds(1738886400),
            userId = 113,
            insertedAt = Instant.fromEpochSeconds(1638886400),
            viewed = false,
            user = User(113, "wanderlust", "#55E6C1", "+1234567902")
        ),
        Post(
            id = 48,
            contentUrl = null,
            description = "Travel tips and tricks",
            contentText = "My essential packing list for long-term travel",
            tags = listOf("travel", "tips", "packing"),
            expiresAt = Instant.fromEpochSeconds(1738972800),
            userId = 113,
            insertedAt = Instant.fromEpochSeconds(1638972800),
            viewed = true,
            user = User(113, "wanderlust", "#55E6C1", "+1234567902")
        ),
        Post(
            id = 49,
            contentUrl = "https://example.com/cultural_experience.mp4",
            description = "Cultural immersion",
            contentText = "Learning so much from local traditions and customs",
            tags = listOf("travel", "culture", "learning"),
            expiresAt = Instant.fromEpochSeconds(1739059200),
            userId = 113,
            insertedAt = Instant.fromEpochSeconds(1639059200),
            viewed = false,
            user = User(113, "wanderlust", "#55E6C1", "+1234567902")
        )
    ),
    114 to listOf(
        Post(
            id = 50,
            contentUrl = "https://example.com/startup_office.jpg",
            description = "Startup life",
            contentText = "Our team working late to launch our new product",
            tags = listOf("startup", "entrepreneur", "business"),
            expiresAt = Instant.fromEpochSeconds(1739145600),
            userId = 114,
            insertedAt = Instant.fromEpochSeconds(1639145600),
            viewed = false,
            user = User(114, "entrepreneur", "#FD7272", "+1234567903")
        ),
        Post(
            id = 51,
            contentUrl = null,
            description = "Business insights",
            contentText = "Important lessons learned from our first year in business",
            tags = listOf("business", "lessons", "startup"),
            expiresAt = Instant.fromEpochSeconds(1739232000),
            userId = 114,
            insertedAt = Instant.fromEpochSeconds(1639232000),
            viewed = true,
            user = User(114, "entrepreneur", "#FD7272", "+1234567903")
        ),
        Post(
            id = 52,
            contentUrl = "https://example.com/networking_event.jpg",
            description = "Networking event",
            contentText = "Great connections made at today's entrepreneur meetup",
            tags = listOf("business", "networking", "entrepreneur"),
            expiresAt = Instant.fromEpochSeconds(1739318400),
            userId = 114,
            insertedAt = Instant.fromEpochSeconds(1639318400),
            viewed = false,
            user = User(114, "entrepreneur", "#FD7272", "+1234567903")
        )
    )
)