package com.github.bumblebee202111.doubean.feature.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (type == SubjectType.TV) "找电视" else "找电影")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
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
            FilterRow(
                label = "类型",
                options = ExploreViewModel.CATEGORIES,
                selected = filters.category,
                onSelect = viewModel::setCategory,
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
                label = "排序",
                options = ExploreSort.entries.map { it.label },
                selected = filters.sort.label,
                onSelect = { label ->
                    ExploreSort.entries.firstOrNull { it.label == label }?.let(viewModel::setSort)
                },
            )

            when (val state = items) {
                is UiState.Idle, is UiState.Loading -> LoadingBox()
                is UiState.Error -> ErrorBox(state.message, onRetry = viewModel::reload)
                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        EmptyBox("没有符合条件的结果")
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(state.data, key = { it.id + it.title }) { subject ->
                                SubjectCard(
                                    subject = subject,
                                    width = 104,
                                    onClick = { onOpenDetail(it.id, type) },
                                )
                            }
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    if (loadingMore) {
                                        LoadingBox()
                                    } else {
                                        LaunchedEffect(state.data.size) { viewModel.loadMore() }
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

@Composable
private fun FilterRow(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(top = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, bottom = 2.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(options) { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = { Text(option) },
                )
            }
        }
    }
}
