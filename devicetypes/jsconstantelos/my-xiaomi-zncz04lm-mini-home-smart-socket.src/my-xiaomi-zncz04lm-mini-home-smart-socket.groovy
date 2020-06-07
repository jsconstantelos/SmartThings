/*
 *  Copyright 2020 SmartThings
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
 *  Updates:
 *  -------
 *  06-07-2020 : Initial commit.
 */
import physicalgraph.zigbee.zcl.DataType

metadata {
	definition(name: "My Xiaomi ZNCZ04LM Mini Home Smart Socket", namespace: "jsconstantelos", author: "SmartThings", ocfDeviceType: "oic.d.smartplug", vid: "generic-switch-power-energy") {
        capability "Energy Meter"
        capability "Power Meter"
        capability "Actuator"
        capability "Switch"
        capability "Refresh"
        capability "Health Check"
        capability "Sensor"
        capability "Configuration"

        attribute "kwhTotal", "number"		// this is value reported by the switch since joining the hub.
        attribute "resetTotal", "number"	// used to calculate accumulated kWh after a reset by the user.

        command "reset"

        // Raw Description : 01 0104 0051 01 09 0000 0002 0003 0004 0005 0006 0009 0702 0B04 02 000A 0019
		fingerprint profileId: "0104", inClusters: "0000,0002,0003,0004,0005,0006,0009,0702,0B04", outClusters: "000A,0019", manufacturer: "LUMI", model: "lumi.plug.mmeu01", deviceJoinName: "Xiaomi ZNCZ04LM Mini Home Smart Socket"
	}

	preferences {
		input "forceConfig", "boolean", title: "Toggle ONCE to force device configuration (any position will force a config)"
	}

	tiles(scale: 2) {
		multiAttributeTile(name: "switch", width: 6, height: 4, canChangeIcon: false) {
			tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label: '${name}', action: "switch.off", icon: "st.switches.light.on", backgroundColor: "#00A0DC", nextState: "turningOff"
				attributeState "off", label: '${name}', action: "switch.on", icon: "st.switches.light.off", backgroundColor: "#ffffff", nextState: "turningOn"
				attributeState "turningOn", label: '${name}', action: "switch.off", icon: "st.switches.light.on", backgroundColor: "#00A0DC", nextState: "turningOff"
				attributeState "turningOff", label: '${name}', action: "switch.on", icon: "st.switches.light.off", backgroundColor: "#ffffff", nextState: "turningOn"
			}
			tileAttribute("power", key: "SECONDARY_CONTROL") {
				attributeState "power", label: '${currentValue} W'
			}
		}
        standardTile("energy", "device.energy", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:'${currentValue} kWh'
        }
        standardTile("reset", "device.energy", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:'Reset kWh', action:"reset", icon: "st.secondary.refresh-icon"
        }
		standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label: "Refresh", action: "refresh.refresh", icon: "st.secondary.refresh-icon"
		}
		standardTile("configure", "device.configure", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", action: "configuration.configure", icon: "st.secondary.configure"
		}
		main "switch"
	}
}

def installed() {
	log.debug "Installed"
    sendEvent(name: "resetTotal", value: 0, unit: "kWh")
	updateDataValue("onOff", "catchall")
}

def updated() {
	log.debug "Updated"
	updateDataValue("onOff", "catchall")
	refresh()
}

def parse(String description) {
	Map eventMap = zigbee.getEvent(description)
    log.debug "eventMap is : $eventDescMap"
	Map eventDescMap = zigbee.parseDescriptionAsMap(description)
    log.debug "eventDescMap is : $eventDescMap"
    if (device.currentState('resetTotal')?.doubleValue == null) {
    	sendEvent(name: "resetTotal", value: 0, unit: "kWh")
    }
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
            }
            else {
                sendEvent(eventMap)
            }
		}
	}
}

def on() {
	zigbee.on()
}

def off() {
	zigbee.off()
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
		"zdo bind 0x${device.deviceNetworkId} 1 1 0xb04 {${device.zigbeeId}} {}", "delay 1000",
        "zdo bind 0x${device.deviceNetworkId} 1 1 0x702 {${device.zigbeeId}} {}", "delay 1000",
		"send 0x${device.deviceNetworkId} 1 1"
	]
    log.debug "...reporting intervals..."
    [
    	zigbee.configureReporting(0x0006, 0x0000, 0x10, 0, 600, null), "delay 1000",	// configure reporting of on/off
        zigbee.configureReporting(0x0B04, 0x050B, 0x29, 0, 600, 0x01), "delay 1000",	// configure power reporting
        zigbee.configureReporting(0x0702, 0x0000, 0x25, 0, 1800, 0x01)					// configure energy reporting
	]
	log.debug "...refreshing values..."
    [
    	zigbee.readAttribute(0x0006, 0x0000), "delay 1000",
    	zigbee.readAttribute(0x0B04, 0x050B)
	]
}

def configureHealthCheck() {
	log.debug "configureHealthCheck"
	Integer hcIntervalMinutes = 12
	def healthEvent = [name: "checkInterval", value: hcIntervalMinutes * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID]]
	sendEvent(healthEvent)
}