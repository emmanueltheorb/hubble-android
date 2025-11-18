package com.orb.hubble.navigation

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
}