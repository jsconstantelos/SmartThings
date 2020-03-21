/**
 *  My Xiaomi Mijia Smart Light Sensor
 *  Version 1.0
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
 */

import groovy.json.JsonOutput
import physicalgraph.zigbee.zcl.DataType

metadata {
    definition (name: "My Xiaomi Mijia Smart Light Sensor", namespace: "jsconstantelos", author: "jsconstantelos") {
        capability "Illuminance Measurement"
        capability "Configuration"
        capability "Refresh"
        capability "Battery"
        capability "Sensor"
        capability "Health Check"
    }
    tiles(scale: 2) {
		multiAttributeTile(name:"illuminance", type: "generic", width: 6, height: 4){
			tileAttribute("device.illuminance", key: "PRIMARY_CONTROL") {
				attributeState("illuminance", label:'${currentValue} LUX')
			}
		}
		valueTile("battery", "device.battery", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "battery", label:'${currentValue}% battery', unit:"%"
		}
		standardTile("refresh", "device.illuminance", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label: 'Refresh', action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		standardTile("configure", "device.illuminance", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label: 'Configure', action:"configuration.configure", icon:"st.secondary.refresh"
		}
		main(["illuminance"])
		details(["illuminance", "battery", "refresh", "configure"])
	}
}

// Parse incoming device messages to generate events
def parse(String description) {
//	log.debug "DESCRIPTION: $description"
    if (description?.startsWith("catchall:")) {
		def descMap = zigbee.parseDescriptionAsMap(description)
		log.debug "Desc Map : $descMap"
		log.debug "cluster: $descMap.cluster"
		log.debug "clusterId: $descMap.clusterId"
        log.debug "attribute: $descMap.attrId"
		log.debug "value: $descMap.value"
        log.debug "data: $descMap.data"
	}
    if (description?.startsWith("read attr -")) {
		def descMap = zigbee.parseDescriptionAsMap(description)
		if (descMap.cluster == "0001" && descMap.attrId == "0020") {
            def vBatt = Integer.parseInt(descMap.value,16) / 10
            def batteryValue =  ((vBatt - 2.0) / (3.0 - 2.0) * 100) as int
            sendEvent("name": "battery", "value": batteryValue, "displayed": true, isStateChange: true)
		} else {
        	log.debug "UNKNOWN Cluster and Attribute : $descMap"
        }
	}
    if (description?.startsWith("illuminance:")) {
		parseIlluminance(description)
	}
}

private parseIlluminance(String description) {
    def lux = ((description - "illuminance: ").trim()) as int
    sendEvent("name": "illuminance", "value": lux, "unit": "lux", "displayed": true, isStateChange: true)
}

def installed() {
	configure()
}

def updated() {
	configure()
}

def refresh() {
	log.debug "Refreshing values..."
	[
		"st rattr 0x${device.deviceNetworkId} 1 0x000 0", "delay 200",
        "st rattr 0x${device.deviceNetworkId} 1 0x001 0", "delay 200",
        "st rattr 0x${device.deviceNetworkId} 1 0x003 0", "delay 200",
        "st rattr 0x${device.deviceNetworkId} 1 0x400 0", "delay 200"
	]
}

def configure() {
	log.debug "Configuration starting..."
	sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
    log.debug "...bindings..."
	[
		"zdo bind 0x${device.deviceNetworkId} 1 1 0x000 {${device.zigbeeId}} {}", "delay 1000",	// basic cluster
        "zdo bind 0x${device.deviceNetworkId} 1 1 0x001 {${device.zigbeeId}} {}", "delay 1000",	// power cluster
		"zdo bind 0x${device.deviceNetworkId} 1 1 0x003 {${device.zigbeeId}} {}", "delay 1000",	// identify cluster
		"zdo bind 0x${device.deviceNetworkId} 1 1 0x400 {${device.zigbeeId}} {}", "delay 1000",	// illuminance cluster
		"send 0x${device.deviceNetworkId} 1 1"
	]
    log.debug "...reporting intervals..."
    [
    	zigbee.configureReporting(0x0000, 0x0005, 0xff, 5, 300, null), "delay 1000",	// basic cluster
        zigbee.configureReporting(0x0001, 0x0020, 0x20, 30, 3600, 0x01), "delay 1000",	// power cluster (battery reporting)
        zigbee.configureReporting(0x0003, 0x0000, 0xff, 0, 0, null), "delay 1000",		// identify cluster
        zigbee.configureReporting(0x0400, 0x0000, 0x01, 0, 0, null)						// illuminance cluster
	]
}