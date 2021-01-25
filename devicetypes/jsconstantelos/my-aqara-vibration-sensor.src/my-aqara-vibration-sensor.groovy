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
//		capability "Three Axis"
		capability "Battery"
		capability "Configuration"
		capability "Sensor"
		capability "Refresh"
		capability "Health Check"
    }

	preferences {
		input "forceConfig", "boolean", title: "This is a hack for the new app since there is no Configuration tile like there was in the old Classic app.  Toggle ONCE to force device configuration (any position will force a config)"
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
}

def installed() {
	configure()
}

def updated() {
	configure()
}

def ping() {
	refresh()
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
	sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
    sendEvent(name: "acceleration", value: "inactive", descriptionText: "{{ device.displayName }} was $value", displayed: false)
    log.debug "...bindings..."
	[
		"zdo bind 0x${device.deviceNetworkId} 1 1 0x000 {${device.zigbeeId}} {}", "delay 1000",	// basic cluster
        "zdo bind 0x${device.deviceNetworkId} 1 1 0x001 {${device.zigbeeId}} {}", "delay 1000",	// power cluster
		"zdo bind 0x${device.deviceNetworkId} 1 1 0x003 {${device.zigbeeId}} {}", "delay 1000",	// identify cluster
		"zdo bind 0x${device.deviceNetworkId} 1 1 0x500 {${device.zigbeeId}} {}", "delay 1000",	// IAS Zone cluster
		"send 0x${device.deviceNetworkId} 1 1"
	]
    log.debug "...sending additional Zigbee commmands..."
    [
        zigbee.configureReporting(0x0001, 0x0020, 0x20, 60, 3600, 0x01), "delay 1000",	// power cluster (get battery voltage every hour, or if it changes)
        zigbee.readAttribute(zigbee.IAS_ZONE_CLUSTER, zigbee.ATTRIBUTE_IAS_ZONE_STATUS), "delay 1000",
        zigbee.enrollResponse()
	]
    log.debug "Configuration finished..."
}