package com.geejoe.lifecycleeventbus

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set


/**
 * Created by zhiyueli on 5/17/21.
 * zhiyueli.dev@gmail.com
 */
internal typealias EVENT = Any

object LifecycleEventBus {

    private val observerMap =
        mutableMapOf<Class<*>, ConcurrentHashMap<EventObserver<*>, ObserverWrapper>>()

    /** For unit test primarily. */
    fun clearAll() {
        observerMap.clear()
    }

    fun <T : EVENT> sendEvent(event: T) {
        val eventType = event::class.java
        observerMap[eventType]?.forEach { (_, wrapper) ->
            wrapper.observer.dispatchEvent(event)
        }
    }

    fun <T : EVENT> observe(
        owner: LifecycleOwner,
        eventType: Class<T>,
        observer: EventObserver<T>
    ) {
        addObserver(eventType, LifecycleBoundObserver(owner, observer))
    }

    fun <T : EVENT> observeForever(eventType: Class<T>, observer: EventObserver<T>) {
        addObserver(eventType, ObserverWrapper(observer))
    }

    fun <T : EVENT> removeObserver(observer: EventObserver<T>) {
        observerMap.forEach { (_, observers) ->
            val wrapper = observers.remove(observer)
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
        observer: EventObserver<*>
    ) : ObserverWrapper(observer), LifecycleEventObserver {

        init {
            owner.lifecycle.addObserver(this)
        }

        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            val currentState: Lifecycle.State = source.lifecycle.currentState
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
    private open class ObserverWrapper(val observer: EventObserver<*>) {
        open fun detachObserver() {}
    }

}