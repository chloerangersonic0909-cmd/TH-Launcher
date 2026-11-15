package com.thlauncher.app.core.telemetry

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.view.Choreographer
import java.util.concurrent.atomic.AtomicBoolean

/** Snapshot of Android app memory usage. Values are measured in MiB. */
data class MemorySnapshot(
    val appPssMb: Int,
    val javaHeapUsedMb: Int,
    val javaHeapMaxMb: Int,
    val deviceAvailableMb: Int,
    val deviceLowMemory: Boolean
)

/** Live FPS estimate based on rendered frame callbacks; start/stop with the visible game surface. */
class FpsCounter(
    private val onFpsUpdated: (Int) -> Unit,
    private val choreographer: Choreographer = Choreographer.getInstance()
) : Choreographer.FrameCallback {
    private var windowStartNanos = 0L
    private var frameCount = 0
    private val running = AtomicBoolean(false)

    fun start() {
        if (running.compareAndSet(false, true)) {
            windowStartNanos = 0L
            frameCount = 0
            choreographer.postFrameCallback(this)
        }
    }

    fun stop() {
        running.set(false)
        choreographer.removeFrameCallback(this)
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!running.get()) return
        if (windowStartNanos == 0L) windowStartNanos = frameTimeNanos
        frameCount++
        val elapsed = frameTimeNanos - windowStartNanos
        if (elapsed >= 1_000_000_000L) {
            onFpsUpdated((frameCount * 1_000_000_000L / elapsed).toInt())
            windowStartNanos = frameTimeNanos
            frameCount = 0
        }
        if (running.get()) choreographer.postFrameCallback(this)
    }
}

/** Reads real process and device memory counters; call away from latency-sensitive UI work. */
class DevicePerformanceMonitor(context: Context) {
    private val appContext = context.applicationContext
    private val activityManager =
        appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    fun readMemorySnapshot(): MemorySnapshot {
        val memoryInfo = ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)
        val debugInfo = Debug.MemoryInfo().also(Debug::getMemoryInfo)
        val runtime = Runtime.getRuntime()
        val mib = 1024L * 1024L
        return MemorySnapshot(
            appPssMb = (debugInfo.totalPss.toLong() / 1024L).toInt(),
            javaHeapUsedMb = ((runtime.totalMemory() - runtime.freeMemory()) / mib).toInt(),
            javaHeapMaxMb = (runtime.maxMemory() / mib).toInt(),
            deviceAvailableMb = (memoryInfo.availMem / mib).toInt(),
            deviceLowMemory = memoryInfo.lowMemory
        )
    }
}
