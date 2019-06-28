/**
 *  Virtual Thermostat Child App
 *
 *  Based on Its too cold code by SmartThings and Brian Critchlow
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
definition(
    name: "Virtual Thermostat Child App",
    namespace: "jsconstantelos",
    author: "jscgs350",
    description: "Virtual Thermostat",
    category: "My Apps",
    parent: "jsconstantelos:Virtual Thermostat Parent App",
    iconUrl: "https://graph.api.smartthings.com/api/devices/icons/st.Weather.weather9-icn",
    iconX2Url: "https://graph.api.smartthings.com/api/devices/icons/st.Weather.weather9-icn?displaySize=2x"
)

preferences {
	section("Change the setpoint of which thermostat:") {
		input "thermostat", "capability.thermostat"
	}
	section("Based on which virtual dimmer:") {
		input "switchDevice", "capability.switchLevel", required: false
	}
}

def installed() {
	log.debug "Installed..."
	subscribe(switchDevice, "level", setPointHandler)
}

def updated() {
	log.debug "Updated..."
	unsubscribe()
	subscribe(switchDevice, "level", setPointHandler)
}

def setPointHandler(evt) {
	log.debug "Setpoint being changed to ${evt.value}..."
	thermostat.setTemperature(evt.value)
}