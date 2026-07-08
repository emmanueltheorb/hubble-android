package com.orb.hubble.repository

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
import kotlin.uuid.ExperimentalUuidApi

@Singleton
class PostRepository @Inject constructor(
    private val repository: HubbleRepository,
    private val userRepository: UserRepository,
    @param:ApplicationScope private val externalScope: CoroutineScope
) {

    init {
        setHomeScreenDeps()
    }

    private val _homeScreenDeps = MutableStateFlow(HomeScreenDeps())
    private val _selectedTab = MutableStateFlow(PostFilterTab.ALL)
    val selectedTab = _selectedTab.asStateFlow()

    val postsFromDb = repository.getAllPosts()
        .stateIn(
            externalScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val myPosts: StateFlow<List<Post>> =
        combine(postsFromDb, _homeScreenDeps) { posts, deps ->

            posts.groupBy { it.pageId }[deps.pageId] ?: emptyList()

        }.stateIn(externalScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
                    .filter { it.pageId != deps.pageId && !it.seenAll /* && (it.pageId in deps.followedPagesIds || it.ownerId in deps.contactsUserIds) */ }
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

    val homeScreenStatuses: StateFlow<List<Status>> = combine(homeScreenPostState, _selectedTab) { state, tab ->
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
        userRepository.followedPages
            .onEach { followedPages ->
                if (followedPages.isNotEmpty()) {
                    _homeScreenDeps.update { state ->
                        state.copy(followedPagesIds = followedPages.toSet())
                    }
                }
            }
            .launchIn(externalScope)

        userRepository.contacts
            .onEach { contacts ->
                if (contacts.isNotEmpty()) {
                    _homeScreenDeps.update { state ->
                        state.copy(contactsUserIds = contacts.toSet())
                    }
                }
            }
            .launchIn(externalScope)
    }
}

//Things home screen post state depends on. Deps -> Dependencies
data class HomeScreenDeps @OptIn(ExperimentalUuidApi::class) constructor(
    val userId: String = "f0c7cfbc-604b-437e-bcce-2f6e17e063b7",
    val pageId: String = "25ff15a5-4728-42b4-a2e9-171662b949f5",
    val followedPagesIds: Set<String> = emptySet(),
    val contactsUserIds: Set<String> = emptySet()
)

data class HomeScreenPostState(
    val allTabRecentPosts: List<Status> = emptyList(),
    val allTabViewedPosts: List<Status> = emptyList(),
    val followingTabRecentPosts: List<Status> = emptyList(),
    val followingTabViewedPosts: List<Status> = emptyList(),
    val contactsTabRecentPosts: List<Status> = emptyList(),
    val contactsTabViewedPosts: List<Status> = emptyList()
)