# LifecycleEventBus
An EventBus that support Lifecycle. The observers can auto detach from the EventBus before the Lifecycle is going to destroy.

## Use in 3 Steps
### Define events
Any object can be an event
```kotlin
data class LoginEvent(val userId: String)
```

### Register observers
#### 1. register via a lifecycle owner (can be an Activity/Fragment...)
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

#### 2. register/unregister manually
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

### Send events
```kotlin
LifecycleEventBus.sendEvent(LoginEvent("123456789"))
```
