/**
 *  Copyright 2015 SmartThings
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
 *	Author: SmartThings (original), JSConstantelos (modifications/updates)
 *	Date: 2013-12-02
  *
 *  Updates:
 *  -------
 *  03-06-2019 : Initial commit.
 *  03-08-2019 : Tweaks for the new ST app, and more thermostat capabilities (operating state).
 *  03-10-2019 : Several updates to clean up code, add comments and debug info, and get thermostat operating state.
 *  03-12-2019 : Cleaned up code, removed "run mode" since it's only used when mode is "auto", which this thermostat does not support.  PowerSource is work in progress.
 *  04-01-2019 : Cleaned up code, and made some wording changes for operating state descriptions.
 *
 */
 
import physicalgraph.zigbee.zcl.DataType
 
metadata {
	definition (name: "My Centralite Thermostat", namespace: "jsconstantelos", author: "SmartThings", mnmn: "SmartThings", ocfDeviceType: "oic.d.thermostat") {
		capability "Actuator"
		capability "Temperature Measurement"
		capability "Thermostat"
        capability "Thermostat Fan Mode"
		capability "Configuration"
		capability "Refresh"
		capability "Sensor"
		capability "Polling"
		capability "Battery"
        capability "Health Check"
        capability "Switch"
		capability "Thermostat Mode"
		capability "Thermostat Cooling Setpoint"
		capability "Thermostat Heating Setpoint"
		capability "Thermostat Operating State"
//        capability "Power Source"

		command "setTemperature"
		command "setThermostatHoldMode"
        command "setThermostatFanMode"
		command "getPowerSource"
        command "offmode"
        command "holdOn"
        command "holdOff"
		command "setLevelUp"
		command "setLevelDown"
        command "getOpsReport"
        command "fanOn"
        command "fanAuto"

		attribute "thermostatHoldMode", "string"
        attribute "thermostatSetpoint", "number"
        attribute "thermostatOperatingState", "number"
		
        fingerprint profileId: "0104", inClusters: "0000,0001,0003,0004,0005,0020,0201,0202,0204,0B05", outClusters: "000A, 0019"

	}

	tiles(scale: 2) {
        multiAttributeTile(name:"summary", type: "", width: 6, height: 4) {
        	tileAttribute("device.temperature", key: "PRIMARY_CONTROL") {
				attributeState("temperature", action:"getOpsReport", label:'${currentValue}°', unit:"F",
				backgroundColors:[
					[value: 31, color: "#153591"],
					[value: 44, color: "#1e9cbb"],
					[value: 59, color: "#90d2a7"],
					[value: 74, color: "#44b621"],
					[value: 84, color: "#f1d801"],
					[value: 95, color: "#d04e00"],
					[value: 96, color: "#bc2323"]
					])
			}
			tileAttribute("device.thermostatSetpoint", key: "VALUE_CONTROL") {
				attributeState("VALUE_UP", action: "setLevelUp")
				attributeState("VALUE_DOWN", action: "setLevelDown")
                attributeState("default", label:'${currentValue}°')
			}
            tileAttribute("device.heatingSetpoint", key: "HEATING_SETPOINT") {
                attributeState("default", label:'${currentValue}°')
            }
            tileAttribute("device.coolingSetpoint", key: "COOLING_SETPOINT") {
                attributeState("default", label:'${currentValue}°')
            }
            tileAttribute("device.thermostatOperatingState", key: "SECONDARY_CONTROL") {
                attributeState("default", label:'${currentValue}', icon:"st.Home.home1")
            }
 		}
        
//Thermostat Mode Control
        standardTile("modeheat", "device.thermostatMode", width: 2, height: 1, inactiveLabel: false, decoration: "flat") {
            state "Heat", label:'', action:"heat", icon:"st.thermostat.heat"
        }
        standardTile("modecool", "device.thermostatMode", width: 2, height: 1, inactiveLabel: false, decoration: "flat") {
            state "Cool", label:'', action:"cool", icon:"st.thermostat.cool"
        }
        standardTile("modeheatemrgcy", "device.thermostatMode", width: 3, height: 1, inactiveLabel: false, decoration: "flat") {
            state "AUXHeat", label:'', action:"emergencyHeat", icon:"st.thermostat.emergency-heat"
        }         
        standardTile("modeoff", "device.thermostatMode", width: 2, height:1, inactiveLabel: false, decoration: "flat") {
            state "OFF", label: '', action:"offmode", icon:"st.thermostat.heating-cooling-off"
        } 

//Fan Mode Control
		standardTile("fanMode", "device.thermostatFanMode", height: 1, width: 2, inactiveLabel: false, decoration: "flat") {
			state "fanAuto", label:'', action:"thermostat.setThermostatFanMode", icon: "st.thermostat.fan-auto"
			state "fanOn", label:'', action:"thermostat.setThermostatFanMode", icon: "st.thermostat.fan-on"
		}

//Temperature Set Point Controls
		controlTile("heatSliderControl", "device.heatingSetpoint", "slider", height: 1, width: 2, inactiveLabel: false, range:"(60..80)") {
			state "setHeatingSetpoint", action:"thermostat.setHeatingSetpoint", backgroundColor:"#d04e00", unit:"F"
		}
		controlTile("coolSliderControl", "device.coolingSetpoint", "slider", height: 1, width: 2, inactiveLabel: false, range:"(60..80)") {
			state "setCoolingSetpoint", action:"thermostat.setCoolingSetpoint", backgroundColor: "#003CEC", unit:"F"
		}

//Additional thermostat capabilities
		standardTile("refresh", "device.temperature", height: 1, width: 3, inactiveLabel: false, decoration: "flat") {
			state "default", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		standardTile("configure", "device.configure", height: 1, width: 3, inactiveLabel: false, decoration: "flat") {
			state "configure", label:'', action:"configuration.configure", icon:"st.secondary.configure"
		}
		valueTile("battery", "device.battery", decoration: "flat", inactiveLabel: false, width: 3, height: 1) {
			state "battery", label:'${currentValue}% Battery', unit:""
		}
		standardTile("holdMode", "device.thermostatHoldMode", height: 1, width: 3, inactiveLabel: false, decoration: "flat") {
			state "holdOff", label:'Hold Off', action:"setThermostatHoldMode", nextState:"holdOff"
			state "holdOn", label:'Hold On', action:"setThermostatHoldMode", nextState:"holdOn"
		}
		standardTile("powerMode", "device.powerSource", height: 1, width: 2, inactiveLabel: false, decoration: "flat") {
			state "24VAC", label:'AC', action:"getPowerSource", icon: "st.switches.switch.on", backgroundColor: "#79b821"
			state "Battery", label:'Battery', action:"getPowerSource", icon: "st.switches.switch.off"
		}
        
//Miscellaneous tiles used in this DTH
		valueTile("temperature", "device.temperature", width: 2, height: 2) {
			state("temperature", label:'${currentValue}°', icon:"st.Home.home1", backgroundColor:"#38a815")
		}

//Tiles to display in the mobile app.  Main is used for the Room and Things view, and Details is for the Device view.
		main(["temperature"])
        details(["summary", "heatSliderControl", "fanMode", "coolSliderControl", "modeheat", "modecool", "modeoff", "battery", "holdMode", "refresh", "configure"])
	}
}
//*************
// parse events into clusters and attributes and do something with the data we received from the thermostat
//*************
def parse(String description) {
//	log.debug "Parse description : $description"
    if ((description?.startsWith("catchall:")) || (description?.startsWith("read attr -"))) {
		def descMap = zigbee.parseDescriptionAsMap(description)
        def tempScale = location.temperatureScale
//		log.debug "Desc Map : $descMap"
        // TEMPERATURE
		if (descMap.cluster == "0201" && descMap.attrId == "0000") {
        	if (descMap.value != null) {
            	def trimvalue = descMap.value[-4..-1]
                def celsius = Integer.parseInt(trimvalue, 16) / 100
                def fahrenheit = String.format("%3.1f",celsiusToFahrenheit(celsius))
                if (tempScale == "F") {
                    sendEvent("name": "temperature", "value": fahrenheit, "displayed": true)
                    log.debug "TEMPERATURE is : ${fahrenheit}${tempScale}"
                } else {
                    sendEvent("name": "temperature", "value": celsius, "displayed": true)
                    log.debug "TEMPERATURE is : ${celsius}${tempScale}"
                }
            }
        // COOLING SETPOINT
		} else if (descMap.cluster == "0201" && descMap.attrId == "0011") {
        	if (descMap.value != null) {
            	def trimvalue = descMap.value[-4..-1]
                def celsius = Integer.parseInt(trimvalue, 16) / 100
                def fahrenheit = String.format("%3.1f",celsiusToFahrenheit(celsius))
                if (tempScale == "F") {
                    sendEvent("name": "coolingSetpoint", "value": fahrenheit, "displayed": true)
                    log.debug "COOLING SETPOINT is : ${fahrenheit}${tempScale}"
                    if (device.currentValue("thermostatMode") == "Cool") {
                    	sendEvent("name": "thermostatSetpoint", "value": fahrenheit, "displayed": false)
                	}
                } else {
                    sendEvent("name": "coolingSetpoint", "value": celsius, "displayed": true)
                    log.debug "COOLING SETPOINT is : ${celsius}${tempScale}"
                    if (device.currentValue("thermostatMode") == "Cool") {
                    	sendEvent("name": "thermostatSetpoint", "value": celsius, "displayed": false)
                	}
                }
            }
        // HEATING SETPOINT
		} else if (descMap.cluster == "0201" && descMap.attrId == "0012") {
        	if (descMap.value != null) {
            	def trimvalue = descMap.value[-4..-1]
                def celsius = Integer.parseInt(trimvalue, 16) / 100
                def fahrenheit = String.format("%3.1f",celsiusToFahrenheit(celsius))
                if (tempScale == "F") {
                    sendEvent("name": "heatingSetpoint", "value": fahrenheit, "displayed": true)
                    log.debug "HEATING SETPOINT is : ${fahrenheit}${tempScale}"
                    if (device.currentValue("thermostatMode") == "Heat" || "AUXHeat") {
                    	sendEvent("name": "thermostatSetpoint", "value": fahrenheit, "displayed": false)
                	}
                } else {
                    sendEvent("name": "heatingSetpoint", "value": celsius, "displayed": true)
                    log.debug "HEATING SETPOINT is : ${celsius}${tempScale}"
                    if (device.currentValue("thermostatMode") == "Heat" || "AUXHeat") {
                    	sendEvent("name": "thermostatSetpoint", "value": celsius, "displayed": false)
                	}
                }
            }
        // THERMOSTAT MODE
		} else if (descMap.cluster == "0201" && descMap.attrId == "001c") {
        	def trimvalue = descMap.value[-2..-1]
			def modeValue = getModeMap()[trimvalue]
            sendEvent("name": "thermostatMode", "value": modeValue, "displayed": true)
            log.debug "THERMOSTAT MODE is : ${modeValue}"
        // THERMOSTAT FAN MODE
		} else if (descMap.cluster == "0202" && descMap.attrId == "0000") {
        	def trimvalue = descMap.value[-2..-1]
			def modeValue = getFanModeMap()[trimvalue]
            sendEvent("name": "thermostatFanMode", "value": modeValue, "displayed": true)
            log.debug "THERMOSTAT FAN MODE is : ${modeValue}"
        // BATTERY LEVEL
		} else if (descMap.cluster == "0001" && descMap.attrId == "0020") {
            def vBatt = Integer.parseInt(descMap.value,16) / 10
            def batteryValue =  ((vBatt - 2.1) / (3.0 - 2.1) * 100) as int
            sendEvent("name": "battery", "value": batteryValue, "displayed": true)
            log.debug "BATTERY LEVEL is : ${batteryValue}"
        // THERMOSTAT OPERATING STATE
		} else if (descMap.cluster == "0201" && descMap.attrId == "0029") {
        	def trimvalue = descMap.value[-4..-1]
            def stateValue = getThermostatOperatingState()[trimvalue]
            sendEvent("name": "thermostatOperatingState", "value": stateValue, "displayed": true)
            log.debug "THERMOSTAT OPERATING STATE is : ${stateValue}"
        // THERMOSTAT HOLD MODE
		} else if (descMap.cluster == "0201" && descMap.attrId == "0023") {
        	def trimvalue = descMap.value[-2..-1]
            def modeValue = getHoldModeMap()[trimvalue]
            sendEvent("name": "thermostatHoldMode", "value": modeValue, "displayed": true)
            log.debug "THERMOSTAT HOLD MODE is : ${modeValue}"
        // POWER SOURCE (work in progress)
		} else if (descMap.cluster == "0001" && descMap.attrId == "003e") {
        	def trimvalue = descMap.value[-2..-1]
            def sourceValue = getPowerSourceMap()[trimvalue]
            sendEvent("name": "powerSource", "value": sourceValue, "displayed": true)
            log.debug "POWER SOURCE is : ${sourceValue}"
		} else {
        	log.debug "UNKNOWN Cluster and Attribute : $descMap"
        }
	} else {
    	log.debug "UNKNOWN data from device : $description"
    }
}

def getOpsReport() {["st rattr 0x${device.deviceNetworkId} 1 0x201 0x29"]}  // Forces the thermostat to send it's operating state (used for testing).

def getPowerSource() { //This is only used for figuring out how/where to get power source data from to determine if we're on AC or batteries.  Work in progress.
	[
    "st rattr 0x${device.deviceNetworkId} 1 0x001 0x00", "delay 1000",
    "st rattr 0x${device.deviceNetworkId} 1 0x001 0x3e", "delay 1000",
    "st rattr 0x${device.deviceNetworkId} 1 0x000 0x07"
    ]
}

def getModeMap() {
	[
    "00":"OFF",
    "01":"Auto",
    "03":"Cool",
    "04":"Heat",
    "05":"AUXHeat",
    "06":"Precooling",
    "07":"Fan Only"
    ]
}

def getHoldModeMap() {
	[
    "00":"holdOff",
    "01":"holdOn"
    ]
}

def getPowerSourceMap() {  // These don't look right.  This is a work in progress.
	[
    "00000000":"24VAC",
    "00000001":"Battery"
    ]
}

def getFanModeMap() {
	[
    "00":"fanOff",
    "04":"fanOn",
    "05":"fanAuto"
    ]
}

def getThermostatOperatingState() {
	[
    "0000":"Mode is ${device.currentValue("thermostatMode")} and is Idle",
    "0001":"Mode is ${device.currentValue("thermostatMode")} and is Heating",
    "0002":"Mode is ${device.currentValue("thermostatMode")} and is Cooling",
    "0004":"Mode is ${device.currentValue("thermostatMode")} and the Fan is Running",
    "0005":"Mode is ${device.currentValue("thermostatMode")} and is Heating",
    "0006":"Mode is ${device.currentValue("thermostatMode")} and is Cooling",
    "0008":"Mode is ${device.currentValue("thermostatMode")} and is Heating",
    "0009":"Mode is ${device.currentValue("thermostatMode")} and is Heating",
    "000A":"Mode is ${device.currentValue("thermostatMode")} and is Heating",
    "000D":"Mode is ${device.currentValue("thermostatMode")} and is Heating",
    "0010":"Mode is ${device.currentValue("thermostatMode")} and is Cooling",
    "0012":"Mode is ${device.currentValue("thermostatMode")} and is Cooling",
    "0014":"Mode is ${device.currentValue("thermostatMode")} and is Cooling",
    "0015":"Mode is ${device.currentValue("thermostatMode")} and is Cooling"
    ]
}

//********************************
// Send commands to the thermostat
//********************************

def setLevelUp(){
	log.debug "Adjusting temperature UP one degree"
    if (device.currentValue("thermostatMode") == "Heat") {
    	int nextLevel = device.currentValue("heatingSetpoint") + 1
    	setHeatingSetpoint(nextLevel)
	} else if (device.currentValue("thermostatMode") == "AUXHeat") {   
        int nextLevel = device.currentValue("heatingSetpoint") + 1
        setHeatingSetpoint(nextLevel)
	} else if (device.currentValue("thermostatMode") == "Cool") {   
        int nextLevel = device.currentValue("coolingSetpoint") + 1
        setCoolingSetpoint(nextLevel)
	} else {
    	log.debug "Can't adjust set point when unit isn't in heat, e-heat, or cool mode."
    }
}

def setLevelDown(){
	log.debug "Adjusting temperature DOWN one degree"
    if (device.currentValue("thermostatMode") == "Heat") {
    	int nextLevel = device.currentValue("heatingSetpoint") - 1
    	setHeatingSetpoint(nextLevel)
	} else if (device.currentValue("thermostatMode") == "AUXHeat") {   
        int nextLevel = device.currentValue("heatingSetpoint") - 1
        setHeatingSetpoint(nextLevel)
	} else if (device.currentValue("thermostatMode") == "Cool") {   
        int nextLevel = device.currentValue("coolingSetpoint") - 1
        setCoolingSetpoint(nextLevel)
	} else {
    	log.debug "Can't adjust set point when unit isn't in heat, e-heat, or cool mode."
    }
}

def setHeatingSetpoint(degrees) {
	if (device.currentValue("thermostatMode") == "Heat" || "AUXHeat") {
        if (degrees != null) {
        	log.debug "Setting HEAT set point to ${degrees}"
            def degreesInteger = Math.round(degrees)
            sendEvent("name": "heatingSetpoint", "value": degreesInteger)
            sendEvent("name": "thermostatSetpoint", "value": degreesInteger)
            def celsius = (getTemperatureScale() == "C") ? degreesInteger : (fahrenheitToCelsius(degreesInteger) as Double).round(2)
            ["st wattr 0x${device.deviceNetworkId} 1 0x201 0x12 0x29 {" + hex(celsius * 100) + "}", "delay 10000",
	       	"st rattr 0x${device.deviceNetworkId} 1 0x201 0x29"]
        }
    }
}

def setCoolingSetpoint(degrees) {
	if (device.currentValue("thermostatMode") == "Cool") {
        if (degrees != null) {
        	log.debug "Setting COOL set point to ${degrees}"
            def degreesInteger = Math.round(degrees)
            sendEvent("name": "coolingSetpoint", "value": degreesInteger)
            sendEvent("name": "thermostatSetpoint", "value": degreesInteger)
            def celsius = (getTemperatureScale() == "C") ? degreesInteger : (fahrenheitToCelsius(degreesInteger) as Double).round(2)
            ["st wattr 0x${device.deviceNetworkId} 1 0x201 0x11 0x29 {" + hex(celsius * 100) + "}", "delay 10000",
	       	"st rattr 0x${device.deviceNetworkId} 1 0x201 0x29"]
        }
    }
}

def setThermostatFanMode() {
	def currentFanMode = device.currentState("thermostatFanMode")?.value
	def returnCommand
	switch (currentFanMode) {
		case "fanAuto":
			returnCommand = fanOn()
			break
		case "fanOn":
			returnCommand = fanAuto()
			break
	}
	if(!currentFanMode) { returnCommand = fanAuto() }
	returnCommand
}

def setThermostatHoldMode() {
	def currentHoldMode = device.currentState("thermostatHoldMode")?.value
	def returnCommand
	switch (currentHoldMode) {
		case "holdOff":
			returnCommand = holdOn()
			break
		case "holdOn":
			returnCommand = holdOff()
			break
	}
	if(!currentHoldMode) { returnCommand = holdOff() }
	returnCommand
}

def offmode() {
	log.debug "Setting mode to OFF"
	sendEvent("name":"thermostatMode", "value":"OFF")
    [
		"st wattr 0x${device.deviceNetworkId} 1 0x201 0x1C 0x30 {00}", "delay 10000",
       	"st rattr 0x${device.deviceNetworkId} 1 0x201 0x29"
	]
}

def cool() {
	log.debug "Setting mode to COOL"
	sendEvent("name":"thermostatMode", "value":"Cool")
    sendEvent("name": "thermostatSetpoint", "value": device.currentState("coolingSetpoint")?.value)
    [
		"st wattr 0x${device.deviceNetworkId} 1 0x201 0x1C 0x30 {03}", "delay 10000",
       	"st rattr 0x${device.deviceNetworkId} 1 0x201 0x29"
	]
}

def heat() {
	log.debug "Setting mode to HEAT"
	sendEvent("name":"thermostatMode", "value":"Heat")
    sendEvent("name": "thermostatSetpoint", "value": device.currentState("heatingSetpoint")?.value)
    [
		"st wattr 0x${device.deviceNetworkId} 1 0x201 0x1C 0x30 {04}", "delay 10000",
       	"st rattr 0x${device.deviceNetworkId} 1 0x201 0x29",
	]
}

def emergencyHeat() {
	log.debug "Setting mode to EMERGENCY HEAT"
	sendEvent("name":"thermostatMode", "value":"AUXHeat")
    sendEvent("name": "thermostatSetpoint", "value": device.currentState("heatingSetpoint")?.value)
    [
		"st wattr 0x${device.deviceNetworkId} 1 0x201 0x1C 0x30 {05}", "delay 10000",
       	"st rattr 0x${device.deviceNetworkId} 1 0x201 0x29"
	]
}

def on() {
	fanOn()
}

def off() {
	fanAuto()
}

def fanOn() {
	log.debug "Setting fan to ON"
	sendEvent("name":"thermostatFanMode", "value":"fanOn")
    [
		"st wattr 0x${device.deviceNetworkId} 1 0x202 0 0x30 {04}", "delay 10000",
       	"st rattr 0x${device.deviceNetworkId} 1 0x201 0x29"
	]
}

def fanAuto() {
	log.debug "Setting fan to AUTO"
	sendEvent("name":"thermostatFanMode", "value":"fanAuto")
    [
		"st wattr 0x${device.deviceNetworkId} 1 0x202 0 0x30 {05}", "delay 10000",
       	"st rattr 0x${device.deviceNetworkId} 1 0x201 0x29"
	]
}

def holdOn() {
	log.debug "Setting hold to ON"
	sendEvent("name":"thermostatHoldMode", "value":"holdOn")
    [
		"st wattr 0x${device.deviceNetworkId} 1 0x201 0x23 0x30 {01}", "delay 5000",
       	"st rattr 0x${device.deviceNetworkId} 1 0x201 0x29"
	]
}

def holdOff() {
	log.debug "Setting hold to OFF"
	sendEvent("name":"thermostatHoldMode", "value":"holdOff")
    [
		"st wattr 0x${device.deviceNetworkId} 1 0x201 0x23 0x30 {00}", "delay 5000",
       	"st rattr 0x${device.deviceNetworkId} 1 0x201 0x29"
	]
}

// Commment out below if no C-wire since it will kill the batteries.
def poll() {
	refresh()
}

// PING is used by Device-Watch in attempt to reach the Device
def ping() {
	refresh()
}

def configure() {
	log.debug "Configuration starting..."
	sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 1 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
	[
		"zdo bind 0x${device.deviceNetworkId} 1 1 0x000 {${device.zigbeeId}} {}", "delay 1000",
        "zdo bind 0x${device.deviceNetworkId} 1 1 0x001 {${device.zigbeeId}} {}", "delay 1000",
		"zdo bind 0x${device.deviceNetworkId} 1 1 0x201 {${device.zigbeeId}} {}", "delay 1000",
		"zdo bind 0x${device.deviceNetworkId} 1 1 0x202 {${device.zigbeeId}} {}", "delay 1000",
		"zcl global send-me-a-report 1 0x20 0x20 3600 86400 {01}", "delay 1000", // Battery report
		"send 0x${device.deviceNetworkId} 1 1"
	]
    zigbee.configureReporting(0x0201, 0x0029, 0x19, 0, 0, null) // Thermostat operating state report to send whenever it changes, or at a minimum of every minute.  This is also known as Running State (Zen).
}

def refresh() {
	log.debug "Refreshing values..."
	[
		"st rattr 0x${device.deviceNetworkId} 1 0x000 0x07", "delay 200",
		"st rattr 0x${device.deviceNetworkId} 1 0x201 0", "delay 200",
		"st rattr 0x${device.deviceNetworkId} 1 0x201 0x11", "delay 200",
  		"st rattr 0x${device.deviceNetworkId} 1 0x201 0x12", "delay 200",
		"st rattr 0x${device.deviceNetworkId} 1 0x201 0x1C", "delay 200",
		"st rattr 0x${device.deviceNetworkId} 1 0x201 0x23", "delay 200",
        "st rattr 0x${device.deviceNetworkId} 1 0x201 0x29", "delay 200",
		"st rattr 0x${device.deviceNetworkId} 1 0x001 0x20", "delay 200",
        "st rattr 0x${device.deviceNetworkId} 1 0x001 0x3e", "delay 200",
		"st rattr 0x${device.deviceNetworkId} 1 0x202 0"
	]
    zigbee.configureReporting(0x0201, 0x0029, 0x19, 0, 0, null)
}

private hex(value) {
	new BigInteger(Math.round(value).toString()).toString(16)
}