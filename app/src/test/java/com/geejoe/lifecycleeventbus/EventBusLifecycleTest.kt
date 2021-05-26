package com.geejoe.lifecycleeventbus

import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Created by zhiyueli on 5/17/21.
 * zhiyueli.dev@gmail.com
 */
@Config(sdk = [Build.VERSION_CODES.O_MR1])
@RunWith(RobolectricTestRunner::class)
class EventBusLifecycleTest {

    companion object {
        private const val TARGET_EVENT_VALUE = "Hello World"
        private const val ORIGIN_EVENT_VALUE = "Origin"
    }

    private class MockLifeCycleOwner : LifecycleOwner {
        private val registry = LifecycleRegistry(this)

        override fun getLifecycle(): Lifecycle {
            return registry
        }

        fun handleLifecycle(event: Lifecycle.Event) {
            registry.handleLifecycleEvent(event)
        }
    }

    private val mockLifecycleOwner = MockLifeCycleOwner()

    @Before
    fun before() {
        mockLifecycleOwner.handleLifecycle(Lifecycle.Event.ON_CREATE)
    }

    private fun mockLifecycleDestroy() {
        mockLifecycleOwner.handleLifecycle(Lifecycle.Event.ON_DESTROY)
    }

    /**
     * 测试基本的发送事件
     */
    @Test
    fun testEventBusObserve() {
        // given
        var eventReceived: String = ORIGIN_EVENT_VALUE
        val observer = object : EventObserver<String> {
            override fun onEvent(event: String) {
                eventReceived = event
            }
        }
        LifecycleEventBus.observe(mockLifecycleOwner, String::class.java, observer)
        // when
        LifecycleEventBus.sendEvent(TARGET_EVENT_VALUE)
        // then
        assert(eventReceived == TARGET_EVENT_VALUE)
    }

    /**
     * 测试生命周期结束之后是否还会接收到事件
     */
    @Test
    fun testEventBusObserveAfterRemove() {
        // given
        var eventReceived: String = ORIGIN_EVENT_VALUE
        val observer = object : EventObserver<String> {
            override fun onEvent(event: String) {
                eventReceived = event
            }
        }
        LifecycleEventBus.observe(mockLifecycleOwner, String::class.java, observer)
        mockLifecycleDestroy()
        // when
        LifecycleEventBus.sendEvent(TARGET_EVENT_VALUE)
        // then
        assert(eventReceived == ORIGIN_EVENT_VALUE)
    }

    /**
     * 测试注册多个相同事件类型的 observer 能否都能收到事件
     */
    @Test
    fun testEventBusObserveForMultiObservers() {
        // given
        val observerCount = 1000
        var receivedResult = 0
        repeat(observerCount) {
            val observer = object : EventObserver<Integer> {
                override fun onEvent(event: Integer) {
                    receivedResult += event.toInt()
                }
            }
            LifecycleEventBus.observe(mockLifecycleOwner, Integer::class.java, observer)
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
        LifecycleEventBus.observe(mockLifecycleOwner, String::class.java, observer)
        mockLifecycleOwner.handleLifecycle(Lifecycle.Event.ON_DESTROY)
        // when
        LifecycleEventBus.sendEvent(10086)
        // then
        assert(eventReceived == ORIGIN_EVENT_VALUE)
    }

    /**
     * 测试注册多个不同事件类型的 observer 能否都能收到事件
     */
    @Test
    fun testEventBusObserveForMultiDifferentTypeObservers() {
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
            LifecycleEventBus.observe(mockLifecycleOwner, Integer::class.java, observerInt)

            val observerString = object : EventObserver<String> {
                override fun onEvent(event: String) {
                    receivedResultForString += event
                }
            }
            LifecycleEventBus.observe(mockLifecycleOwner, String::class.java, observerString)
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
        mockLifecycleDestroy()
    }
}