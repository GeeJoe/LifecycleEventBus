package com.geejoe.lifecycleeventbus


/**
 * Created by zhiyueli on 5/17/21.
 * zhiyueli.dev@gmail.com
 *
 * 事件监听器接口，业务方需要实现 [onEvent] 方法处理接收到的事件
 */
interface EventObserver<T : EVENT> {
    fun onEvent(event: T)
}

@Suppress("UNCHECKED_CAST")
internal fun <T : EVENT> EventObserver<T>.dispatchEvent(event: EVENT, threadMode: ThreadMode) {
    (event as? T)?.let {
        if (threadMode == ThreadMode.MAIN) {
            ThreadManager.runOnMainThread {
                onEvent(it)
            }
        } else {
            onEvent(it)
        }
    }
}
