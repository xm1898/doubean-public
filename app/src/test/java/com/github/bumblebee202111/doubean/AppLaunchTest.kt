package com.github.bumblebee202111.doubean

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    /** 首页 → 找电影 → 详情 的导航链路。 */
    @Test
    fun navigateToExploreAndDetail() {
        rule.mainClock.autoAdvance = false
        waitUntilText("正在上映", 40_000)

        // 找电影
        rule.onAllNodesWithText("找电影").onFirst().performClick()
        waitUntilText("类型", 20_000)
        assertTrue(rule.onAllNodesWithText("地区").fetchSemanticsNodes().isNotEmpty())
        assertTrue(rule.onAllNodesWithText("排序").fetchSemanticsNodes().isNotEmpty())
        waitUntilTag("subject_card", 30_000)
        saveScreenshot("explore")

        // 进详情
        rule.onAllNodesWithTag("subject_card").onFirst().performClick()
        waitUntilText("演职人员", 30_000)
        saveScreenshot("detail")
    }

    private fun waitUntilText(text: String, timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            rule.mainClock.advanceTimeBy(32)
            if (rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()) return true
            Thread.sleep(300)
        }
        throw AssertionError("等待文本超时: $text")
    }

    private fun waitUntilTag(tag: String, timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            rule.mainClock.advanceTimeBy(32)
            if (rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()) return true
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
