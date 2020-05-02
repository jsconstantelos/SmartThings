/**
 *  My Xiaomi Mijia Smart Light Sensor
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */

import physicalgraph.zigbee.zcl.DataType

metadata {
    definition (name: "My Xiaomi Mijia Smart Light Sensor", namespace: "jsconstantelos", author: "jsconstantelos", ocfDeviceType: "oic.r.sensor.illuminance") {
        capability "Illuminance Measurement"
        capability "Configuration"
        capability "Refresh"
        capability "Battery"
        capability "Sensor"
        capability "Health Check"
    }

	fingerprint profileId: "0104", inClusters: "0000,0400,0003,0001", outClusters: "0003", manufacturer: "LUMI", model: "lumi.sen_ill.mgl01", deviceJoinName: "Xiaomi Mijia Smart Home Light Sensor", ocfDeviceType: "oic.r.sensor.illuminance"    
    
    tiles(scale: 2) {
		multiAttributeTile(name:"illuminance", type: "generic", width: 6, height: 4){
			tileAttribute("device.illuminance", key: "PRIMARY_CONTROL") {
				attributeState("illuminance", label:'${currentValue} LUX', icon:"st.illuminance.illuminance.bright", backgroundColor:"#999999")
			}
		}
		standardTile("refresh", "device.illuminance", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label: 'Refresh', action:"refresh.refresh", icon:"st.secondary.refresh-icon"
		}
		standardTile("configure", "device.illuminance", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", action:"configuration.configure", icon:"st.secondary.configure"
		}
		standardTile("battery", "device.battery", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'${currentValue}% battery', unit:"%"
		}
		main(["illuminance"])
		details(["illuminance", "battery", "refresh", "configure"])
	}
}

def parse(String description) {
    if (description?.startsWith("catchall:")) {
		def descMap = zigbee.parseDescriptionAsMap(description)
		log.debug "Desc Map : $descMap"
	}
    if (description?.startsWith("read attr -")) {
		def descMap = zigbee.parseDescriptionAsMap(description)
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
    if (description?.startsWith("illuminance:")) {
        def lux = ((description - "illuminance: ").trim()) as int
        sendEvent("name": "illuminance", "value": lux, "unit": "lux", "displayed": true, isStateChange: true)
	}
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
        zigbee.configureReporting(0x0001, 0x0020, 0x20, 60, 3600, 0x01), "delay 1000",	// power cluster (get battery voltage every hour, or if it changes)
        zigbee.configureReporting(0x0003, 0x0000, 0xff, 0, 0, null), "delay 1000",		// identify cluster
        zigbee.configureReporting(0x0400, 0x0000, 0x01, 0, 0, null)						// illuminance cluster (get lux value as soon as it changes)
	]
}