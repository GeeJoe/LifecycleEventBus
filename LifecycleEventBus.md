## Overview

- 支持绑定 Lifecycle，能够在生命周期 onDestroy 的时候自动移除监听，可与 Android Jetpack 中的 Lifecycle 组件无缝衔接
- 支持监听者线程切换
- 支持手动注册/反注册监听器
- 代码精简，只有 100 行左右

相比 EventBus/RxBus 优势：

- EventBus 中事件分发采用反射，LifecycleEventBus 以接口形式回调，不存在反射
- RxBus 依赖 RxJava，对包大小有影响，LifecycleEventBus 代码精简，只有 100 行左右
- LifecycleEventBus 具备 EventBus 和 RxBus 没有的「生命周期感知能力」

### Sample

```kotlin
// difine an event
data class LoginEvent(val userId: String)
// define an observer 
val observer = object : EventObserver<LoginEvent> {
            override fun onEvent(event: LoginEvent) {
                println("onEvent --> $event")
            }
        }
// add observer with lifecycleOwner.
// the observer will be removed when lifecycle goes to destroy
LifecycleEventBus.observe(this, LoginEvent::class.java, observer)
// send event
LifecycleEventBus.sendEvent(LoginEvent("12345"))
```

## 手把手教你如何用 100 行代码实现一个有生命周期感知能力的 EventBus

事件总线是一个项目开发中必不可少的能力，市面上也有几个非常有名的事件库，比如 EventBus 以及基于 RxJava 的 RxBus 等

但是他们在使用的时候都必须要手动注册/反注册监听，我们能否实现一个不需要手动反注册的事件总线呢，换句话说，我们如何实现一个能够在生命周期 destroy 的时候自定解绑监听的事件总线呢

### 1. 一个 EventBus 的简易模型

首先简单构思一下我们的 EventBus —— 任意对象都可以作为事件被发送，通过对象的 Class 类型来对事件进行订阅和分发

我们先定义一下 `LifecycleEventBus` 的几个基础方法（暂不考虑生命周期感知能力）

```kotlin
object LifecycleEventBus {
    // 添加监听
    fun <T : Any> observe(eventType: Class<T>, observer: EventObserver<T>) {}
  
  	// 移除监听
    fun <T : EVENT> removeObserver(observer: EventObserver<T>) {}
  
  	// 发送事件
  	fun <T: Any> sendEvent(event: T) {}
}
```

抽象的监听者接口 —— `EventObserver`

```kotlin
interface EventObserver<T : Any> {
  	// 收到事件的时候回调函数，业务方在这里实现对事件的处理逻辑
    fun onEvent(event: T)
}
```

这样一个简易的 EventBus 就搭建好了，核心思路是：以事件的 Class 类型作为 key，对应的 Observer 作为 value，将此 key-value 存储起来，在发送事件的时候，根据传入的 event 的 Class 类型，找到对应的 Observer 然后调用其 `onEvent()` 方法来分发事件，实现代码如下：

```kotlin
private val observerMap =
        mutableMapOf<Class<*>, MutableList<EventObserver<*>>>()

private fun addObserver(eventType: Class<*>, observer: EventObserver<*>) {
    val observers = observerMap[eventType] ?: MutableList()
    if (observerMap[eventType] == null) {
        observerMap[eventType] = observers
    }
  	if (!observers.contains(observer)) {
      	observers.add(observer)
    }
}

fun <T: Any> sendEvent(event: T) {
  	val eventType = event::class.java
    observerMap[eventType]?.forEach { observer ->
    		observer.onEvent(event)
    }
}

```

### 2. 让 Observer 具备生命周期感知能力

借助 `androidx-lifecycle` 中的 `LifecycleEventObserver` 我们可以让 `EventObserver` 具备生命周期感知能力

为了兼容无生命周期的组件，我们同时也要保留非生命周期感知能力的 Observer，为此，我们可以抽象一个 Observer 包装器 —— `ObserverWrapper`

```kotlin
open class ObserverWrapper(val observer: EventObserver<*>) {
	// 用于解绑生命周期，后续介绍	
  open fun detachObserver() {}
}
```

