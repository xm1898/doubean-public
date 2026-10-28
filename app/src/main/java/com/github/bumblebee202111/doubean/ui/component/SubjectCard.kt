package com.github.bumblebee202111.doubean.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.github.bumblebee202111.doubean.model.subject.Rating
import com.github.bumblebee202111.doubean.model.subject.Subject

/** 豆瓣海报上的评分配色，固定值，不受明暗主题影响（因为它是压在海报上的）。 */
private val RatingOnPoster = Color(0xFFFFB400)

@Composable
fun RatingText(
    rating: Rating,
    modifier: Modifier = Modifier,
) {
    when (rating) {
        is Rating.NonNull -> Text(
            text = formatRating(rating.value),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = modifier,
        )

        is Rating.Null -> Text(
            text = "暂无",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
    }
}

fun formatRating(value: Float): String {
    val rounded = (value * 10).toInt() / 10.0
    return rounded.toString()
}

/**
 * 海报卡片：片名直接压在封面底部，底下垫一层黑色渐变遮罩。
 *
 * 之前片名放在封面下方的小字里，封面一多、字又小，用户反馈「字都看不见」；
 * 现在片名在封面上、白字 + 渐变压底，海报再亮再花也读得清；
 * 评分做成右上角的小角标，同样压在深色底上，不会跟海报底色糊在一起。
 */
@Composable
fun SubjectCard(
    subject: Subject,
    modifier: Modifier = Modifier,
    width: Int = 104,
    onClick: (Subject) -> Unit,
) {
    Column(
        modifier = modifier
            .width(width.dp)
            .testTag("subject_card")
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick(subject) },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(10.dp))
                // 海报没加载出来时也有个底色，不会是一块空白
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            AsyncImage(
                model = subject.imageUrl,
                contentDescription = subject.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            // 底部渐变 + 片名
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.5f to Color.Black.copy(alpha = 0.30f),
                            1f to Color.Black.copy(alpha = 0.88f),
                        )
                    )
                    .padding(horizontal = 6.dp, vertical = 6.dp),
            ) {
                Text(
                    text = subject.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // 右上角评分角标
            val rating = subject.rating
            if (rating is Rating.NonNull) {
                Surface(
                    color = Color.Black.copy(alpha = 0.62f),
                    contentColor = RatingOnPoster,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                ) {
                    Text(
                        text = formatRating(rating.value),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                }
            }
        }

        if (subject.cardSubtitle.isNotBlank()) {
            Text(
                text = subject.cardSubtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp),
            )
        }
    }
}
