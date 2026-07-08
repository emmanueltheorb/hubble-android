package com.orb.hubble.navigation

import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.orb.hubble.R
import kotlinx.serialization.Serializable

sealed class SubGraph {

    @Serializable
    data object Auth: SubGraph()

    @Serializable
    data object Home: SubGraph()
}

@Serializable
sealed class Dest {

    @Serializable
    object MainTabsScreen: Dest()

    @Serializable
    data object HomeScreen: Dest()

    @Serializable
    data class ViewStatusScreen(
        val pageId: String? = null,
        val messageId: String? = null,
        val postId: String? = null
    ): Dest()

    @Serializable
    data class PostViewerScreen(
        val pageId: String? = null,
        val postId: String? = null
    ): Dest()

    @Serializable
    data object PickMediaScreen: Dest()

    @Serializable
    data object ReplyLogScreen: Dest()

    @Serializable
    data object ExploreScreen: Dest()

    @Serializable
    data object ProfileScreen: Dest()

    @Serializable
    data class CreatePostScreen(
        val mediaUri: String
    ): Dest()
}