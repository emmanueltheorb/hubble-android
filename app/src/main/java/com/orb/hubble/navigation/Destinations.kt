package com.orb.hubble.navigation

import android.net.Uri
import kotlinx.serialization.Serializable

sealed class SubGraph {

    @Serializable
    data object Auth: SubGraph()

    @Serializable
    data object Home: SubGraph()
}

sealed class Dest {

    @Serializable
    data object HomeScreen: Dest()

    @Serializable
    data class ViewStatusScreen(
        val userId: Int
    ): Dest()

    @Serializable
    data object PostScreen: Dest()

    @Serializable
    data class CreatePostScreen(
        val mediaUri: String
    ): Dest()
}