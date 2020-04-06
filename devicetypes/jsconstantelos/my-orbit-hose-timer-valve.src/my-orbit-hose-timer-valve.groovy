/**
 *  Orbit Hose Timer Valve
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Zigbee Clusters
 *  0x0000 - Basic
 *  0x0001 - Power Configuration
 *  0x0003 - Identify
 *  0x0006 - On/Off
 *  0x0020 - Poll Control
 *  0x0201 - Thermostat
 */

import physicalgraph.zigbee.zcl.DataType

metadata {
	definition (name: "My Orbit Hose Timer Valve", namespace: "jsconstantelos", author: "John Constantelos", vid: "generic-valve") {
		capability "Actuator"
		capability "Battery"
		capability "Configuration"
		capability "Refresh"
		capability "Switch"
		capability "Valve"
        capability "Health Check"

        fingerprint profileId: "0104", inClusters: "0000,0001,0003,0020,0006,0201", outClusters: "000A,0019", manufacturer: "Orbit", model: "WT15ZB-12", deviceJoinName: "Orbit Hose Timer Valve", ocfDeviceType: "x.com.st.valve"
	}

	tiles(scale: 2) {
		multiAttributeTile(name:"switch", type: "generic", width: 6, height: 4){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'Open', action:"switch.off", icon:"st.Outdoor.outdoor16", backgroundColor:"#00A0DC"
				attributeState "off", label:'Closed', action:"switch.on", icon:"st.Outdoor.outdoor16", backgroundColor:"#ffffff"
			}
            tileAttribute ("device.battery", key: "SECONDARY_CONTROL") {
                attributeState("default", label:'${currentValue}% battery', unit:"%")
            }
		}
		valueTile("temperature", "device.temperature", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state("temperature", label:'${currentValue}°')
        }
		standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		standardTile("configure", "device.configuration", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", action:"configuration.configure", icon:"st.secondary.configure"
		}
		main "switch"
		details(["switch", "temperature", "refresh", "configure"])
	}
}

def installed() {
	configure()
}

def updated() {
	configure()
}

def parse(String description) {
	def result = zigbee.getEvent(description)
    log.debug "Result Map : $result"
    if(result?.name == "switch") {
        if(result?.value == "off") {
        	off()
        } else {
        	on()
        }
    }
    if (description?.startsWith("catchall:")) {
		def descMap = zigbee.parseDescriptionAsMap(description)
		log.debug "Desc Map : $descMap"
	}
    if (description?.startsWith("read attr -")) {
		def descMap = zigbee.parseDescriptionAsMap(description)
		def tempScale = location.temperatureScale
        // TEMPERATURE
		if (descMap.cluster == "0201" && descMap.attrId == "0000") {
        	if (descMap.value != null) {
            	def trimvalue = descMap.value[-4..-1]
                def celsius = Integer.parseInt(trimvalue, 16) / 100
                def fahrenheit = String.format("%3.1f",celsiusToFahrenheit(celsius))
                if (tempScale == "F") {
                    sendEvent("name": "temperature", "value": fahrenheit, "unit": temperatureScale, "displayed": true)
                    log.debug "TEMPERATURE is : ${fahrenheit}${temperatureScale}"
                } else {
                    sendEvent("name": "temperature", "value": celsius, "unit": temperatureScale, "displayed": true)
                    log.debug "TEMPERATURE is : ${celsius}${temperatureScale}"
                }
            }
        }
        // BATTERY LEVEL
		if (descMap.cluster == "0001" && descMap.attrId == "0020") {
            def vBatt = Integer.parseInt(descMap.value,16) / 10
            def pct = (vBatt - 2.1) / (3 - 2.1)
            def roundedPct = Math.round(pct * 100)
            if (roundedPct <= 0) roundedPct = 1
            def batteryValue = Math.min(100, roundedPct)
            sendEvent("name": "battery", "value": batteryValue, "displayed": true, isStateChange: true)
		} else {
        	log.debug "UNKNOWN Cluster and Attribute : $descMap"
        }
	}
}

def on() {
    sendEvent(name: "valve", value:"open")
    zigbee.on()
}

def off() {
    sendEvent(name: "valve", value:"closed")
    zigbee.off()
}

def open() {
    on()
}

def close() {
    off()
}

def refresh() {
	log.debug "Refreshing values..."
    [
    	zigbee.readAttribute(0x0006, 0x0000), "delay 1000",
        zigbee.readAttribute(0x0001, 0x0020), "delay 1000",
        zigbee.readAttribute(0x0201, 0x0000)
        // "st rattr 0x${device.deviceNetworkId} 1 0x201 0"
    ]
}

def configure() {
	log.debug "Configuration starting..."
	sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
    log.debug "...bindings..."
	[
		"zdo bind 0x${device.deviceNetworkId} 1 1 0x000 {${device.zigbeeId}} {}", "delay 1000",	// basic
        "zdo bind 0x${device.deviceNetworkId} 1 1 0x001 {${device.zigbeeId}} {}", "delay 1000",	// power
		"zdo bind 0x${device.deviceNetworkId} 1 1 0x003 {${device.zigbeeId}} {}", "delay 1000",	// identify
        "zdo bind 0x${device.deviceNetworkId} 1 1 0x006 {${device.zigbeeId}} {}", "delay 1000",	// on/off
		"zdo bind 0x${device.deviceNetworkId} 1 1 0x201 {${device.zigbeeId}} {}", "delay 1000",	// thermostat
		"send 0x${device.deviceNetworkId} 1 1"
	]
    log.debug "...reporting intervals..."
    [
    	zigbee.configureReporting(0x0000, 0x0005, 0xff, 5, 300, null), "delay 1000",	// basic cluster
        zigbee.configureReporting(0x0001, 0x0020, 0x20, 60, 3600, 0x01), "delay 1000",	// power cluster (get battery voltage every hour, or if it changes)
        zigbee.configureReporting(0x0003, 0x0000, 0xff, 0, 0, null), "delay 1000",		// identify cluster
        zigbee.configureReporting(0x0006, 0x0000, 0x10, 0, 600, null), "delay 1000",	// on/off
        zigbee.configureReporting(0x0201, 0x0000, 0x01, 0, 0, null)						// thermostat (get temp value as soon as it changes)
	]
    log.debug "...refreshing..."
    [
    	zigbee.readAttribute(0x0006, 0x0000), "delay 1000",
        zigbee.readAttribute(0x0001, 0x0020), "delay 1000",
        zigbee.readAttribute(0x0201, 0x0000)
        // "st rattr 0x${device.deviceNetworkId} 1 0x201 0"
    ]
}