doubean · 书影音精简版
====================

一个 [豆瓣][douban] 非官方客户端（Jetpack Compose），本仓库是
[Bumblebee202111/doubean-public][upstream] 的 fork，被裁剪成**只做电影 / 电视浏览**的极简版本。

界面只有一层：**书影音**。没有小组、没有动态、没有登录、没有个人中心、没有设置。

## 这一版做了什么

- **只保留书影音一个界面**，其它页面（小组 / 动态 / 个人中心 / 登录 / 设置 / 我的收藏 / 评论）连代码一起删除
- **去掉底部导航栏**、去掉顶部的读书 / 音乐 tab、去掉左上角头像按钮
- **电影 / 电视两个按钮与搜索图标并排**放在顶部栏右侧，点一下切换内容
- 首页内容区是圆形入口 + 分区卡片流：
  - 电影：正在上映 / 即将上映 / 豆瓣 Top 250
  - 电视：近期热门 / 国产剧 / 美剧 / 韩剧 / 日剧 / 动画
- **加入豆瓣的「找电影 / 找电视」**：按类型 / 地区 / 年份 / 排序筛选，三列海报流、滚动分页
- 影片详情页**只读**：海报、标题、评分、国家 / 类型 / 上映时间 / 片长、简介、演职人员、剧照、相关推荐
- 搜索页：输入即出建议（350ms 防抖），按电影 / 电视过滤

## 数据来源

直接用豆瓣公开的 Web / rexxar 接口，不需要登录态、不需要 apikey：

| 用途 | 接口 |
| --- | --- |
| 首页合集 | `m.douban.com/rexxar/api/v2/subject_collection/{movie_showing,movie_soon,movie_top250,tv_hot,tv_domestic,tv_american,tv_korean,tv_japanese,tv_animation}/items` |
| 影片详情 | `m.douban.com/rexxar/api/v2/{movie,tv}/{id}` |
| 演职人员 / 剧照 / 相关推荐 | `.../{id}/credits`、`.../{id}/photos`、`.../{id}/recommendations` |
| 榜单 | `movie.douban.com/j/chart/top_list` |
| 找片筛选 | `movie.douban.com/j/new_search_subjects` |
| 搜索建议 | `movie.douban.com/j/subject_suggest` |

rexxar 接口必须带 `Referer: https://m.douban.com/movie/`（电视用 `/tv/`），否则返回 403。

## 结构

```
core/network/           Ktor 客户端、Cookie（注入 apiKey）、DTO
core/network/api/       DoubanApiService：所有豆瓣接口
data/subject/           SubjectRepository（数据装配）+ SubjectMappers（DTO → 领域模型）
feature/subjects/       书影音首页
feature/explore/        找电影 / 找电视（筛选 + 分页）
feature/detail/         影片详情（只读）
feature/search/         搜索
feature/chart/          榜单
ui/                     DoubeanApp（手写返回栈）、通用组件
```

## 构建

需要 JDK 21（JDK 26 的 `jlink` 处理不了 `android-37` 的 `core-for-system-modules.jar`）与 Android SDK
（`compileSdk = 37`，即 `platforms;android-37.0`）。

```bash
# local.properties
sdk.dir=/path/to/android-sdk

./gradlew :app:assembleDebug      # 产物 app/build/outputs/apk/debug/
./gradlew :app:testDebugUnitTest  # 网络冒烟 + Robolectric 界面测试（会把截图落到 app/build/screenshots/）
```

测试覆盖：真实豆瓣接口取数（首页 9 个合集 / 详情 / 演职人员 / 剧照 / 榜单 / 筛选 / 搜索建议）、
首页渲染断言（有「电影」「电视」，没有「读书」「音乐」「小组」）、首页 → 找电影 → 详情 的导航链路。

## 已知限制

- 只读：没有「想看 / 看过 / 评分 / 短评」等需要登录态的操作
- 豆瓣接口是公开 Web 接口，字段与可用性由豆瓣决定，可能随时变化
- 没有离线缓存

## 致谢与许可

界面组件与领域模型来自上游 [Bumblebee202111/doubean-public][upstream]，遵循其
[LICENSE](LICENSE)。豆瓣名称、内容与接口归豆瓣所有，本项目与豆瓣官方无关。

[douban]: https://www.douban.com/
[upstream]: https://github.com/Bumblebee202111/doubean-public
