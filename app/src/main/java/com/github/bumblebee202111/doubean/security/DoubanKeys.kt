package com.github.bumblebee202111.doubean.security

/**
 * 豆瓣 frodo 客户端的公开 apiKey，用于 CookieStorage 注入。
 * 本项目只访问豆瓣 Web 端（movie.douban.com / m.douban.com rexxar）接口，
 * 不依赖 frodo 私有接口，因此该值仅用于保持 UA/Cookie 形态一致。
 */
const val DOUBAN_API_KEY = "0dad551ec0f84ed02907ff5c42e8ec70"
