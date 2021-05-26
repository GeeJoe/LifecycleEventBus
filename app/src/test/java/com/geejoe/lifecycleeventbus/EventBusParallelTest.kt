package com.geejoe.lifecycleeventbus

import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import org.hamcrest.MatcherAssert
import org.hamcrest.core.IsEqual
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.Executors

/**
 * Created by zhiyueli on 5/17/21.
 * zhiyueli.dev@gmail.com
 */
@Config(sdk = [Build.VERSION_CODES.O_MR1])
@RunWith(RobolectricTestRunner::class)
class EventBusParallelTest {

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
        mockLifecycleCreate()
    }

    private fun mockLifecycleCreate() {
        mockLifecycleOwner.handleLifecycle(Lifecycle.Event.ON_CREATE)
    }

    private fun mockLifecycleDestroy() {
        mockLifecycleOwner.handleLifecycle(Lifecycle.Event.ON_DESTROY)
    }

    private val threadPool = Executors.newFixedThreadPool(10)

    /**
     * 测试多线程发送事件是否能正常接收事件
     */
    @Test
    fun testEventBusObserve() {
        // given
        val observerCount = 10
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

        threadPool.execute {
            LifecycleEventBus.sendEvent(observerCount)
        }

        threadPool.execute {
            LifecycleEventBus.sendEvent("1234")
        }


        // then
        threadPool.execute {
            // make sure all event was sent
            Thread.sleep(4000L)
            MatcherAssert.assertThat(receivedResultForInt, IsEqual(observerCount * observerCount))
            MatcherAssert.assertThat(receivedResultForString.length, IsEqual(4 * observerCount))
        }
    }

    /**
     * 测试多线程发送事件并且期间生命周期结束是否能正常
     */
    @Test
    fun testEventBusObserveWhenParallel() {
        val observer = object : EventObserver<String> {
            override fun onEvent(event: String) {

            }
        }
        repeat(100) {
            mockLifecycleCreate()
            LifecycleEventBus.observe(mockLifecycleOwner, String::class.java, observer)
            threadPool.execute {
                LifecycleEventBus.sendEvent("1234")
            }
            mockLifecycleDestroy()
        }
    }

    @After
    fun after() {
        mockLifecycleDestroy()
    }
}