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
 *  01-30-2021 : Initial commit.
 */

import physicalgraph.zigbee.zcl.DataType

metadata {
    definition (name: "My Aqara Vibration Sensor", namespace: "jsconstantelos", author: "jsconstantelos") {
    	capability "Acceleration Sensor"
		capability "Battery"
		capability "Configuration"
		capability "Sensor"
		capability "Refresh"
//		capability "Health Check"	// This sensor does not send data to "keep it alive" (aka online) unless there is activity.  Since that may not happen for a very long time, this capability is not used.

		fingerprint profileId: "0104", inClusters: "0000,0500,0003,0001", outClusters: "0003,0019", manufacturer: "LUMI", model: "lumi.vibration.agl01", deviceJoinName: "Aqara Vibration Sensor"
    }

	preferences {
        input "vibrationreset", "number", title: "Reporting Interval (in seconds)", description: "Reporting interval to keep showing vibration before resetting to inactive (default = 10 seconds)", range: "1..60"
        input("sensitivity", "enum",
              title: "Sensitivity Level",
              description: "What should the sensitivity level be (default = high)?",
              options: [1: "High",
                        11: "Medium",
                        21: "Low"])
	}
    
    // These tiles are not needed for the new ST mobile app.  R.I.P. ST Classic app.
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
//	log.debug "Incoming data from device : $description"
    if (description?.startsWith("catchall:")) {
		def descMap = zigbee.parseDescriptionAsMap(description)
		log.debug "Catchall Map Data : $descMap"
        if (descMap.clusterId == "0000") {
        	def dataElements = descMap.data.size()
        	log.debug "Found ${dataElements} data elements for attribute ID ${descMap.attrId} : ${descMap.data}"
        }
	}
    if (description?.startsWith("read attr -")) {
		def descMap = zigbee.parseDescriptionAsMap(description)
//		log.debug "Read Attr Map Data : $descMap"
		if (descMap.cluster == "0001" && descMap.attrId == "0020") {
            def vBatt = Integer.parseInt(descMap.value,16) / 10
            def pct = (vBatt - 2.1) / (3 - 2.1)
            def roundedPct = Math.round(pct * 100)
            if (roundedPct <= 0) roundedPct = 1
            def batteryValue = Math.min(100, roundedPct)
            sendEvent(name: "battery", value: batteryValue, displayed: true, isStateChange: true)
		} else if (descMap.cluster == "0500" && descMap.attrId == "002d") {
            sendEvent(name: "acceleration", value: "active", displayed: true, isStateChange: true)
            def resetseconds = vibrationreset ? vibrationreset : 10
            runIn(resetseconds,resetvib)
        } else {
        	log.debug "UNKNOWN Cluster and Attribute : $descMap"
        }
	}
}

def installed() {
	log.debug "Device installed so setting values and beginning configuration..."
    sendEvent(name: "battery", value: "100", displayed: true, isStateChange: true)	// Sensor only sends out a low battery event, so we need to set this value here and wait for the sensor to send a low value event later.
	configure()
}

def updated() {
	log.debug "Device updated so validating configuration..."
	configure()
}

def ping() {
	refresh()
}

def resetvib() {
//	log.debug "Setting device to inactive (no activity)..."
	sendEvent(name: "acceleration", value: "inactive", displayed: true, isStateChange: true)
}

def refresh() {
	log.debug "Device refresh requested..."
	[
        "st rattr 0x${device.deviceNetworkId} 1 0x000 0", "delay 2000",
        "st rattr 0x${device.deviceNetworkId} 1 0x001 0", "delay 2000",
        "st rattr 0x${device.deviceNetworkId} 1 0x500 0", "delay 2000"
	]
    zigbee.readAttribute( 0x0000, 0xFF0D)	// This cluster/attribute should contain certain settings we're interested in.
}

def configure() {
	log.debug "Configuration starting..."
	sendEvent(name: "checkInterval", value: 86400, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
    sendEvent(name: "acceleration", value: "inactive", descriptionText: "{{ device.displayName }} was $value", displayed: false)
    log.debug "...perform Zigbee bindings..."
	[
		"zdo bind 0x${device.deviceNetworkId} 1 1 0x000 {${device.zigbeeId}} {}", "delay 2000",	// basic cluster
        "zdo bind 0x${device.deviceNetworkId} 1 1 0x001 {${device.zigbeeId}} {}", "delay 2000",	// power cluster
		"zdo bind 0x${device.deviceNetworkId} 1 1 0x003 {${device.zigbeeId}} {}", "delay 2000",	// identify cluster
		"zdo bind 0x${device.deviceNetworkId} 1 1 0x500 {${device.zigbeeId}} {}", "delay 2000",	// IAS Zone cluster
		"send 0x${device.deviceNetworkId} 1 1"
	]
    log.debug "...attempting to set battery reporting frequency..."
    [
    	zigbee.configureReporting(0x0001, 0x0020, DataType.UINT8, 60, 3600, 0x01), "delay 2000",	// get battery voltage
        zigbee.configureReporting(0x0001, 0x0021, DataType.UINT8, 60, 3600, 0x01), "delay 2000"		// get battery %
    ]
    log.debug "...attempting to set sensitivity level..."
    if (senselevel == "1") {
    	zigbee.writeAttribute(0x0000, 0xFF0D, DataType.UINT8, 0x01)									// set sensitivity level (0x15=low, 0x0B=medium, 0x01=high) data type 0x20
    } else if (senselevel == "11") {
    	zigbee.writeAttribute(0x0000, 0xFF0D, DataType.UINT8, 0x0b)									// set sensitivity level (0x15=low, 0x0B=medium, 0x01=high) data type 0x20
    } else if (senselevel == "21") {
    	zigbee.writeAttribute(0x0000, 0xFF0D, DataType.UINT8, 0x15)									// set sensitivity level (0x15=low, 0x0B=medium, 0x01=high) data type 0x20
    } else {
    	zigbee.writeAttribute(0x0000, 0xFF0D, DataType.UINT8, 0x01)									// set sensitivity level (0x15=low, 0x0B=medium, 0x01=high) data type 0x20
    }
    def resetseconds = vibrationreset ? vibrationreset : 10
    log.debug "...vibration reset is set to $resetseconds seconds..."
    log.debug "Configuration (${device.deviceNetworkId} ${device.zigbeeId}) finished..."
}