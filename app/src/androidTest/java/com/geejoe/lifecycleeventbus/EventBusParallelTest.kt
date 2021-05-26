package com.geejoe.lifecycleeventbus

import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.geejoe.lifecycleeventbus.demo.LoginEvent
import org.hamcrest.MatcherAssert
import org.hamcrest.core.IsEqual
import org.hamcrest.core.IsNot
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Executors

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
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
        ThreadManager.runOnMainThread {
            mockLifecycleOwner.handleLifecycle(Lifecycle.Event.ON_CREATE)
        }
    }

    private fun mockLifecycleDestroy() {
        ThreadManager.runOnMainThread {
            mockLifecycleOwner.handleLifecycle(Lifecycle.Event.ON_DESTROY)
        }
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

        // make sure all observer was registered
        Thread.sleep(4000L)

        threadPool.execute {
            LifecycleEventBus.sendEvent(observerCount)
        }

        threadPool.execute {
            LifecycleEventBus.sendEvent("1234")
        }


        // make sure all event was sent
        Thread.sleep(1000L)
        MatcherAssert.assertThat(receivedResultForInt, IsEqual(observerCount * observerCount))
        MatcherAssert.assertThat(receivedResultForString.length, IsEqual(4 * observerCount))
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

    /**
     * 测试子线程 observer 和发送事件
     */
    @Test
    fun testEventBusOnSubThread() {
        var count = 0
        var result = ""
        val observer = object : EventObserver<LoginEvent> {
            override fun onEvent(event: LoginEvent) {
                MatcherAssert.assertThat(event.userId, IsEqual("1234_$count"))
                count++
                println("onEvent---> ${event.userId} ${Thread.currentThread()}")
                result = event.userId
            }
        }
        threadPool.execute {
            LifecycleEventBus.observe(mockLifecycleOwner, LoginEvent::class.java, observer)
        }

        threadPool.execute {
            // make sure observer is registered
            Thread.sleep(1000)
            repeat(100) {
                LifecycleEventBus.sendEvent(LoginEvent("1234_$it"))
                println("send---> ${Thread.currentThread()}")
            }
        }
        // make sure sub thread is finish running
        Thread.sleep(5000)
        MatcherAssert.assertThat(result, IsEqual("1234_99"))
    }

    /**
     * 测试线程模式
     */
    @Test
    fun testEventBusThreadMode() {
        var resultForMainThread: Looper? = null
        var resultForSubThread: Looper? = null
        val observerSubThread = object : EventObserver<LoginEvent> {
            override fun onEvent(event: LoginEvent) {
                println("onEvent --> ${Thread.currentThread()} $event")
                resultForSubThread = Looper.myLooper()
            }
        }
        val observerMainThread = object : EventObserver<LoginEvent> {
            override fun onEvent(event: LoginEvent) {
                println("onEvent --> ${Thread.currentThread()} $event")
                resultForMainThread = Looper.myLooper()
            }
        }
        LifecycleEventBus.observe(mockLifecycleOwner, LoginEvent::class.java, observerSubThread)
        LifecycleEventBus.observe(mockLifecycleOwner, LoginEvent::class.java, observerMainThread, ThreadMode.MAIN)

        // make sure observer was registered
        Thread.sleep(1000)

        LifecycleEventBus.sendEvent(LoginEvent("hello"))

        // make sure event as sent
        Thread.sleep(1000)
        MatcherAssert.assertThat(resultForMainThread, IsEqual(Looper.getMainLooper()))
        MatcherAssert.assertThat(resultForSubThread, IsNot(IsEqual(Looper.getMainLooper())))
    }

    @After
    fun after() {
        mockLifecycleDestroy()
    }
}