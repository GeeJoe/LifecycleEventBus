package com.geejoe.lifecycleeventbus

import android.os.Handler
import android.os.Looper


/**
 * Created by zhiyueli on 2021/5/26.
 * zhiyueli.dev@gmail.com
 */
object ThreadManager {

    private val mainHandler by lazy {
        Handler(Looper.getMainLooper())
    }

    fun runOnMainThread(block: () -> Unit) {
        if (isMainThread()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    private fun isMainThread(): Boolean {
        return Looper.myLooper() == Looper.getMainLooper()
    }
}