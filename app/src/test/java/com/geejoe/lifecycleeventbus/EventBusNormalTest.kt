package com.geejoe.lifecycleeventbus

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual
import org.junit.After
import org.junit.Test

/**
 * Created by zhiyueli on 5/17/21.
 * zhiyueli.dev@gmail.com
 */
class EventBusNormalTest {

    companion object {
        private const val TARGET_EVENT_VALUE = "Hello World"
        private const val ORIGIN_EVENT_VALUE = "Origin"
    }

    /**
     * 测试基本的发送事件
     */
    @Test
    fun testEventBusObserveForever() {
        // given
        var eventReceived: String = ORIGIN_EVENT_VALUE
        val observer = object : EventObserver<String> {
            override fun onEvent(event: String) {
                eventReceived = event
            }
        }
        LifecycleEventBus.observeForever(String::class.java, observer)
        // when
        LifecycleEventBus.sendEvent(TARGET_EVENT_VALUE)
        // then
        assert(eventReceived == TARGET_EVENT_VALUE)
    }

    /**
     * 测试移除 observer 之后是否还会接收到事件
     */
    @Test
    fun testEventBusObserveForeverAfterRemove() {
        // given
        var eventReceived: String = ORIGIN_EVENT_VALUE
        val observer = object : EventObserver<String> {
            override fun onEvent(event: String) {
                eventReceived = event
            }
        }
        LifecycleEventBus.observeForever(String::class.java, observer)
        LifecycleEventBus.removeObserver(observer)
        // when
        LifecycleEventBus.sendEvent(TARGET_EVENT_VALUE)
        // then
        assert(eventReceived == ORIGIN_EVENT_VALUE)
    }

    /**
     * 测试注册多个相同事件类型的 observer 能否都能收到事件
     */
    @Test
    fun testEventBusObserveForeverForMultiObservers() {
        // given
        val observerCount = 1000
        var receivedResult = 0
        repeat(observerCount) {
            val observer = object : EventObserver<Integer> {
                override fun onEvent(event: Integer) {
                    receivedResult += event.toInt()
                }
            }
            LifecycleEventBus.observeForever(Integer::class.java, observer)
        }
        // when
        LifecycleEventBus.sendEvent(observerCount)
        // then
        assertThat(receivedResult, IsEqual(observerCount * observerCount))
    }

    /**
     * 测试发送非相同类型的 observer 能否收到事件
     */
    @Test
    fun testEventBusObserveWhenSendOtherEvent() {
        // given
        var eventReceived: String = ORIGIN_EVENT_VALUE
        val observer = object : EventObserver<String> {
            override fun onEvent(event: String) {
                eventReceived = event
            }
        }
        LifecycleEventBus.observeForever(String::class.java, observer)
        LifecycleEventBus.removeObserver(observer)
        // when
        LifecycleEventBus.sendEvent(10086)
        // then
        assert(eventReceived == ORIGIN_EVENT_VALUE)
    }

    /**
     * 测试注册多个不同事件类型的 observer 能否都能收到事件
     */
    @Test
    fun testEventBusObserveForeverForMultiDifferentTypeObservers() {
        // given
        val observerCount = 1000
        var receivedResultForInt = 0
        var receivedResultForString = ""
        repeat(observerCount) {
            val observerInt = object : EventObserver<Integer> {
                override fun onEvent(event: Integer) {
                    receivedResultForInt += event.toInt()
                }
            }
            LifecycleEventBus.observeForever(Integer::class.java, observerInt)

            val observerString = object : EventObserver<String> {
                override fun onEvent(event: String) {
                    receivedResultForString += event
                }
            }
            LifecycleEventBus.observeForever(String::class.java, observerString)
        }
        // when
        LifecycleEventBus.sendEvent(observerCount)
        LifecycleEventBus.sendEvent("1234")
        // then
        assertThat(receivedResultForInt, IsEqual(observerCount * observerCount))
        assertThat(receivedResultForString.length, IsEqual(4 * observerCount))
    }

    @After
    fun after() {
        LifecycleEventBus.clearAll()
    }
}