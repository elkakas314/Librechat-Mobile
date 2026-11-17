package com.garfiec.librechat.feature.agents.devicecontrol

import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSystemFreeSize
import platform.Foundation.NSFileSystemSize
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSNumber
import platform.Foundation.NSProcessInfo
import platform.Foundation.isLowPowerModeEnabled
import platform.UIKit.UIDevice
import com.garfiec.librechat.core.common.network.IosConnectivityObserver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.time.Clock

/**
 * On-demand iOS metrics from public native APIs.
 * No Bluetooth permission, no background service, no file mutation or transmission.
 * Network state reuses Switchboard's existing iOS connectivity observer.
 */
@OptIn(ExperimentalForeignApi::class)
internal actual suspend fun readDeviceTelemetry(): DeviceTelemetry {
    val device = UIDevice.currentDevice
    val previousMonitoring = device.batteryMonitoringEnabled
    device.batteryMonitoringEnabled = true
    val level = device.batteryLevel
    val battery = if (level >= 0f && level <= 1f) (level * 100).toInt() else null
    device.batteryMonitoringEnabled = previousMonitoring

    val process = NSProcessInfo.processInfo
    val capacity = NSFileManager.defaultManager
        .attributesOfFileSystemForPath(NSHomeDirectory(), error = null)
    val free = (capacity?.get(NSFileSystemFreeSize) as? NSNumber)?.longLongValue
    val total = (capacity?.get(NSFileSystemSize) as? NSNumber)?.longLongValue
    val networkConnected = withTimeoutOrNull(2_000L) {
        IosConnectivityObserver().isConnected.first()
    }

    return DeviceTelemetry(
        observedAtEpochMillis = Clock.System.now().toEpochMilliseconds(),
        batteryPercent = battery,
        chargingState = "UNKNOWN",
        lowPowerMode = process.isLowPowerModeEnabled(),
        thermalPressure = "UNKNOWN",
        availableCapacityBytes = free,
        totalCapacityBytes = total,
        networkPath = when (networkConnected) {
            true -> "CONNECTED"
            false -> "DISCONNECTED"
            null -> "UNKNOWN_TIMEOUT"
        },
        isActualDeviceReadback = true,
    )
}
