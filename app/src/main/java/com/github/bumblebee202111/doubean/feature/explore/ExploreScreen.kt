package com.github.bumblebee202111.doubean.feature.explore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.bumblebee202111.doubean.model.subject.SubjectType
import com.github.bumblebee202111.doubean.ui.UiState
import com.github.bumblebee202111.doubean.ui.component.EmptyBox
import com.github.bumblebee202111.doubean.ui.component.ErrorBox
import com.github.bumblebee202111.doubean.ui.component.LoadingBox
import com.github.bumblebee202111.doubean.ui.component.SubjectCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    type: SubjectType,
    onBack: () -> Unit,
    onOpenDetail: (String, SubjectType) -> Unit,
    viewModel: ExploreViewModel = hiltViewModel(),
) {
    LaunchedEffect(type) { viewModel.start(type) }

    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()
    val loadingMore by viewModel.loadingMore.collectAsStateWithLifecycle()
    val exhausted by viewModel.exhausted.collectAsStateWithLifecycle()

    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (type == SubjectType.TV) "找电视" else "找电影") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (!filters.isDefault) {
                        TextButton(onClick = viewModel::resetFilters) { Text("重置") }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // 常驻一行：排序 + 筛选开关，尽量把高度留给海报
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(ExploreSort.entries.toList()) { sort ->
                        FilterPill(
                            text = sort.label,
                            selected = filters.sort == sort,
                            onClick = { viewModel.setSort(sort) },
                        )
                    }
                }
                Surface(
                    onClick = { expanded = !expanded },
                    shape = RoundedCornerShape(50),
                    color = if (filters.activeCount > 0) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    contentColor = if (filters.activeCount > 0) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier
                        .height(28.dp)
                        .testTag("filter_toggle"),
                ) {
                    Row(
                        modifier = Modifier.padding(start = 10.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (filters.activeCount > 0) "筛选 ${filters.activeCount}" else "筛选",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            contentDescription = if (expanded) "收起筛选" else "展开筛选",
                            modifier = Modifier
                                .padding(start = 2.dp)
                                .width(16.dp),
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    FilterRow(
                        label = "类型",
                        options = ExploreViewModel.CATEGORIES,
                        selected = filters.category,
                        onSelect = viewModel::setCategory,
                    )
                    FilterRow(
                        label = "体裁",
                        options = ExploreViewModel.genresFor(filters.category),
                        selected = filters.genre,
                        onSelect = viewModel::setGenre,
                    )
                    FilterRow(
                        label = "地区",
                        options = ExploreViewModel.REGIONS,
                        selected = filters.region,
                        onSelect = viewModel::setRegion,
                    )
                    FilterRow(
                        label = "年份",
                        options = ExploreViewModel.YEARS,
                        selected = filters.year,
                        onSelect = viewModel::setYear,
                    )
                    FilterRow(
                        label = "评分",
                        options = ExploreViewModel.RATINGS,
                        selected = filters.rating,
                        onSelect = viewModel::setRating,
                    )
                }
            }

            when (val state = items) {
                is UiState.Idle, is UiState.Loading -> LoadingBox()
                is UiState.Error -> ErrorBox(state.message, onRetry = viewModel::reload)
                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        EmptyBox("没有符合条件的结果，换个筛选试试")
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(state.data, key = { it.id + it.title }) { subject ->
                                SubjectCard(
                                    subject = subject,
                                    width = 104,
                                    onClick = { onOpenDetail(subject.id, type) },
                                )
                            }
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    when {
                                        loadingMore -> LoadingBox()
                                        exhausted -> Text(
                                            text = "没有更多了",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )

                                        else -> LaunchedEffect(state.data.size) { viewModel.loadMore() }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 一行筛选：左侧固定宽度标签 + 可横向滚动的紧凑胶囊。 */
@Composable
private fun FilterRow(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(32.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(options) { option ->
                FilterPill(
                    text = option,
                    selected = option == selected,
                    onClick = { onSelect(option) },
                )
            }
        }
    }
}

@Composable
private fun FilterPill(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier.height(28.dp),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}
