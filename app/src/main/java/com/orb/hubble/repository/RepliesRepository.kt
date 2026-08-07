package com.orb.hubble.repository

import com.orb.hubble.data.MessageLog
import com.orb.hubble.data.MessageWithPostAndReply
import com.orb.hubble.data.PostFilterTab
import com.orb.hubble.data.RepliesFilterTab
import com.orb.hubble.data.ThreadLink
import com.orb.hubble.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RepliesRepository @Inject constructor(
    private val repository: FakeHubbleRepository,
    private val userRepository: UserRepository,
    @param:ApplicationScope private val externalScope: CoroutineScope
) {
    private val userId = MutableStateFlow("")

    private val _selectedTab = MutableStateFlow(RepliesFilterTab.FOR_ME)
    val selectedTab = _selectedTab.asStateFlow()

    fun setSelectedTab(tab: RepliesFilterTab) {
        _selectedTab.value = tab
    }

    private val repliesForMe = repository.getReplyLogs(userId.value)
        .stateIn(
            externalScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val repliesMessagesState: StateFlow<RepliesScreenMessagesState> =
        combine(repliesForMe, _selectedTab) { repliesForMe, selectedTab ->

            val messagesForMe = repliesForMe.toMessageLogs()

            RepliesScreenMessagesState(
                forMeTabMessages = messagesForMe
            )
        }
            .flowOn(Dispatchers.Default)
            .stateIn(
                externalScope,
                SharingStarted.WhileSubscribed(5000),
                RepliesScreenMessagesState()
            )
}

data class RepliesScreenMessagesState(
    val forMeTabMessages: List<MessageLog> = emptyList(),
    val myPostsTabMessages: List<MessageLog> = emptyList(),
    val otherPostsTabMessages: List<MessageLog> = emptyList(),
    val forMeTabSeenMessages: List<MessageLog> = emptyList(),
    val myPostsTabSeenMessages: List<MessageLog> = emptyList(),
    val otherPostsTabSeenMessages: List<MessageLog> = emptyList()
)

/**
 * Pure mapping function that determines ThreadLink status for each item
 * based on its adjacent neighbors in the list.
 */
private fun List<MessageWithPostAndReply>.toMessageLogs(): List<MessageLog> {
    return mapIndexed { index, current ->
        val currentPostId = current.postSummary.id

        val prevHasSamePost = index > 0 && this[index - 1].postSummary.id == currentPostId
        val nextHasSamePost = index < lastIndex && this[index + 1].postSummary.id == currentPostId

        val threadLink = when {
            !prevHasSamePost && nextHasSamePost -> ThreadLink.BOTTOM // Thread start
            prevHasSamePost && nextHasSamePost  -> ThreadLink.FULL   // Thread middle
            prevHasSamePost && !nextHasSamePost -> ThreadLink.TOP    // Thread end
            else -> ThreadLink.NONE
        }

        MessageLog(
            message = current.message,
            repliedMessage = current.repliedMessage,
            postSummary = current.postSummary,
            threadLink = threadLink
        )
    }
}