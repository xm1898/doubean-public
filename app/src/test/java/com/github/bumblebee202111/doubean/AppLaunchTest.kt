package com.github.bumblebee202111.doubean

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

/**
 * 启动冒烟测试：验证 Hilt 图、主题、首页 Compose 树都能真实渲染。
 * 同时把首页截图落到 app/build/screenshots/。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppLaunchTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeRendersWithMovieTvChipsAndNoLegacyTabs() {
        // 首页有无限循环的加载动画，暂停时钟让 Compose 进入 idle
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()

        // 顶部：电影 / 电视 两个按钮在（和搜索图标并排）
        assertTrue(rule.onAllNodesWithText("电影").fetchSemanticsNodes().isNotEmpty())
        assertTrue(rule.onAllNodesWithText("电视").fetchSemanticsNodes().isNotEmpty())

        // 顶部不再有「读书」「音乐」tab
        assertEquals(0, rule.onAllNodesWithText("读书").fetchSemanticsNodes().size)
        assertEquals(0, rule.onAllNodesWithText("音乐").fetchSemanticsNodes().size)

        // 入口区：找电影 / 豆瓣榜单 / 搜索
        assertTrue(rule.onAllNodesWithText("找电影").fetchSemanticsNodes().isNotEmpty())
        assertTrue(rule.onAllNodesWithText("豆瓣榜单").fetchSemanticsNodes().isNotEmpty())

        // 底部导航（动态 / 小组 / 我）已经不存在
        assertEquals(0, rule.onAllNodesWithText("小组").fetchSemanticsNodes().size)
        assertEquals(0, rule.onAllNodesWithText("动态").fetchSemanticsNodes().size)

        saveScreenshot("home")
    }

    /** 等真实豆瓣数据回来，验证「合集 → 卡片」这条链路在 App 内能渲染。 */
    @Test
    fun homeLoadsRealFeedFromDouban() {
        rule.mainClock.autoAdvance = false

        var loaded = false
        val deadline = System.currentTimeMillis() + 40_000
        while (System.currentTimeMillis() < deadline) {
            rule.mainClock.advanceTimeBy(32)
            if (rule.onAllNodesWithText("正在上映").fetchSemanticsNodes().isNotEmpty()) {
                loaded = true
                break
            }
            if (rule.onAllNodesWithText("重试").fetchSemanticsNodes().isNotEmpty()) break
            Thread.sleep(400)
        }

        if (loaded) {
            rule.mainClock.advanceTimeBy(500)
            assertTrue(
                "Top250 section missing",
                rule.onAllNodesWithText("豆瓣 Top 250").fetchSemanticsNodes().isNotEmpty()
            )
            saveScreenshot("home_loaded")
        }
        assertTrue("首页数据没有加载出来（检查网络）", loaded)
    }

    /** 首页 → 找电影 → 组合筛选 → 详情 的导航链路。 */
    @Test
    fun navigateToExploreAndDetail() {
        rule.mainClock.autoAdvance = false
        waitUntilText("正在上映", 40_000)

        // 找电影
        rule.onAllNodesWithText("找电影").onFirst().performClick()
        waitUntilText("综合", 20_000)
        waitUntilExploreCards(3, 40_000)

        // 展开筛选面板：类型 / 体裁 / 地区 / 年份 / 评分 五行都在
        rule.onAllNodesWithTag("filter_toggle").onFirst().performClick()
        waitUntilText("类型", 10_000)
        assertTrue(rule.onAllNodesWithText("体裁").fetchSemanticsNodes().isNotEmpty())
        assertTrue(rule.onAllNodesWithText("地区").fetchSemanticsNodes().isNotEmpty())
        assertTrue(rule.onAllNodesWithText("年份").fetchSemanticsNodes().isNotEmpty())
        assertTrue(rule.onAllNodesWithText("评分").fetchSemanticsNodes().isNotEmpty())

        // 展开动画要跑完，否则点击会落在动画中途的坐标上
        rule.mainClock.advanceTimeBy(800)
        rule.mainClock.advanceTimeBy(800)

        // 体裁=喜剧 + 评分=8分以上，两者叠加后重新出结果
        // （评分区间只有搜索接口支持，时间排序那条信息流是本地过滤，单独在 timeSort 用例里验）
        rule.onAllNodesWithText("喜剧").onFirst().performClick()
        rule.mainClock.advanceTimeBy(500)
        val afterGenre = toggleText()
        rule.onAllNodesWithText("8分以上").onFirst().performClick()
        rule.mainClock.advanceTimeBy(500)
        val afterRating = toggleText()
        assertTrue(
            "genre=$afterGenre rating=$afterRating",
            rule.onAllNodesWithText("筛选 2").fetchSemanticsNodes().isNotEmpty(),
        )
        waitUntilExploreCards(3, 40_000)
        saveScreenshot("explore_filtered")

        // 收起面板后看纯结果列表
        rule.onAllNodesWithTag("filter_toggle").onFirst().performClick()
        var collapsed = false
        val collapseDeadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < collapseDeadline) {
            rule.mainClock.advanceTimeBy(64)
            if (rule.onAllNodesWithText("体裁").fetchSemanticsNodes().isEmpty()) {
                collapsed = true
                break
            }
            Thread.sleep(100)
        }
        assertTrue("筛选面板没有收起", collapsed)
        saveScreenshot("explore")

        // 进详情
        exploreCards().onFirst().performClick()
        waitUntilText("演职人员", 30_000)
        saveScreenshot("detail")
    }

    /**
     * 「时间」排序走官方首映时间信息流：卡片副标题形如 `2026 / 中国大陆 / 剧情`，
     * 列表里的年份必须从新到旧，而不是旧片排在前面。
     */
    @Test
    fun timeSortShowsNewestYearFirst() {
        rule.mainClock.autoAdvance = false
        waitUntilText("正在上映", 40_000)

        rule.onAllNodesWithText("找电影").onFirst().performClick()
        waitUntilText("综合", 20_000)
        waitUntilExploreCards(3, 40_000)

        rule.onAllNodesWithText("时间").onFirst().performClick()

        val years = waitUntilYears(min = 3, timeoutMs = 40_000)
        assertTrue("时间排序不是从新到旧：$years", years.zipWithNext().all { (a, b) -> a >= b })
        assertTrue("时间排序的第一部不是最新年份：$years", years.first() == years.max())
        println("[ok] 时间排序年份顺序=${years.take(9)}")
        saveScreenshot("explore_time")
    }

    /**
     * 连续下拉翻页不能崩：之前同一部片在两页里重复出现，`items(key = …)` 撞 key
     * 会让 LazyVerticalGrid 抛 `Key was already used`，表现就是「刷两下闪退」。
     */
    @Test
    fun pagingDoesNotCrashOnRepeatedScroll() {
        rule.mainClock.autoAdvance = false
        waitUntilText("正在上映", 40_000)

        rule.onAllNodesWithText("找电影").onFirst().performClick()
        waitUntilText("综合", 20_000)
        waitUntilExploreCards(3, 40_000)

        repeat(4) { round ->
            exploreCards().onLast().performTouchInput { swipeUp() }
            // 给翻页请求 + 重组留时间
            repeat(20) { rule.mainClock.advanceTimeBy(50) }
            Thread.sleep(600)
            rule.mainClock.advanceTimeBy(300)
            val alive = exploreCards().fetchSemanticsNodes().size
            if (alive == 0) saveScreenshot("paging_debug")
            assertTrue(
                "第 ${round + 1} 次下拉后列表空了（很可能崩了），可见文本=${visibleTexts().take(20)}",
                alive > 0,
            )
            println("[ok] 第 ${round + 1} 次下拉后卡片数=$alive")
        }
        saveScreenshot("explore_paged")
    }

    /**
     * 边翻页边来回切排序：旧请求晚回来时不能把新列表污染成重复 id，
     * 也不能让 LazyGrid 撞 key 崩掉。
     */
    @Test
    fun rapidSortSwitchDuringPagingDoesNotCrash() {
        rule.mainClock.autoAdvance = false
        waitUntilText("正在上映", 40_000)

        rule.onAllNodesWithText("找电影").onFirst().performClick()
        waitUntilText("综合", 20_000)
        waitUntilExploreCards(3, 40_000)

        repeat(3) { round ->
            exploreCards().onLast().performTouchInput { swipeUp() }
            repeat(6) { rule.mainClock.advanceTimeBy(50) }

            listOf("RANK", "TIME", "RECOMMEND").forEach { sort ->
                rule.onNodeWithTag("sort_pill_$sort").performClick()
                repeat(5) { rule.mainClock.advanceTimeBy(50) }
            }

            Thread.sleep(500)
            repeat(10) { rule.mainClock.advanceTimeBy(50) }

            // 切排序会先回到 Loading，等新一页回来；等不到就是崩了或卡死。
            val alive = waitUntilExploreCards(1, 25_000)
            if (alive == 0) saveScreenshot("sort_stress_debug")
            assertTrue(
                "第 ${round + 1} 轮边翻页边切排序后没有卡片回来（很可能崩了），可见文本=${visibleTexts().take(20)}",
                alive > 0,
            )
            println("[ok] 第 ${round + 1} 轮切排序后卡片数=$alive")
        }
    }

    /** 等结果网格里真的出现卡片（首页卡片不算）。 */
    private fun waitUntilExploreCards(min: Int, timeoutMs: Long): Int {
        val deadline = System.currentTimeMillis() + timeoutMs
        var last = 0
        while (System.currentTimeMillis() < deadline) {
            rule.mainClock.advanceTimeBy(32)
            last = exploreCards().fetchSemanticsNodes().size
            if (last >= min) return last
            skipIfThrottled()
            Thread.sleep(300)
        }
        throw AssertionError("等待结果卡片超时，可见文本=" + visibleTexts().take(20))
    }

    /** 点完排序后列表会先回到 Loading，等卡片副标题真正出来再断言。 */
    private fun waitUntilYears(min: Int, timeoutMs: Long): List<Int> {
        val deadline = System.currentTimeMillis() + timeoutMs
        var last: List<Int> = emptyList()
        while (System.currentTimeMillis() < deadline) {
            rule.mainClock.advanceTimeBy(32)
            last = visibleCardYears()
            if (last.size >= min) return last
            skipIfThrottled()
            Thread.sleep(300)
        }
        throw AssertionError("等待年份副标题超时，可见文本=" + visibleTexts().take(30))
    }

    /** 只看结果网格里的卡片：首页也用了同一个 SubjectCard，不加范围会串。 */
    private fun exploreCards() = rule.onAllNodes(
        hasTestTag("subject_card") and hasAnyAncestor(hasTestTag("explore_grid")),
    )

    private fun visibleTexts(): List<String> =
        rule.onAllNodes(hasAnyAncestor(hasTestTag("explore_grid")))
            .fetchSemanticsNodes()
            .flatMap { node -> node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } }

    /** 把结果网格里卡片副标题（形如 `2026 / 中国大陆 / …`）的年份按顺序读出来。 */
    private fun visibleCardYears(): List<Int> {
        val pattern = Regex("^\\s*(\\d{4})\\s*/")
        return visibleTexts().mapNotNull { pattern.find(it)?.groupValues?.get(1)?.toIntOrNull() }
    }

    private fun toggleText(): String = rule.onAllNodesWithTag("filter_toggle")
        .fetchSemanticsNodes()
        .mapNotNull { it.config.getOrNull(SemanticsProperties.Text)?.joinToString() }
        .joinToString("|")

    /** 豆瓣限流时界面会给出提示；这是外部因素，跳过用例而不是让构建红。 */
    private fun skipIfThrottled() {
        val throttled = rule.onAllNodesWithText("豆瓣限制访问频率了，歇一会儿再试")
            .fetchSemanticsNodes()
            .isNotEmpty()
        if (throttled) {
            println("[skip] 豆瓣限流，跳过本次界面用例")
            Assume.assumeTrue("豆瓣限流，跳过本次界面用例", false)
        }
    }

    private fun waitUntilText(text: String, timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            rule.mainClock.advanceTimeBy(32)
            if (rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()) return true
            skipIfThrottled()
            Thread.sleep(300)
        }
        throw AssertionError("等待文本超时: $text")
    }

    private fun waitUntilTag(tag: String, timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            rule.mainClock.advanceTimeBy(32)
            if (rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()) return true
            skipIfThrottled()
            Thread.sleep(300)
        }
        throw AssertionError("等待节点超时: $tag")
    }

    private fun saveScreenshot(name: String) {
        val activity = rule.activity
        val decor = activity.window.decorView
        val width = if (decor.width > 0) decor.width else 1080
        val height = if (decor.height > 0) decor.height else 2160
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        decor.draw(canvas)
        val dir = File("build/screenshots").apply { mkdirs() }
        FileOutputStream(File(dir, "$name.png")).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }
}
