package com.garfiec.librechat.feature.agents.devicecontrol

import kotlin.test.Test
import kotlin.test.assertEquals

class DeviceControlPolicyTest {
    @Test fun uniqueContractsForEveryCapability() {
        assertEquals(DeviceCapability.entries.size, IOSDeviceCatalog.entries.size)
        assertEquals(DeviceCapability.entries.size, IOSDeviceCatalog.entries.map { it.capability }.toSet().size)
    }

    @Test fun onlyLocallyImplementedTelemetryIsMarkedWired() {
        assertEquals(setOf(
            DeviceCapability.BATTERY_PERCENT,
            DeviceCapability.LOW_POWER_MODE,
            DeviceCapability.VOLUME_CAPACITY,
            DeviceCapability.NETWORK_PATH,
        ), IOSDeviceCatalog.entries.filter { it.wiredInCurrentBuild }.map { it.capability }.toSet())
    }

    @Test fun batteryIsLocalReadOnly() {
        assertEquals(ControlDecision.ALLOW_R0_LOCAL,
            IOSDevicePolicy.decide(DeviceActionRequest(DeviceCapability.BATTERY_PERCENT)))
    }

    @Test fun systemRestrictedActionsAlwaysDenied() {
        for (capability in listOf(DeviceCapability.BATTERY_HEALTH, DeviceCapability.WIFI_ALL_NETWORKS,
            DeviceCapability.OTHER_APP_CACHE_DELETE, DeviceCapability.INSTALL_OTHER_APPS,
            DeviceCapability.REPAIR_IOS, DeviceCapability.SYSTEM_BACKUP)) {
            assertEquals(ControlDecision.DENY, IOSDevicePolicy.decide(DeviceActionRequest(
                capability, userInitiated = true, nativePermissionGranted = true,
                scopedOneTimeApproval = true,
            )))
        }
    }

    @Test fun unconnectedCapabilitiesStayDeniedEvenWhenPlatformPermissionExists() {
        for (capability in listOf(DeviceCapability.BLE_DISCOVERY, DeviceCapability.LOCATION_SAMPLE,
            DeviceCapability.PHOTO_INVENTORY, DeviceCapability.APP_DOCUMENT_EXPORT,
            DeviceCapability.APP_CACHE_USAGE, DeviceCapability.SERVER_REACHABILITY)) {
            assertEquals(ControlDecision.DENY, IOSDevicePolicy.decide(DeviceActionRequest(
                capability, userInitiated = true, nativePermissionGranted = true,
                deviceOnline = true, destinationIsAuthenticatedNostromoBackend = true,
                outboundTelemetryConsent = true,
            )))
        }
    }

    @Test fun materialActionsNeverReceiveGrantFromThisPolicy() {
        for (capability in listOf(DeviceCapability.APP_CACHE_CLEAN, DeviceCapability.APP_BACKUP, DeviceCapability.PHOTO_DELETE)) {
            assertEquals(ControlDecision.DENY, IOSDevicePolicy.decide(DeviceActionRequest(capability)))
            assertEquals(ControlDecision.DENY,
                IOSDevicePolicy.decide(DeviceActionRequest(capability, userInitiated = true,
                    nativePermissionGranted = true, scopedOneTimeApproval = true)))
        }
    }

    @Test fun remoteTargetsCannotDirectlyControlIPhoneOrBypassNostromo() {
        for (target in listOf(TargetSystem.MANOJOS3, TargetSystem.ORACULO, TargetSystem.VX13)) {
            assertEquals(ControlDecision.DENY, IOSDevicePolicy.decide(DeviceActionRequest(
                DeviceCapability.BATTERY_PERCENT, target = target, deviceOnline = true
            )))
            assertEquals(ControlDecision.ALLOW_R0_LOCAL, IOSDevicePolicy.decide(DeviceActionRequest(
                DeviceCapability.BATTERY_PERCENT, target = target, deviceOnline = true,
                destinationIsAuthenticatedNostromoBackend = true,
                outboundTelemetryConsent = true
            )))
            assertEquals(ControlDecision.DENY, IOSDevicePolicy.decide(DeviceActionRequest(
                DeviceCapability.BATTERY_PERCENT, target = target, deviceOnline = true,
                destinationIsAuthenticatedNostromoBackend = true,
                attemptsMaterialEffectOnRemoteSystem = true
            )))
        }
    }

    @Test fun backgroundBluetoothIsBlocked() {
        assertEquals(ControlDecision.DENY, IOSDevicePolicy.decide(DeviceActionRequest(
            DeviceCapability.BLE_DISCOVERY, userInitiated = true,
            nativePermissionGranted = true, appForeground = false,
        )))
    }

    @Test fun remoteReadRequiresExplicitTelemetryConsent() {
        assertEquals(ControlDecision.DENY, IOSDevicePolicy.decide(DeviceActionRequest(
            DeviceCapability.BATTERY_PERCENT, target = TargetSystem.ORACULO,
            deviceOnline = true, destinationIsAuthenticatedNostromoBackend = true,
        )))
        assertEquals(ControlDecision.ALLOW_R0_LOCAL, IOSDevicePolicy.decide(DeviceActionRequest(
            DeviceCapability.BATTERY_PERCENT, target = TargetSystem.ORACULO,
            deviceOnline = true, destinationIsAuthenticatedNostromoBackend = true,
            outboundTelemetryConsent = true,
        )))
    }

    @Test fun diskSpaceNeverLeavesTheIphoneByRemoteControl() {
        assertEquals(ControlDecision.DENY, IOSDevicePolicy.decide(DeviceActionRequest(
            DeviceCapability.VOLUME_CAPACITY, deviceOnline = true,
            destinationIsAuthenticatedNostromoBackend = true,
            outboundTelemetryConsent = true, transmitsTelemetryOffDevice = true
        )))
        assertEquals(ControlDecision.ALLOW_R0_LOCAL, IOSDevicePolicy.decide(
            DeviceActionRequest(DeviceCapability.VOLUME_CAPACITY)
        ))
    }

    @Test fun evenNostromoBackendNeedsExplicitConsentForOutboundTelemetry() {
        assertEquals(ControlDecision.DENY, IOSDevicePolicy.decide(DeviceActionRequest(
            DeviceCapability.BATTERY_PERCENT, transmitsTelemetryOffDevice = true,
            deviceOnline = true, destinationIsAuthenticatedNostromoBackend = true,
        )))
        assertEquals(ControlDecision.ALLOW_R0_LOCAL, IOSDevicePolicy.decide(DeviceActionRequest(
            DeviceCapability.BATTERY_PERCENT, transmitsTelemetryOffDevice = true,
            deviceOnline = true, destinationIsAuthenticatedNostromoBackend = true,
            outboundTelemetryConsent = true
        )))
    }
}
