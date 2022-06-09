/*
 *  Copyright 2019 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy
 *  of the License at:
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations
 *  under the License.
 *
 *  Original code : ZigBee Multi Switch Power, modified for the Aqare double rocker switch by JSConstantelos
 *  Community discussion : https://community.smartthings.com/t/aqara-double-rocker-wall-switch-discussion/192205
 *
 *  Updates:
 *  -------
 *  04-30-2020 : Initial commit.
 *  05-02-2020 : Added child switch tile so that both switches are controllable in this DTH.  This only works for the Classic app, but not in the new app yet.  The child switch still shows up as a separete device in both apps.
 *  05-26-2020 : Added accumulated energy (kWh) since the device was joined to the hub.  There is no device reset, so used state variables to reflect a reset.  IDE shows total and total since last reset (which is what the mobile app shows).
 */

metadata {
	definition(name: "My Aqara Double Rocker Switch", namespace: "jsconstantelos", author: "jsconstantelos", vid: "generic-switch-power") {
		capability "Actuator"
		capability "Configuration"
		capability "Refresh"
		capability "Health Check"
		capability "Switch"
		capability "Power Meter"
        capability "Energy Meter"
        
        attribute "kwhTotal", "number"		// this is value reported by the switch since joining the hub.  See change log above for details.
        attribute "resetTotal", "number"	// used to calculate accumulated kWh after a reset by the user.  See change log above for details.

		command "childOn", ["string"]
		command "childOff", ["string"]
        command "reset"
        
//		fingerprint profileId: "0104", inClusters: "0000,0002,0003,0004,0005,0006,0009,0702,0B04", outClusters: "000A,0019", manufacturer: "LUMI", model: "lumi.switch.b2naus01", deviceJoinName: "Aqara Double Rocker Switch"
	}
    preferences {
       input "debugOutput", "boolean", title: "Enable debug logging?", defaultValue: false, displayDuringSetup: false
    }
}

def installed() {
	log.debug "Installed"
    sendEvent(name: "resetTotal", value: 0, unit: "kWh")
	updateDataValue("onOff", "catchall")
	createChildDevices()
    configure()
}

def updated() {
	log.debug "Updated"
	updateDataValue("onOff", "catchall")
	configure()
}

def parse(String description) {
//	log.debug "Raw Data : $description"
	Map eventMap = zigbee.getEvent(description)
	Map eventDescMap = zigbee.parseDescriptionAsMap(description)
    if (device.currentState('resetTotal')?.doubleValue == null) {
    	sendEvent(name: "resetTotal", value: 0, unit: "kWh")
    }
//    log.debug "eventDescMap : $eventDescMap"
    if (eventDescMap) {
        if (eventDescMap.cluster == "0702") {
            def value = (zigbee.convertHexToInt(eventDescMap.value) / 1000) - device.currentState('resetTotal')?.doubleValue
            sendEvent(name: "energy", value: value.round(3), unit: "kWh")
            sendEvent(name: "kwhTotal", value: zigbee.convertHexToInt(eventDescMap.value) / 1000, unit: "kWh", displayed: false)
        }
    }
	if (eventMap) {
		if (eventDescMap?.sourceEndpoint == "01" || eventDescMap?.endpoint == "01") {
            if (eventMap.name == "power") {
                def powerValue
                def div = device.getDataValue("divisor")
                div = div ? (div as int) : 10
                powerValue = (eventMap.value as Integer)/div
                sendEvent(name: "power", value: powerValue, unit: "W")
				def children = getChildDevices()
                children.each {
                    def childDevice = "${it.deviceNetworkId}"
                    it.sendEvent(name: "power", value: powerValue, unit: "W")
                }
            }
            else {
                sendEvent(eventMap)
            }
		} else {
			def childDevice = childDevices.find {
				it.deviceNetworkId == "$device.deviceNetworkId:${eventDescMap.sourceEndpoint}" || it.deviceNetworkId == "$device.deviceNetworkId:${eventDescMap.endpoint}"
			}
			if (childDevice) {
				childDevice.sendEvent(eventMap)
			} else {
				log.debug "Child device: $device.deviceNetworkId:${eventDescMap.sourceEndpoint} was not found"
			}
		}
	}
}

