package com.geejoe.lifecycleeventbus.demo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.geejoe.lifecycleeventbus.EventObserver
import com.geejoe.lifecycleeventbus.LifecycleEventBus
import com.geejoe.lifecycleeventbus.ThreadMode
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private val threadPool = Executors.newFixedThreadPool(10)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val observerSubThread = object : EventObserver<LoginEvent> {
            override fun onEvent(event: LoginEvent) {
                println("onEvent --> ${Thread.currentThread()} $event")
            }
        }
        val observerMainThread = object : EventObserver<LoginEvent> {
            override fun onEvent(event: LoginEvent) {
                println("onEvent --> ${Thread.currentThread()} $event")
            }
        }
        LifecycleEventBus.observe(this, LoginEvent::class.java, observerSubThread)
        LifecycleEventBus.observe(this, LoginEvent::class.java, observerMainThread, ThreadMode.MAIN)

        threadPool.execute {
            LifecycleEventBus.sendEvent(LoginEvent("hello"))
        }
    }

}