package com.github.bumblebee202111.doubean.core.network

/**
 * 豆瓣对高频请求不会返回空列表，而是回一个对象：
 * `{"msg":"检测到有异常请求从您的IP发出，请登录再试!","r":1}`
 *
 * 不识别的话会当成「没有数据」，界面上就变成空白，看不出是被限流了。
 */
class DoubanThrottledException(val serverMessage: String) : Exception(serverMessage)
