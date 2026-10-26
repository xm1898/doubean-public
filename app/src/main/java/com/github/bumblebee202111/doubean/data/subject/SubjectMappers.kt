package com.github.bumblebee202111.doubean.data.subject

import com.github.bumblebee202111.doubean.core.network.model.DtoActor
import com.github.bumblebee202111.doubean.core.network.model.DtoChartItem
import com.github.bumblebee202111.doubean.core.network.model.DtoCollectionSubject
import com.github.bumblebee202111.doubean.core.network.model.DtoImageItem
import com.github.bumblebee202111.doubean.core.network.model.DtoPhotoItem
import com.github.bumblebee202111.doubean.core.network.model.DtoRating
import com.github.bumblebee202111.doubean.core.network.model.DtoRecommendSubject
import com.github.bumblebee202111.doubean.core.network.model.DtoSearchSubject
import com.github.bumblebee202111.doubean.core.network.model.DtoSuggestItem
import com.github.bumblebee202111.doubean.core.network.model.DtoTagSubject
import com.github.bumblebee202111.doubean.core.network.model.DtoTrailer
import com.github.bumblebee202111.doubean.core.network.model.DtoVendor
import com.github.bumblebee202111.doubean.core.network.model.NetworkPhotos
import com.github.bumblebee202111.doubean.core.network.model.NetworkSubjectDetail
import com.github.bumblebee202111.doubean.model.ImageItem
import com.github.bumblebee202111.doubean.model.Photo
import com.github.bumblebee202111.doubean.model.PhotoList
import com.github.bumblebee202111.doubean.model.SizedImage
import com.github.bumblebee202111.doubean.model.subject.Celebrity
import com.github.bumblebee202111.doubean.model.subject.CreditList
import com.github.bumblebee202111.doubean.model.subject.Movie
import com.github.bumblebee202111.doubean.model.subject.MovieDetail
import com.github.bumblebee202111.doubean.model.subject.MovieTrailer
import com.github.bumblebee202111.doubean.model.subject.Rating
import com.github.bumblebee202111.doubean.model.subject.RecommendSubject
import com.github.bumblebee202111.doubean.model.subject.SearchResultSubjectItem
import com.github.bumblebee202111.doubean.model.subject.Subject
import com.github.bumblebee202111.doubean.model.subject.SubjectDetail
import com.github.bumblebee202111.doubean.model.subject.SubjectType
import com.github.bumblebee202111.doubean.model.subject.SubjectWithRank
import com.github.bumblebee202111.doubean.model.subject.Tv
import com.github.bumblebee202111.doubean.model.subject.TvDetail
import com.github.bumblebee202111.doubean.model.subject.Vendor

/* --------------------------- 评分 --------------------------- */

fun DtoRating?.toRating(): Rating {
    val dto = this ?: return Rating.Null("")
    val v = dto.value
    if (v == null || v <= 0.0) {
        return Rating.Null(dto.nullRatingReason.orEmpty())
    }
    return Rating.NonNull(v.toFloat(), if (dto.max > 0) dto.max else 10)
}

/* --------------------------- 列表项 --------------------------- */

fun DtoCollectionSubject.toSubject(fallbackType: SubjectType): Subject {
    val resolved = type?.let(SubjectType::fromString) ?: fallbackType
    val imageUrl = cover?.url ?: pic?.large ?: pic?.normal.orEmpty()
    val subtitle = cardSubtitle ?: info.orEmpty()
    val id = id
    val title = title
    val uri = uri ?: url.orEmpty()
    val rating = rating.toRating()
    return when (resolved) {
        SubjectType.TV -> Tv(id, rating, subtitle, title, imageUrl, uri)
        SubjectType.MOVIE -> Movie(id, rating, subtitle, title, imageUrl, uri)
        else -> Subject.Unsupported(id, rating, title, subtitle, imageUrl, uri)
    }
}

fun DtoRecommendSubject.toRecommendSubject(fallbackType: SubjectType): RecommendSubject =
    RecommendSubject(
        id = id,
        title = title,
        imageUrl = pic?.large ?: pic?.normal.orEmpty(),
        rating = rating.toRating(),
        type = type?.let(SubjectType::fromString) ?: fallbackType,
        uri = uri ?: url.orEmpty(),
        cardSubtitle = cardSubtitle.orEmpty(),
        interest = null,
    )

fun DtoChartItem.toSubjectWithRank(type: SubjectType): SubjectWithRank<Subject> {
    val score = rating.getOrNull(0)?.toFloatOrNull()
    val rating = if (score == null || score <= 0f) {
        Rating.Null("")
    } else {
        Rating.NonNull(score, 10)
    }
    val subtitle = buildList {
        regions.firstOrNull()?.let(::add)
        types.take(2).joinToString(" ").takeIf { it.isNotEmpty() }?.let(::add)
    }.joinToString(" / ")
    val subject: Subject = when (type) {
        SubjectType.TV -> Tv(id, rating, subtitle, title, coverUrl.orEmpty(), url.orEmpty())
        else -> Movie(id, rating, subtitle, title, coverUrl.orEmpty(), url.orEmpty())
    }
    return SubjectWithRank(subject, rank)
}

fun DtoTagSubject.toSubject(type: SubjectType): Subject {
    val score = rate?.toFloatOrNull()
    val rating = if (score == null || score <= 0f) Rating.Null("") else Rating.NonNull(score, 10)
    val subtitle = episodesInfo.orEmpty()
    return when (type) {
        SubjectType.TV -> Tv(id, rating, subtitle, title, cover.orEmpty(), url.orEmpty())
        else -> Movie(id, rating, subtitle, title, cover.orEmpty(), url.orEmpty())
    }
}

