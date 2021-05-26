package com.geejoe.lifecycleeventbus.demo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.geejoe.lifecycleeventbus.EventObserver
import com.geejoe.lifecycleeventbus.LifecycleEventBus

class MainActivity : AppCompatActivity() {

    private val observer = object : EventObserver<LoginEvent> {
        override fun onEvent(event: LoginEvent) {
            // handle event
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        LifecycleEventBus.observe(this, LoginEvent::class.java, object : EventObserver<LoginEvent> {
            override fun onEvent(event: LoginEvent) {
                // handle event
            }
        })

        LifecycleEventBus.observeForever(LoginEvent::class.java, observer)
        LifecycleEventBus.sendEvent(LoginEvent("123456789"))
    }

    override fun onDestroy() {
        super.onDestroy()
        LifecycleEventBus.removeObserver(observer)
    }
}