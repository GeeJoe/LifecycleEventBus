package com.geejoe.lifecycleeventbus

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set


/**
 * Created by zhiyueli on 5/17/21.
 * zhiyueli.dev@gmail.com
 *
 * 具备生命周期感知能力的 EventBus，能够实现根据生命周期自动解绑监听者
 * 和 Android Jetpack Lifecycle 相关组件无缝衔接
 */
internal typealias EVENT = Any

object LifecycleEventBus {

    private val observerMap =
        mutableMapOf<Class<*>, ConcurrentHashMap<EventObserver<*>, ObserverWrapper>>()

    /** For unit test primarily. */
    fun clearAll() {
        observerMap.clear()
    }

    /**
     * 发送事件
     *
     * 任意一个对象都可作为一个事件被发送
     * LifecycleEventBus 将根据事件的 Class 类型来找对应注册了的 Observer 进行事件通知
     */
    fun <T : EVENT> sendEvent(event: T) {
        val eventType = event::class.java
        observerMap[eventType]?.forEach { (_, wrapper) ->
            wrapper.observer.dispatchEvent(event, wrapper.threadMode)
        }
    }

    /**
     * 注册监听者
     *
     * 这里注册的 Observer 将具备生命周期感知能力，能够在生命周期处于 [Lifecycle.State.DESTROYED]
     * 时自定解绑，可与 Android Jetpack Lifecycle 组件无缝衔接
     *
     *
     * @param owner: 生命周期 owner，可以是 Activity/Fragment
     * @param eventType: 事件类型，只有发送相同类型的事件才能够被接收到
     * @param observer: 监听者，在这里处理接收到事件的逻辑
     * @param threadMode: 指定 observer 运行在那个线程，详见 [ThreadMode], 默认为 [ThreadMode.ORIGIN]
     */
    fun <T : EVENT> observe(
        owner: LifecycleOwner,
        eventType: Class<T>,
        observer: EventObserver<T>,
        threadMode: ThreadMode = ThreadMode.ORIGIN
    ) {
        ThreadManager.runOnMainThread {
            addObserver(eventType, LifecycleBoundObserver(owner, observer, threadMode))
        }
    }

    /**
     * 注册监听者
     *
     * 使用该方法注册的 observer 将在整个应用进程期间存在，必要的时候需要手动调用 [removeObserver] 移除监听
     *
     * @param threadMode: 指定 observer 运行在那个线程，详见 [ThreadMode], 默认为 [ThreadMode.ORIGIN]
     * @param eventType: 事件类型，只有发送相同类型的事件才能够被接收到
     * @param observer: 监听者，在这里处理接收到事件的逻辑
     */
    fun <T : EVENT> observeForever(
        eventType: Class<T>,
        observer: EventObserver<T>,
        threadMode: ThreadMode = ThreadMode.ORIGIN
    ) {
        addObserver(eventType, ObserverWrapper(observer, threadMode))
    }

    /**
     * 移除监听者
     *
     * 与 [observeForever] 配套使用；如果使用 [observer] 方法注册的监听者，无需手动调用本方法
     */
    fun <T : EVENT> removeObserver(observer: EventObserver<T>) {
        observerMap.forEach { (_, observers) ->
            val wrapper = observers.remove(observer)
            // 移除监听的时候，同时也需要移除 lifecycle 的监听
            wrapper?.detachObserver()
        }
    }

    private fun addObserver(eventType: Class<*>, observerWrapper: ObserverWrapper) {
        val observers = observerMap[eventType] ?: ConcurrentHashMap()
        if (observerMap[eventType] == null) {
            observerMap[eventType] = observers
        }
        observers.putIfAbsent(observerWrapper.observer, observerWrapper)
    }

    /**
     * 具备生命周期感知能力的 Observer，能够在生命周期结束的时候主动释放监听
     */
    private class LifecycleBoundObserver(
        private val owner: LifecycleOwner,
        observer: EventObserver<*>,
        threadMode: ThreadMode
    ) : ObserverWrapper(observer, threadMode), LifecycleEventObserver {

        init {
            // 监听生命周期的变化
            owner.lifecycle.addObserver(this)
        }

        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            val currentState: Lifecycle.State = source.lifecycle.currentState
            // 当生命周期即将销毁的时候，移除监听器
            if (currentState == Lifecycle.State.DESTROYED) {
                removeObserver(observer)
                return
            }
        }

        override fun detachObserver() {
            super.detachObserver()
            owner.lifecycle.removeObserver(this)
        }
    }

    /**
     * Observer 包装器，其子类 [LifecycleBoundObserver] 具备生命周期感知能力
     */
    private open class ObserverWrapper(val observer: EventObserver<*>, val threadMode: ThreadMode) {
        open fun detachObserver() {}
    }

}