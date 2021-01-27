/**
 *  My Aqara Vibration Sensor
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
 *  Updates:
 *  -------
 *  01-26-2020 : Initial commit.
 */

import physicalgraph.zigbee.zcl.DataType

metadata {
    definition (name: "My Aqara Vibration Sensor", namespace: "jsconstantelos", author: "jsconstantelos") {
    	capability "Acceleration Sensor"
		capability "Battery"
		capability "Configuration"
		capability "Sensor"
		capability "Refresh"
		capability "Health Check"
    }

	preferences {
        input "vibrationreset", "number", title: "", description: "Number of seconds when vibration detection restarts (default = 10 seconds)", range: "1..60"
	}

	fingerprint profileId: "0104", inClusters: "0000,0500,0003,0001", outClusters: "0003,0019", manufacturer: "LUMI", model: "lumi.vibration.agl01", deviceJoinName: "Aqara Vibration Sensor"
    
    tiles(scale: 2) {
		standardTile("acceleration", "device.acceleration", width: 2, height: 2) {
			state("active", label:'${name}', icon:"st.motion.acceleration.active", backgroundColor:"#00a0dc")
			state("inactive", label:'${name}', icon:"st.motion.acceleration.inactive", backgroundColor:"#cccccc")
		}
		valueTile("battery", "device.battery", decoration: "flat", inactiveLabel: false, width: 2, height: 2) {
			state "battery", label: '${currentValue}% battery', unit: ""
		}
		standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", action: "refresh.refresh", icon: "st.secondary.refresh"
		}
		main(["acceleration"])
		details(["acceleration","battery","refresh"])
	}
}

def parse(String description) {
	log.debug "Incoming data from device : $description"
    if (description?.startsWith("read attr -")) {
		def descMap = zigbee.parseDescriptionAsMap(description)
//        log.debug "Map Data : $descMap"
		if (descMap.cluster == "0001" && descMap.attrId == "0020") {
            def vBatt = Integer.parseInt(descMap.value,16) / 10
            def pct = (vBatt - 2.1) / (3 - 2.1)
            def roundedPct = Math.round(pct * 100)
            if (roundedPct <= 0) roundedPct = 1
            def batteryValue = Math.min(100, roundedPct)
            sendEvent(name: "battery", value: batteryValue, displayed: true, isStateChange: true)
		} else if (descMap.cluster == "0500" && descMap.attrId == "002d") {
//        	log.debug "Vibration detected!"
            sendEvent(name: "acceleration", value: "active", displayed: true, isStateChange: true)
            def resetseconds = vibrationreset ? vibrationreset : 10
//            log.debug "Vibration resetting in $resetseconds seconds"
            runIn(resetseconds,resetvib)
        } else {
        	log.debug "UNKNOWN Cluster and Attribute : $descMap"
        }
	}
}


def installed() {
	log.debug "Device installed so setting values and beginning configuration..."
    sendEvent(name: "battery", value: "100", displayed: true, isStateChange: true)
	configure()
}

def updated() {
	log.debug "Device updated so validating configuration..."
	configure()
}

def ping() {
	log.debug "Device refresh requested..."
	refresh()
}

def resetvib() {
//	log.debug "Setting device to inactive (no vibration)..."
	sendEvent(name: "acceleration", value: "inactive", displayed: true, isStateChange: true)
}

def refresh() {
	log.debug "Refreshing values..."
	[
        "st rattr 0x${device.deviceNetworkId} 1 0x001 0", "delay 200",
        "st rattr 0x${device.deviceNetworkId} 1 0x500 0", "delay 200"
	]
}

def configure() {
	log.debug "Configuration starting..."
	sendEvent(name: "checkInterval", value: 1320, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
    sendEvent(name: "acceleration", value: "inactive", descriptionText: "{{ device.displayName }} was $value", displayed: false)
    log.debug "...bindings..."
	[
		"zdo bind 0x${device.deviceNetworkId} 1 1 0x000 {${device.zigbeeId}} {}", "delay 1000",	// basic cluster
        "zdo bind 0x${device.deviceNetworkId} 1 1 0x001 {${device.zigbeeId}} {}", "delay 1000",	// power cluster
		"zdo bind 0x${device.deviceNetworkId} 1 1 0x003 {${device.zigbeeId}} {}", "delay 1000",	// identify cluster
		"zdo bind 0x${device.deviceNetworkId} 1 1 0x500 {${device.zigbeeId}} {}", "delay 1000",	// IAS Zone cluster
		"send 0x${device.deviceNetworkId} 1 1"
	]
    def resetseconds = vibrationreset ? vibrationreset : 10
    log.debug "...vibration reset is set to $resetseconds seconds..."
    log.debug "Configuration (${device.deviceNetworkId} ${device.zigbeeId}) finished..."
}