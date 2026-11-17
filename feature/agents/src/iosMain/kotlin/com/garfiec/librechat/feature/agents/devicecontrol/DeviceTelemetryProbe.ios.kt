package com.garfiec.librechat.feature.agents.devicecontrol

import com.garfiec.librechat.core.common.network.IosConnectivityObserver
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSystemFreeSize
import platform.Foundation.NSFileSystemSize
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSNumber
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSProcessInfoThermalStateCritical
import platform.Foundation.NSProcessInfoThermalStateFair
import platform.Foundation.NSProcessInfoThermalStateNominal
import platform.Foundation.NSProcessInfoThermalStateSerious
import platform.Foundation.isLowPowerModeEnabled
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceBatteryStateCharging
import platform.UIKit.UIDeviceBatteryStateFull
import platform.UIKit.UIDeviceBatteryStateUnplugged
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
    val chargingState = when (device.batteryState) {
        UIDeviceBatteryStateCharging -> "CHARGING"
        UIDeviceBatteryStateFull -> "FULL"
        UIDeviceBatteryStateUnplugged -> "UNPLUGGED"
        else -> "UNKNOWN"
    }
    device.batteryMonitoringEnabled = previousMonitoring

    val process = NSProcessInfo.processInfo
    val thermalPressure = when (process.thermalState) {
        NSProcessInfoThermalStateNominal -> "NOMINAL"
        NSProcessInfoThermalStateFair -> "FAIR"
        NSProcessInfoThermalStateSerious -> "SERIOUS"
        NSProcessInfoThermalStateCritical -> "CRITICAL"
        else -> "UNKNOWN"
    }
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
        chargingState = chargingState,
        lowPowerMode = process.isLowPowerModeEnabled(),
        thermalPressure = thermalPressure,
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