实现一个生命周期感知能力的 Observer —— `LifecycleBoundObserver`

```kotlin
private class LifecycleBoundObserver(
        private val owner: LifecycleOwner,
        observer: EventObserver<*>
    ) : ObserverWrapper(observer), LifecycleEventObserver {

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

  			// 在移除监听 removeObserver 中回调被移除的 observer 的
  			// detachObserver 方法来解绑对生命周期的监听
        override fun detachObserver() {
            super.detachObserver()
            owner.lifecycle.removeObserver(this)
        }
    }
```

然后添加监听的地方修改如下：

```kotlin
// 由于我们使用了包装类 ObserverWrapper，但是对于外部而言该包装类是不可见的，为了能够
// 正常移除 observer，我们需要建立原始 observer 和 ObserverWrapper 的映射关系
// 这里表现为一个 HashMap
private val observerMap =
        mutableMapOf<Class<*>, ConcurrentHashMap<EventObserver<*>, ObserverWrapper>>()

private fun addObserver(eventType: Class<*>, observerWrapper: ObserverWrapper) {
        val observers = observerMap[eventType] ?: ConcurrentHashMap()
        if (observerMap[eventType] == null) {
            observerMap[eventType] = observers
        }
        observers.putIfAbsent(observerWrapper.observer, observerWrapper)
    }
```

同时在移除监听器的时候也要把对生命周期的监听移除：

```kotlin
fun <T : Any> removeObserver(observer: EventObserver<T>) {
        observerMap.forEach { (_, observers) ->
            val wrapper = observers.remove(observer)
            // 移除监听的时候，同时也需要移除 lifecycle 的监听
            wrapper?.detachObserver()
        }
    }
```

如此，我们就实现了一个具备生命周期感知能力的 Observer，在使用的时候传入对应的 `LifecycleOwner` 就可实现自动解绑监听

```kotlin 
LifecycleEventBus.observe(lifecycleOwner, MyEvent::class.java, observer)
```

而对于不需要生命周期感知能力的 Observer，我们直接使用 `ObserverWrapper` 就可以了，代码很简单，这里不再赘述

### 3. 支持线程切换

当前的实现 observer 将运行在 `sendEvent()` 所在的线程，很多时候，我们可能在子线程发送事件，但是期望在主线程监听，那么我们就需要实现线程切换能力，让 Observer 可以运行在指定的线程上

定义 Enum 线程模式 —— `ThreadMode`

```kotlin
/** 
* ORIGIN: Observer 将运行在发送事件所在的线程
* MAIN: Observer 将运行在主线程
*/
enum class ThreadMode {
    ORIGIN, MAIN
}
```

给 `observe()` 方法增加参数，默认是 `ThreadMode.ORIGIN`

```kotlin
fun <T : Any> observe(
        owner: LifecycleOwner,
        eventType: Class<T>,
        observer: EventObserver<T>,
        threadMode: ThreadMode = ThreadMode.ORIGIN
    ) {
        addObserver(eventType, LifecycleBoundObserver(owner, observer, threadMode))
    }
```

把 `threadMode` 传递到 `observer` 中，当分发事件的时候，判断如果 `threadMode` 为 `ThreadMode.MAIN` 切换到主线程即可

```kotlin
 if (threadMode == ThreadMode.MAIN) {
     ThreadManager.runOnMainThread {
          onEvent(it)
     }
  } else {
     onEvent(it)
  }
```

```kotlin
object ThreadManager {

    private val mainHandler by lazy {
        Handler(Looper.getMainLooper())
    }

    fun runOnMainThread(block: () -> Unit) {
        if (isMainThread()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    private fun isMainThread(): Boolean {
        return Looper.myLooper() == Looper.getMainLooper()
    }
}
```



至此，一个完整的具备生命周期感知能力的 EventBus 就完成了，整体代码 100 左右，十分精简。源码详情请到 GitHub 自行查看：[LifecycleEventBus](https://github.com/GeeJoe/LifecycleEventBus)




