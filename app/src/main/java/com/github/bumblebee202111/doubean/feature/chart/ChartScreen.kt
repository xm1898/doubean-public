package com.github.bumblebee202111.doubean.feature.chart

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.github.bumblebee202111.doubean.model.subject.SubjectType
import com.github.bumblebee202111.doubean.ui.UiState
import com.github.bumblebee202111.doubean.ui.component.EmptyBox
import com.github.bumblebee202111.doubean.ui.component.ErrorBox
import com.github.bumblebee202111.doubean.ui.component.LoadingBox
import com.github.bumblebee202111.doubean.ui.component.formatRating
import com.github.bumblebee202111.doubean.model.subject.Rating

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartScreen(
    type: SubjectType,
    onBack: () -> Unit,
    onOpenDetail: (String, SubjectType) -> Unit,
    viewModel: ChartViewModel = hiltViewModel(),
) {
    LaunchedEffect(type) { viewModel.load(type) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("豆瓣榜单") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        when (val s = state) {
            is UiState.Idle, is UiState.Loading -> LoadingBox(
                Modifier.padding(innerPadding),
            )

            is UiState.Error -> ErrorBox(
                message = s.message,
                modifier = Modifier.padding(innerPadding),
                onRetry = viewModel::retry,
            )

            is UiState.Success -> {
                if (s.data.isEmpty()) {
                    EmptyBox(modifier = Modifier.padding(innerPadding))
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        items(s.data, key = { it.subject.id + it.subject.title }) { ranked ->
                            val subject = ranked.subject
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenDetail(subject.id, type) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = ranked.rankValue.toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.width(32.dp),
                                )
                                AsyncImage(
                                    model = subject.imageUrl,
                                    contentDescription = subject.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .width(56.dp)
                                        .height(80.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = subject.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (subject.cardSubtitle.isNotBlank()) {
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = subject.cardSubtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    (subject.rating as? Rating.NonNull)?.let {
                                        Text(
                                            text = formatRating(it.value),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary,
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
