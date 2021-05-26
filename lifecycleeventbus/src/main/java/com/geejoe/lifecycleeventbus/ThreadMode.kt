package com.geejoe.lifecycleeventbus


/**
 * Created by zhiyueli on 2021/5/26.
 * zhiyueli@tencent.com
 *
 * 线程模式
 *
 * ORIGIN: Observer 将运行在发送事件所在的线程
 * MAIN: Observer 将运行在主线程
 */
enum class ThreadMode {
    ORIGIN, MAIN
}