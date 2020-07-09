/**
 *	Copyright 2015 SmartThings
 *
 *	Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *	in compliance with the License. You may obtain a copy of the License at:
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *	on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *	for the specific language governing permissions and limitations under the License.
 *
 */

metadata {
	definition (name: "My ZigBee Garage Door Relay", namespace: "jsconstantelos", author: "SmartThings", ocfDeviceType: "oic.d.switch", genericHandler: "Zigbee") {
		capability "Actuator"
		capability "Configuration"
		capability "Refresh"
		capability "Switch"
		capability "Health Check"
	}

	tiles(scale: 2) {
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'Push', action:"switch.off", icon:"st.doors.garage.garage-closed", backgroundColor:"#00A0DC"
				attributeState "off", label:'Push', action:"switch.on", icon:"st.doors.garage.garage-closed", backgroundColor:"#ffffff"
			}
		}
		standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		main "switch"
		details(["switch", "refresh"])
	}
}

// Parse incoming device messages to generate events
def parse(String description) {
//	log.debug "description is $description"
	def event = zigbee.getEvent(description)
	if (event) {
		sendEvent(event)
        push()
	}
	else {
		log.warn "DID NOT PARSE MESSAGE for description : $description"
		log.debug zigbee.parseDescriptionAsMap(description)
		log.debug zigbee.getEvent(description)
	}
}

def push() {
	log.debug "Actuating device..."
    def cmds = delayBetween([
		zigbee.on(),
    	zigbee.off()
    ],50)
    cmds
}

def open() {
	push()
}

def close() {
	push()
}

def off() {
	push()
}

def on() {
	push()
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
	return refresh()
}

def refresh() {
	zigbee.onOffRefresh() + zigbee.onOffConfig()
}

def configure() {
	// Device-Watch allows 2 check-in misses from device + ping (plus 2 min lag time)
	sendEvent(name: "checkInterval", value: 2 * 10 * 60 + 2 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
	log.debug "Configuring Reporting and Bindings."
	zigbee.onOffRefresh() + zigbee.onOffConfig()
}