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
 *  Community discussion : https://community.smartthings.com/t/aqara-double-rocker-wall-switch-discussion/192205
 *
 *  Updates:
 *  -------
 *  09-02-2020 : Initial commit.
 */

metadata {
	definition(name: "My Aqara Double Rocker Switch No Neutral", namespace: "jsconstantelos", author: "jsconstantelos", ocfDeviceType: "oic.d.switch") {
		capability "Actuator"
		capability "Configuration"
		capability "Refresh"
		capability "Health Check"
		capability "Switch"
        
		command "childOn", ["string"]
		command "childOff", ["string"]

        fingerprint profileId: "0104", inClusters: "0000,0002,0003,0004,0005,0006,0009", outClusters: "000A,0019", manufacturer: "LUMI", model: "lumi.switch.b2laus01", deviceJoinName: "Aqara Double Rocker Switch No Neutral"
	}

	tiles(scale: 2) {
		multiAttributeTile(name: "switch", width: 6, height: 4, canChangeIcon: false) {
			tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label: '${name}', action: "switch.off", icon: "st.switches.light.on", backgroundColor: "#00A0DC", nextState: "turningOff"
				attributeState "off", label: '${name}', action: "switch.on", icon: "st.switches.light.off", backgroundColor: "#ffffff", nextState: "turningOn"
				attributeState "turningOn", label: '${name}', action: "switch.off", icon: "st.switches.light.on", backgroundColor: "#00A0DC", nextState: "turningOff"
				attributeState "turningOff", label: '${name}', action: "switch.on", icon: "st.switches.light.off", backgroundColor: "#ffffff", nextState: "turningOn"
			}
		}
        childDeviceTiles("all")
		standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label: "Refresh", action: "refresh.refresh", icon: "st.secondary.refresh-icon"
		}
		standardTile("configure", "device.configure", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", action: "configuration.configure", icon: "st.secondary.configure"
		}
	}
}

def installed() {
	log.debug "Installed"
	updateDataValue("onOff", "catchall")
	createChildDevices()
}

def updated() {
	log.debug "Updated"
	updateDataValue("onOff", "catchall")
	refresh()
}

def parse(String description) {
	Map eventMap = zigbee.getEvent(description)
	Map eventDescMap = zigbee.parseDescriptionAsMap(description)
//    log.debug "eventDescMap : $eventDescMap"
    if (eventDescMap) {
//    	log.debug "Switch sent something we weren't expecting: $eventDescMap"
    }
	if (eventMap) {
		if (eventDescMap?.sourceEndpoint == "01" || eventDescMap?.endpoint == "01") {
			sendEvent(eventMap)
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
			addChildDevice("smartthings","Child Switch Health", "${device.deviceNetworkId}:0${endpoint}", device.hubId,
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
	def refreshCommands = zigbee.onOffRefresh()
	def numberOfChildDevices = modelNumberOfChildDevices[device.getDataValue("model")]
	for(def endpoint : 2..numberOfChildDevices) {
		refreshCommands += zigbee.readAttribute(zigbee.ONOFF_CLUSTER, 0x0000, [destEndpoint: endpoint])
	}
	log.debug "refreshCommands: $refreshCommands"
	return refreshCommands
}

def configure() {
	log.debug "Configure..."
	configureHealthCheck()
    log.debug "...bindings..."
	[
		"zdo bind 0x${device.deviceNetworkId} 1 1 0x000 {${device.zigbeeId}} {}", "delay 1000",
        "zdo bind 0x${device.deviceNetworkId} 1 1 0x006 {${device.zigbeeId}} {}", "delay 1000",
        "zdo bind 0x${device.deviceNetworkId} 2 1 0x006 {${device.zigbeeId}} {}", "delay 1000",
		"send 0x${device.deviceNetworkId} 1 1"
	]
	def numberOfChildDevices = modelNumberOfChildDevices[device.getDataValue("model")]
	def configurationCommands = zigbee.onOffConfig(0, 120)
	for(def endpoint : 2..numberOfChildDevices) {
		configurationCommands += zigbee.configureReporting(zigbee.ONOFF_CLUSTER, 0x0000, 0x10, 0, 120, null, [destEndpoint: endpoint])
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
        "lumi.switch.b2laus01" : 2
	]
}