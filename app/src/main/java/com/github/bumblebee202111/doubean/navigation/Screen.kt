package com.github.bumblebee202111.doubean.navigation

import com.github.bumblebee202111.doubean.model.subject.SubjectType

/**
 * 应用内页面。整个 App 只有书影音相关的这几屏。
 */
sealed interface Screen {
    data object Subjects : Screen
    data class Explore(val type: SubjectType) : Screen
    data class Chart(val type: SubjectType) : Screen
    data class Detail(val id: String, val type: SubjectType) : Screen
    data object Search : Screen
}
