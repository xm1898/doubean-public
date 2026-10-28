package com.github.bumblebee202111.doubean.feature.subjects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Theaters
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
                        text = "影音",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    Row(
                        modifier = Modifier.padding(end = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "搜索",
                                modifier = Modifier.size(22.dp),
                            )
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
        ) {
            item {
                EntryRow(
                    type = selectedType,
                    onOpenExplore = { onOpenExplore(selectedType) },
                    onOpenChart = { onOpenChart(selectedType) },
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
        label = { Text(text, style = MaterialTheme.typography.labelLarge) },
        shape = RoundedCornerShape(50),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    )
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 10.dp),
    )
}

/** 首页顶部的两个快捷入口：找片 / 榜单，等宽铺满一行。 */
@Composable
private fun EntryRow(
    type: SubjectType,
    onOpenExplore: () -> Unit,
    onOpenChart: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        QuickAction(
            icon = if (type == SubjectType.TV) Icons.Outlined.Tv else Icons.Outlined.Theaters,
            label = if (type == SubjectType.TV) "找电视" else "找电影",
            onClick = onOpenExplore,
            modifier = Modifier.weight(1f),
        )
        QuickAction(
            icon = Icons.Outlined.Star,
            label = "豆瓣榜单",
            onClick = onOpenChart,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun QuickAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier
            .height(46.dp)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
            )
        }
    }
}
