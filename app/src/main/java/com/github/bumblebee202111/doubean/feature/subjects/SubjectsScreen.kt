package com.github.bumblebee202111.doubean.feature.subjects

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Theaters
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.bumblebee202111.doubean.model.subject.Subject
import com.github.bumblebee202111.doubean.model.subject.SubjectType
import com.github.bumblebee202111.doubean.ui.UiState
import com.github.bumblebee202111.doubean.ui.component.EmptyBox
import com.github.bumblebee202111.doubean.ui.component.ErrorBox
import com.github.bumblebee202111.doubean.ui.component.LoadingBox
import com.github.bumblebee202111.doubean.ui.component.SubjectCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectsScreen(
    onOpenExplore: (SubjectType) -> Unit,
    onOpenChart: (SubjectType) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenDetail: (String, SubjectType) -> Unit,
    viewModel: SubjectsViewModel = hiltViewModel(),
) {
    val selectedType by viewModel.selectedType.collectAsStateWithLifecycle()
    val feedState by viewModel.feedState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "书影音",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    Row(
                        modifier = Modifier.padding(end = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TypeChip(
                            text = "电影",
                            selected = selectedType == SubjectType.MOVIE,
                            onClick = { viewModel.selectType(SubjectType.MOVIE) },
                        )
                        TypeChip(
                            text = "电视",
                            selected = selectedType == SubjectType.TV,
                            onClick = { viewModel.selectType(SubjectType.TV) },
                        )
                        IconButton(onClick = onOpenSearch) {
                            Icon(Icons.Outlined.Search, contentDescription = "搜索")
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                EntryRow(
                    type = selectedType,
                    onOpenExplore = { onOpenExplore(selectedType) },
                    onOpenChart = { onOpenChart(selectedType) },
                    onOpenSearch = onOpenSearch,
                )
            }

            when (val state = feedState) {
                is UiState.Loading, is UiState.Idle -> item { LoadingBox() }
                is UiState.Error -> item { ErrorBox(state.message, onRetry = viewModel::retry) }
                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        item { EmptyBox() }
                    } else {
                        state.data.forEach { section ->
                            item(key = section.id) {
                                SectionHeader(title = section.title)
                            }
                            item(key = "${section.id}_row") {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    items(section.subjects, key = { it.id + it.title }) { subject ->
                                        SubjectCard(
                                            subject = subject,
                                            onClick = { onOpenDetail(it.id, selectedType) },
                                        )
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
private fun TypeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
    )
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun EntryRow(
    type: SubjectType,
    onOpenExplore: () -> Unit,
    onOpenChart: () -> Unit,
    onOpenSearch: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        if (type == SubjectType.TV) {
            EntryItem(
                icon = Icons.Outlined.Tv,
                label = "找电视",
                onClick = onOpenExplore,
            )
        } else {
            EntryItem(
                icon = Icons.Outlined.Theaters,
                label = "找电影",
                onClick = onOpenExplore,
            )
        }
        EntryItem(
            icon = Icons.Outlined.Star,
            label = "豆瓣榜单",
            onClick = onOpenChart,
        )
        EntryItem(
            icon = Icons.Outlined.Search,
            label = "搜索",
            onClick = onOpenSearch,
        )
    }
}

@Composable
private fun EntryItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(88.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(56.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
}