fun DtoSearchSubject.toSubject(type: SubjectType, yearHint: String? = null): Subject {
    val score = rate?.toFloatOrNull()
    val rating = if (score == null || score <= 0f) Rating.Null("") else Rating.NonNull(score, 10)
    // 搜索接口不返回年份；按年份翻页时把年份传进来，卡片上才看得出「新片在前」。
    val subtitle = yearHint ?: buildList {
        directors.firstOrNull()?.let { add(it) }
        casts.take(2).joinToString(" ").takeIf { it.isNotEmpty() }?.let(::add)
    }.joinToString(" / ")
    return when (type) {
        SubjectType.TV -> Tv(id, rating, subtitle, title, cover.orEmpty(), url.orEmpty())
        else -> Movie(id, rating, subtitle, title, cover.orEmpty(), url.orEmpty())
    }
}

fun DtoSuggestItem.toSearchResultItem(type: SubjectType): SearchResultSubjectItem =
    SearchResultSubjectItem(
        hasLinewatch = null,
        controversyReason = "",
        title = title,
        abstract = episode?.takeIf { it.isNotBlank() },
        uri = url.orEmpty(),
        coverUrl = img.orEmpty(),
        year = year,
        cardSubtitle = buildList {
            year?.takeIf { it.isNotBlank() }?.let(::add)
            type?.let(::add)
        }.joinToString(" / "),
        id = id,
        nullRatingReason = "",
        rating = Rating.Null(""),
        type = this.type?.let(SubjectType::fromString) ?: type,
    )

/* --------------------------- 详情 --------------------------- */

fun DtoVendor.toVendor(): Vendor = Vendor(
    id = id.orEmpty(),
    title = title.orEmpty(),
    url = url.orEmpty(),
    uri = uri.orEmpty(),
    icon = icon.orEmpty(),
    grayIcon = grayIcon.orEmpty(),
    paymentDesc = paymentDesc.orEmpty(),
)

fun DtoTrailer.toMovieTrailer(): MovieTrailer = MovieTrailer(
    id = id.orEmpty(),
    coverUrl = coverUrl.orEmpty(),
    videoUrl = videoUrl.orEmpty(),
    title = title.orEmpty(),
    runtime = runtime.orEmpty(),
)

fun NetworkSubjectDetail.toSubjectDetail(type: SubjectType): SubjectDetail {
    val rating = rating.toRating()
    val title = title
    val cover = pic?.large ?: pic?.normal ?: coverUrl.orEmpty()
    val uri = uri ?: url.orEmpty()
    val intro = intro.orEmpty()
    val subtitle = cardSubtitle.orEmpty()
    val year = year.orEmpty()
    val genres = genres
    val countries = countries
    val durations = durations
    val pubdate = pubdate
    val actorNames = actors.mapNotNull { it.name }
    val directorNames = directors.mapNotNull { it.name }
    val trailers = trailers.map { it.toMovieTrailer() }
    val vendors = vendors.map { it.toVendor() }
    return when (type) {
        SubjectType.TV -> TvDetail(
            id = id,
            rating = rating,
            cardSubtitle = subtitle,
            title = title,
            coverUrl = cover,
            uri = uri,
            intro = intro,
            interest = null,
            isReleased = isReleased,
            vendors = vendors,
            pubdate = pubdate,
            year = year,
            languages = languages,
            genres = genres,
            actorNames = actorNames,
            episodesCount = episodesCount,
            durations = durations,
            trailers = trailers,
            countries = countries,
            originalTitle = originalTitle,
            directorNames = directorNames,
        )

        else -> MovieDetail(
            id = id,
            rating = rating,
            cardSubtitle = subtitle,
            title = title,
            coverUrl = cover,
            uri = uri,
            intro = intro,
            interest = null,
            isReleased = isReleased,
            vendors = vendors,
            pubdate = pubdate,
            year = year,
            genres = genres,
            actorNames = actorNames,
            durations = durations,
            trailers = trailers,
            countries = countries,
            originalTitle = originalTitle,
            directorNames = directorNames,
        )
    }
}

fun DtoActor.toCelebrity(): Celebrity = Celebrity(
    id = id.orEmpty(),
    name = name.orEmpty(),
    character = character.orEmpty().toCreditCharacter(),
    avatarUrl = avatar?.large ?: avatar?.normal.orEmpty(),
)

/**
 * 豆瓣的 character 形如「演员 Actor (饰 里昂)」「导演 Director」，
 * 卡片只有一行位置，取「饰 xxx」里的角色名，否则去掉英文工种只留中文。
 */
internal fun String.toCreditCharacter(): String {
    val raw = trim()
    if (raw.isEmpty()) return ""
    raw.substringAfter("饰 ", "").takeIf { it.isNotBlank() }?.let {
        return it.trimEnd(')', '）').trim()
    }
    return raw
        .replace("Actor", "")
        .replace("Actress", "")
        .replace("Director", "")
        .replace("Writer", "")
        .trim()
}

fun List<DtoActor>.toCreditList(total: Int): CreditList =
    CreditList(items = map { it.toCelebrity() }, total = total)

/* --------------------------- 剧照 --------------------------- */

private fun DtoImageItem?.toImageItem(): ImageItem =
    ImageItem(
        height = this?.height ?: 0,
        size = this?.size,
        url = this?.url.orEmpty(),
        width = this?.width ?: 0,
    )

fun DtoPhotoItem.toPhoto(): Photo = Photo(
    image = SizedImage(
        large = image?.large.toImageItem(),
        normal = image?.normal.toImageItem(),
    ),
    id = id,
)

fun NetworkPhotos.toPhotoList(): PhotoList = PhotoList(
    count = count,
    photos = photos.map { it.toPhoto() },
    start = start,
    total = total,
)
