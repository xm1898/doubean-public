package com.github.bumblebee202111.doubean.feature.detail

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.github.bumblebee202111.doubean.model.subject.Rating
import com.github.bumblebee202111.doubean.model.subject.Subject
import com.github.bumblebee202111.doubean.model.subject.SubjectDetail
import com.github.bumblebee202111.doubean.model.subject.SubjectType
import com.github.bumblebee202111.doubean.ui.UiState
import com.github.bumblebee202111.doubean.ui.component.ErrorBox
import com.github.bumblebee202111.doubean.ui.component.LoadingBox
import com.github.bumblebee202111.doubean.ui.component.SubjectCard
import com.github.bumblebee202111.doubean.ui.component.formatRating

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    id: String,
    type: SubjectType,
    onBack: () -> Unit,
    onOpenDetail: (String, SubjectType) -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(id) { viewModel.load(id, type) }

    val detailState by viewModel.detail.collectAsStateWithLifecycle()
    val credits by viewModel.credits.collectAsStateWithLifecycle()
    val photos by viewModel.photos.collectAsStateWithLifecycle()
    val recommendations by viewModel.recommendations.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = (detailState as? UiState.Success)?.data?.title.orEmpty(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        when (val state = detailState) {
            is UiState.Idle, is UiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            ) { LoadingBox() }

            is UiState.Error -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            ) { ErrorBox(state.message, onRetry = viewModel::retry) }

            is UiState.Success -> {
                val detail = state.data
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(bottom = 32.dp),
                ) {
                    item { DetailHeader(detail) }

                    if (detail.intro.isNotBlank()) {
                        item {
                            SectionTitle("简介")
                            ExpandableText(detail.intro)
                        }
                    }

                    if (!credits?.items.isNullOrEmpty()) {
                        item { SectionTitle("演职人员") }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(credits!!.items, key = { it.id + it.name }) { celebrity ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.width(72.dp),
                                    ) {
                                        AsyncImage(
                                            model = celebrity.avatarUrl,
                                            contentDescription = celebrity.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(CircleShape),
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = celebrity.name,
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        if (celebrity.character.isNotBlank()) {
                                            Text(
                                                text = celebrity.character,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (!photos?.photos.isNullOrEmpty()) {
                        item { SectionTitle("剧照") }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(photos!!.photos, key = { it.id ?: it.image.large.url }) { photo ->
                                    AsyncImage(
                                        model = photo.image.normal.url,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .width(160.dp)
                                            .height(100.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                    )
                                }
                            }
                        }
                    }

                    if (recommendations.isNotEmpty()) {
                        item { SectionTitle("相关推荐") }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(recommendations, key = { it.id + it.title }) { subject ->
                                    SubjectCard(
                                        subject = subject,
                                        onClick = { onOpenDetail(it.id, it.type) },
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

@Composable
private fun DetailHeader(detail: SubjectDetail) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        AsyncImage(
            model = detail.coverUrl,
            contentDescription = detail.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(110.dp)
                .height(160.dp)
                .clip(RoundedCornerShape(10.dp)),
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = detail.displayTitle.getString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            detail.displaySubtitle?.let { subtitle ->
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle.getString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            when (val rating = detail.rating) {
                is Rating.NonNull -> Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = formatRating(rating.value),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "/ ${rating.max}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                is Rating.Null -> Text(
                    text = rating.reason.ifBlank { "暂无评分" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val metaParts = detail.displayMetaInfo
                .split(" / ")
                .filter { it.isNotBlank() }
            if (metaParts.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                metaParts.chunked(2).forEach { line ->
                    Text(
                        text = line.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun ExpandableText(text: String) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = if (expanded) Int.MAX_VALUE else 5,
            overflow = TextOverflow.Ellipsis,
        )
        TextButton(onClick = { expanded = !expanded }) {
            Text(if (expanded) "收起" else "展开全部")
        }
    }
}
