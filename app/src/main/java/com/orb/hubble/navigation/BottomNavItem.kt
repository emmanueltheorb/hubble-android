package com.orb.hubble.navigation

import com.orb.hubble.R

data class BottomNavItem(
    val name: String,
    val dest: Dest,
    val icon: Int,
    val size: Int = 20,
    val hasNews: Boolean = false
)

val bottomNavIcons = listOf(
    BottomNavItem(
        name = "Explore",
        dest = Dest.ExploreScreen,
        icon = R.drawable.explore_icon
    ),
    BottomNavItem(
        name = "Home",
        dest = Dest.HomeScreen,
        icon = R.drawable.home_icon
    ),
    BottomNavItem(
        name = "Replies",
        dest = Dest.ReplyLogScreen,
        icon = R.drawable.replies_icon
    ),
    BottomNavItem(
        name = "You",
        dest = Dest.ProfileScreen,
        icon = R.drawable.profile_icon
    )
)
