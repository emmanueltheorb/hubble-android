package com.orb.hubble.repository

import com.orb.hubble.data.Page
import com.orb.hubble.data.Post
import com.orb.hubble.data.PostFilterTab
import com.orb.hubble.data.Status
import com.orb.hubble.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.uuid.ExperimentalUuidApi

@Singleton
class FakePostRepository @Inject constructor(
    private val userRepository: UserRepository,
    @param:ApplicationScope private val externalScope: CoroutineScope
) {

    init {
        setHomeScreenDeps()
    }

    private val _homeScreenDeps = MutableStateFlow(HomeScreenDeps())
    private val _selectedTab = MutableStateFlow(PostFilterTab.ALL)
    val selectedTab = _selectedTab.asStateFlow()

    // --- Hardcoded In-Memory Mock Data ---
    private val now = Clock.System.now()

    private val mockPosts = listOf(
        // 1. My Personal Page Post (will appear under myPosts)
        Post(
            id = "post-my-1",
            contentUrl = "",
            thumbnailUrl = "",
            thumbnailAt = "1",
            thumbnailString = "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",
            description = "This is a post on my own page!",
            type = "video",
            tags = listOf("dev", "testing"),
            expiresAt = now.plus(24.hours),
            pageId = "25ff15a5-4728-42b4-a2e9-171662b949f5", // Matches default HomeScreenDeps.pageId
            insertedAt = now.minus(1.hours),
            viewed = false,
            page = Page(
                id = "25ff15a5-4728-42b4-a2e9-171662b949f5",
                name = "Valkyrie",
                color = "#FF0000",
                phoneNumber = "123456789",
                ownerId = "f0c7cfbc-604b-437e-bcce-2f6e17e063b7" // Matches default HomeScreenDeps.userId
            )
        ),
        // 2. Followed Page Post (will appear under FOLLOWING tab)
        Post(
            id = "post-followed-1",
            contentUrl = "",
            thumbnailUrl = "",
            thumbnailAt = "1",
            thumbnailString = "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",
            description = "A clean updates update from a page you follow.",
            type = "video",
            tags = emptyList(),
            expiresAt = now.plus(24.hours),
            pageId = "followed-page-id-abc",
            insertedAt = now.minus(2.hours),
            viewed = false,
            page = Page(
                id = "followed-page-id-abc",
                name = "Thuggerboy",
                color = "#00FF00",
                phoneNumber = "987654321",
                ownerId = "some-other-user-1"
            )
        ),
        // 3. Contact Post (will appear under CONTACTS tab)
        Post(
            id = "post-contact-1",
            contentUrl = "",
            thumbnailUrl = "",
            thumbnailAt = "1",
            thumbnailString = "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",
            description = "Hey look, one of your phone contacts posted something!",
            type = "image",
            tags = emptyList(),
            expiresAt = now.plus(24.hours),
            pageId = "contact-page-id-xyz",
            insertedAt = now.minus(3.hours),
            viewed = false,
            page = Page(
                id = "contact-page-id-xyz",
                name = "Da Prince Wunna",
                color = "#0000FF",
                phoneNumber = "555-1234",
                ownerId = "contact-user-id-123"
            )
        ),
        // 4. An already viewed post (to verify viewed filter states)
        Post(
            id = "post-viewed-1",
            contentUrl = null,
            contentText = "Yami",
            thumbnailUrl = null,
            thumbnailAt = "1",
            thumbnailString = "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",
            description = "You've already seen this post.",
            type = "TEXT",
            tags = emptyList(),
            expiresAt = now.plus(24.hours),
            pageId = "followed-page-id-abc",
            insertedAt = now.minus(4.hours),
            viewed = true, // Viewed state
            page = Page(
                id = "followed-page-id-abc",
                name = "Kotlin Weekly",
                color = "#00FF00",
                phoneNumber = "987654321",
                ownerId = "some-other-user-1"
            )
        )
    )

    // Replaced database call with MutableStateFlow using our clean list
    val postsFromDb: StateFlow<List<Post>> = MutableStateFlow(mockPosts)
        .asStateFlow()

    // --- State Combining Logic (Kept exactly as original) ---

    val myPosts: StateFlow<Status?> =
        combine(postsFromDb, _homeScreenDeps) { posts, deps ->
            val postList = posts.groupBy { it.pageId }[deps.pageId] ?: emptyList()

            if (postList.isNotEmpty()) {
                Status(
                    pageId = deps.pageId,
                    ownerId = deps.userId,
                    username = postList.last().page.name,
                    profilePic = "",
                    posts = postList
                )
            } else {
                null
            }
        }.stateIn(externalScope, SharingStarted.WhileSubscribed(5000), null)

    val homeScreenPostState: StateFlow<HomeScreenPostState> =
        combine(postsFromDb, _homeScreenDeps, _selectedTab) { posts, deps, tab ->
            val statuses = posts.groupBy { it.pageId }
                .map { (pageId, posts) ->
                    Status(
                        pageId = pageId,
                        ownerId = posts.first().page.ownerId,
                        posts = posts,
                        username = posts.first().page.name,
                        profilePic = ""
                    )
                }

            HomeScreenPostState(
                allTabRecentPosts = statuses
                    .filter { it.pageId != deps.pageId && !it.seenAll }
                    .sortedByDescending { it.lastestUpdate },
                allTabViewedPosts = statuses
                    .filter { it.pageId != deps.pageId && it.seenAll }
                    .sortedByDescending { it.lastestUpdate },
                followingTabRecentPosts = statuses
                    .filter { it.pageId in deps.followedPagesIds && it.pageId != deps.pageId && !it.seenAll }
                    .sortedByDescending { it.lastestUpdate },
                followingTabViewedPosts = statuses
                    .filter { it.pageId in deps.followedPagesIds && it.pageId != deps.pageId && it.seenAll }
                    .sortedByDescending { it.lastestUpdate },
                contactsTabRecentPosts = statuses
                    .filter { it.ownerId in deps.contactsUserIds && it.pageId != deps.pageId && !it.seenAll }
                    .sortedByDescending { it.lastestUpdate },
                contactsTabViewedPosts = statuses
                    .filter { it.ownerId in deps.contactsUserIds && it.pageId != deps.pageId && it.seenAll }
                    .sortedByDescending { it.lastestUpdate }
            )
        }
            .flowOn(Dispatchers.Default)
            .stateIn(externalScope, SharingStarted.WhileSubscribed(5000), HomeScreenPostState())

    val homeScreenStatuses: StateFlow<List<Status>> =
        combine(homeScreenPostState, _selectedTab) { state, tab ->
            when (tab) {
                PostFilterTab.ALL -> state.allTabRecentPosts + state.allTabViewedPosts
                PostFilterTab.FOLLOWING -> state.followingTabRecentPosts + state.followingTabViewedPosts
                PostFilterTab.CONTACTS -> state.contactsTabRecentPosts + state.contactsTabViewedPosts
            }
        }.stateIn(externalScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSelectedTab(tab: PostFilterTab) {
        _selectedTab.value = tab
    }

    fun setHomeScreenDeps() {
        // We intercept the network/DB observations from userRepository and enforce IDs matching mock data
        userRepository.followedPages
            .onEach { followedPages ->
                if (followedPages.isNotEmpty()) {
                    _homeScreenDeps.update { state ->
                        state.copy(
                            // Add mock ID dynamically along with existing database mappings
                            followedPagesIds = setOf("followed-page-id-abc")
                        )
                    }
                }
            }
            .launchIn(externalScope)

        userRepository.contacts
            .onEach { contacts ->
                if (contacts.isNotEmpty()) {
                    _homeScreenDeps.update { state ->
                        state.copy(
                            // Add mock ID dynamically along with existing database mappings
                            contactsUserIds = setOf("contact-user-id-123")
                        )
                    }
                }
            }
            .launchIn(externalScope)
    }
}