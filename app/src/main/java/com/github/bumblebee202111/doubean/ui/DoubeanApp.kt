package com.github.bumblebee202111.doubean.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.github.bumblebee202111.doubean.feature.chart.ChartScreen
import com.github.bumblebee202111.doubean.feature.detail.DetailScreen
import com.github.bumblebee202111.doubean.feature.explore.ExploreScreen
import com.github.bumblebee202111.doubean.feature.search.SearchScreen
import com.github.bumblebee202111.doubean.feature.subjects.SubjectsScreen
import com.github.bumblebee202111.doubean.navigation.Screen

/**
 * 极简导航：只有书影音一条线（首页 → 找片 / 榜单 / 搜索 → 详情）。
 */
@Composable
fun DoubeanApp() {
    var backStack by remember { mutableStateOf<List<Screen>>(listOf(Screen.Subjects)) }

    val push: (Screen) -> Unit = { screen -> backStack = backStack + screen }
    val pop: () -> Unit = { if (backStack.size > 1) backStack = backStack.dropLast(1) }

    BackHandler(enabled = backStack.size > 1) { pop() }

    when (val screen = backStack.last()) {
        is Screen.Subjects -> SubjectsScreen(
            onOpenExplore = { push(Screen.Explore(it)) },
            onOpenChart = { push(Screen.Chart(it)) },
            onOpenSearch = { push(Screen.Search) },
            onOpenDetail = { id, type -> push(Screen.Detail(id, type)) },
        )

        is Screen.Explore -> ExploreScreen(
            type = screen.type,
            onBack = pop,
            onOpenDetail = { id, type -> push(Screen.Detail(id, type)) },
        )

        is Screen.Chart -> ChartScreen(
            type = screen.type,
            onBack = pop,
            onOpenDetail = { id, type -> push(Screen.Detail(id, type)) },
        )

        is Screen.Detail -> DetailScreen(
            id = screen.id,
            type = screen.type,
            onBack = pop,
            onOpenDetail = { id, type -> push(Screen.Detail(id, type)) },
        )

        is Screen.Search -> SearchScreen(
            onBack = pop,
            onOpenDetail = { id, type -> push(Screen.Detail(id, type)) },
        )
    }
}
