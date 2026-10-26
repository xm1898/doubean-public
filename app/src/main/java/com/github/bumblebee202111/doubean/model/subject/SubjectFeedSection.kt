package com.github.bumblebee202111.doubean.model.subject

/**
 * 首页（电影 / 电视）的一段横向内容。
 */
data class SubjectFeedSection(
    val id: String,
    val title: String,
    val subjects: List<Subject>,
)
