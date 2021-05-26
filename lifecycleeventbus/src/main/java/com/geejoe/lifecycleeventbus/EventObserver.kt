package com.geejoe.lifecycleeventbus


/**
 * Created by zhiyueli on 5/17/21.
 * zhiyueli.dev@gmail.com
 */
interface EventObserver<T : EVENT> {

    @Suppress("UNCHECKED_CAST")
    fun dispatchEvent(event: EVENT) {
        (event as? T)?.let {
            onEvent(it)
        }
    }

    fun onEvent(event: T)
}