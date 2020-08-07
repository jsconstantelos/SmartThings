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
 *
 *	Contributors : Graham Johnson (aka @orangebucket in the ST Community) for the VID for the new ST mobile app.
 *
 *  Updates:
 *  -------
 *  03-20-2020 : Initial commit.
 *  05-06-2020 : Cleaned up code and resolved large lux values by using zigbee.lux() to convert raw value to lux.
 *  05-06-2020 : Added a preference to adjust minimum illuminance reporting time, and a preference for the amount of change in raw lux data.
 *  05-22-2020 : Fixed issues with preference values.
 *  05-23-2020 : Removed preferences because the sensor doesn't seem to honor them.
 *  05-30-2020 : Added a preference/setting for the new app that will force device configuration.  This is a workaround for not having a Configure tile in the new app.
 *  08-07-2020 : Added/Updated mnmn and VID values to have LUX show up properly in the new app vs. battery level or "checking status"
 */

import physicalgraph.zigbee.zcl.DataType

metadata {
    definition (name: "My Xiaomi Mijia Smart Light Sensor", namespace: "jsconstantelos", author: "jsconstantelos", mnmn: "SmartThingsCommunity", vid: "a3fe3c0d-1f51-3d51-9309-566ba1219b4f") {
        capability "Illuminance Measurement"
        capability "Configuration"
        capability "Refresh"
        capability "Battery"
        capability "Sensor"
        capability "Health Check"
    }

	preferences {
		input "forceConfig", "boolean", title: "Toggle ONCE to force device configuration (any position will force a config)"
	}

	fingerprint profileId: "0104", inClusters: "0000,0400,0003,0001", outClusters: "0003", manufacturer: "LUMI", model: "lumi.sen_ill.mgl01", deviceJoinName: "Xiaomi Mijia Smart Home Light Sensor"
    
    tiles(scale: 2) {
		multiAttributeTile(name:"illuminance", type: "generic", width: 6, height: 4){
			tileAttribute("device.illuminance", key: "PRIMARY_CONTROL") {
				attributeState("illuminance", label:'${currentValue} LUX', icon:"st.illuminance.illuminance.bright", backgroundColor:"#999999")
			}
		}
		standardTile("battery", "device.battery", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'${currentValue}% battery', unit:"%"
		}
		standardTile("refresh", "device.illuminance", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label: 'Refresh', action:"refresh.refresh", icon:"st.secondary.refresh-icon"
		}
		standardTile("configure", "device.illuminance", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", action:"configuration.configure", icon:"st.secondary.configure"
		}
		main(["illuminance"])
		details(["illuminance", "battery", "refresh", "configure"])
	}
}

def parse(String description) {
//	log.debug "Incoming data from device : $description"
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
        def raw = ((description - "illuminance: ").trim()) as int
        def lux = Math.round(zigbee.lux(raw as Integer)).toString()
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
        "st rattr 0x${device.deviceNetworkId} 1 0x001 0", "delay 200",
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
        zigbee.configureReporting(0x0001, 0x0020, 0x20, 60, 3600, 0x01), "delay 1000",	// power cluster (get battery voltage every hour, or if it changes)
        zigbee.configureReporting(0x0400, 0x0000, 0x21, 10, 3600, 0x15)					// illuminance cluster (min report time 10 seconds, max 3600 seconds, raw amount of change 21 (0x15))
	]
}