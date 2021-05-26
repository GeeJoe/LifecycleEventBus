# LifecycleEventBus
An EventBus that support Lifecycle. The Lifecycle-Aware Observers can be auto detached from the EventBus before the Lifecycle is going to destroy.

中文文档(CN)：[LifecycleEventBus.md](https://github.com/GeeJoe/LifecycleEventBus/blob/main/LifecycleEventBus.md)

## Use in 3 Steps
### 1. Define events
Any object can be an event
```kotlin
data class LoginEvent(val userId: String)
```

### 2. Register observers
#### register via a lifecycle owner (can be an Activity/Fragment...)
```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // The observer will be removed when onDestroy
        LifecycleEventBus.observe(this, LoginEvent::class.java, object : EventObserver<LoginEvent> {
            override fun onEvent(event: LoginEvent) {
                // handle event
            }
        })
    }
}
```

#### register/unregister manually
```kotlin
class MainActivity : AppCompatActivity() {
    
    private val observer = object : EventObserver<LoginEvent> {
        override fun onEvent(event: LoginEvent) {
            // handle event
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        LifecycleEventBus.observeForever(LoginEvent::class.java, observer)
    }

    override fun onDestroy() {
        super.onDestroy()
        LifecycleEventBus.removeObserver(observer)
    }
}
```

### 3. Send events
```kotlin
LifecycleEventBus.sendEvent(LoginEvent("123456789"))
```

## ThreadMode
The Observer's `onEvent()` is running on the thread which `LifecycleEventBus.sendEvent` is invoked by defalut.
You can pass the ThreadMode as a param to `observer()` or `observeForever` to specify which thread the observer should run on.

set ThradMode to `ThreadMode.MAIN` and the observer will run on main thread.
```kotlin
LifecycleEventBus.observe(this, LoginEvent::class.java, observerSubThread, ThreadMode.MAIN)
```
set ThradMode to `ThreadMode.ORIGIN` and the observer will run on the thread which `LifecycleEventBus.sendEvent` is invoked. This is the default ThreadMode.

```kotlin
LifecycleEventBus.observe(this, LoginEvent::class.java, observerSubThread)
LifecycleEventBus.observe(this, LoginEvent::class.java, observerSubThread, ThreadMode.ORIGIN)
```