private void createChildDevices() {
	def numberOfChildDevices = modelNumberOfChildDevices[device.getDataValue("model")]
	log.debug("createChildDevices(), numberOfChildDevices: ${numberOfChildDevices}")

	for(def endpoint : 2..numberOfChildDevices) {
		try {
			log.debug "creating endpoint: ${endpoint}"
			addChildDevice("smartthings","Child Switch Health Power", "${device.deviceNetworkId}:0${endpoint}", device.hubId,
				[completedSetup: true,
				 label: "${device.displayName} Child Device${endpoint}",
				 isComponent: false
				])
		} catch(Exception e) {
			log.debug "Exception: ${e}"
		}
	}
}

def on() {
	zigbee.on()
}

def off() {
	zigbee.off()
}

def childOn(String dni) {
	def childEndpoint = getChildEndpoint(dni)
	zigbee.command(zigbee.ONOFF_CLUSTER, 0x01, "", [destEndpoint: childEndpoint])
}

def childOff(String dni) {
	def childEndpoint = getChildEndpoint(dni)
	zigbee.command(zigbee.ONOFF_CLUSTER, 0x00, "", [destEndpoint: childEndpoint])
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
	refresh()
}

def refresh() {
	log.debug "Refreshing values..."
	def refreshCommands = zigbee.onOffRefresh() + zigbee.electricMeasurementPowerRefresh()
	def numberOfChildDevices = modelNumberOfChildDevices[device.getDataValue("model")]
	for(def endpoint : 2..numberOfChildDevices) {
		refreshCommands += zigbee.readAttribute(zigbee.ONOFF_CLUSTER, 0x0000, [destEndpoint: endpoint])
		refreshCommands += zigbee.readAttribute(zigbee.ELECTRICAL_MEASUREMENT_CLUSTER, 0x050B, [destEndpoint: endpoint])
	}
	log.debug "refreshCommands: $refreshCommands"
	return refreshCommands
}

def reset() {
    log.debug "Resetting kWh..."
    sendEvent(name: "resetTotal", value: device.currentState('kwhTotal')?.doubleValue, unit: "kWh")
    sendEvent(name: "energy", value: 0, unit: "kWh")
}

def configure() {
	log.debug "Configure..."
	configureHealthCheck()
    log.debug "...bindings..."
	[
		"zdo bind 0x${device.deviceNetworkId} 1 1 0x000 {${device.zigbeeId}} {}", "delay 1000",
        "zdo bind 0x${device.deviceNetworkId} 1 1 0x006 {${device.zigbeeId}} {}", "delay 1000",
        "zdo bind 0x${device.deviceNetworkId} 2 1 0x006 {${device.zigbeeId}} {}", "delay 1000",
		"zdo bind 0x${device.deviceNetworkId} 1 1 0xb04 {${device.zigbeeId}} {}", "delay 1000",
		"zdo bind 0x${device.deviceNetworkId} 2 1 0xb04 {${device.zigbeeId}} {}", "delay 1000",
        "zdo bind 0x${device.deviceNetworkId} 1 1 0x702 {${device.zigbeeId}} {}", "delay 1000",

		"send 0x${device.deviceNetworkId} 1 1"
	]
	def numberOfChildDevices = modelNumberOfChildDevices[device.getDataValue("model")]
	def configurationCommands = zigbee.onOffConfig(0, 120) + zigbee.electricMeasurementPowerConfig() + zigbee.simpleMeteringPowerConfig()
	for(def endpoint : 2..numberOfChildDevices) {
		configurationCommands += zigbee.configureReporting(zigbee.ONOFF_CLUSTER, 0x0000, 0x10, 0, 120, null, [destEndpoint: endpoint])
		configurationCommands += zigbee.configureReporting(zigbee.ELECTRICAL_MEASUREMENT_CLUSTER, 0x050B, 0x29, 1, 600, 0x0005, [destEndpoint: endpoint])
	}
	configurationCommands << refresh()
	log.debug "configurationCommands: $configurationCommands"
	return configurationCommands
}

def configureHealthCheck() {
	log.debug "configureHealthCheck"
	Integer hcIntervalMinutes = 12
	def healthEvent = [name: "checkInterval", value: hcIntervalMinutes * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID]]
	sendEvent(healthEvent)
	childDevices.each {
		it.sendEvent(healthEvent)
	}
}

private getChildEndpoint(String dni) {
	dni.split(":")[-1] as Integer
}

private getModelNumberOfChildDevices() {
	[
		"lumi.switch.b2naus01" : 2
	]
}