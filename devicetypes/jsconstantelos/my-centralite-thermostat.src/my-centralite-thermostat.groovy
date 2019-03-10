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
 *	Author: SmartThings
 *	Date: 2013-12-02
  *
 *  Updates:
 *  -------
 *  03-06-2019 : Initial commit.
 *  03-08-2019 : Tweaks for the new ST app, and more thermostat capabilities (operating state).
 *  03-10-2019 : Several updates to clean up code, add comments and debug info, and get thermostat operating state.
 *
 */
 
import physicalgraph.zigbee.zcl.DataType
 
metadata {
	definition (name: "My Centralite Thermostat", namespace: "jsconstantelos", author: "SmartThings", mnmn: "SmartThings", ocfDeviceType: "oic.d.thermostat", vid: "generic-thermostat-1") {
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
		capability "Power Source"

		command "setTemperature"
		command "setThermostatHoldMode"
		command "getPowerSource"
        command "offmode"
        command "holdOn"
        command "holdOff"
		command "setLevelUp"
		command "setLevelDown"
        command "getOpsReport"

		attribute "temperatureScale", "string"
		attribute "thermostatHoldMode", "string"
		attribute "powerSource", "string"
        attribute "thermostatSetpoint", "number"
        attribute "thermostatRunMode", "number"
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
        standardTile("modeheat", "device.thermostatMode", width: 3, height: 1, inactiveLabel: false, decoration: "flat") {
            state "heat", label:'', action:"heat", icon:"st.thermostat.heat"
        }
        standardTile("modecool", "device.thermostatMode", width: 3, height: 1, inactiveLabel: false, decoration: "flat") {
            state "cool", label:'', action:"cool", icon:"st.thermostat.cool"
        }
        standardTile("modeheatemrgcy", "device.thermostatMode", width: 3, height: 1, inactiveLabel: false, decoration: "flat") {
            state "emergencyHeat", label:'', action:"emergencyHeat", icon:"st.thermostat.emergency-heat"
        }         
        standardTile("modeoff", "device.thermostatMode", width: 3, height:1, inactiveLabel: false, decoration: "flat") {
            state "off", label: '', action:"offmode", icon:"st.thermostat.heating-cooling-off"
        } 

//Fan Mode Control
		standardTile("fanMode", "device.thermostatFanMode", height: 1, width: 3, inactiveLabel: false, decoration: "flat") {
			state "fanAuto", label:'', action:"thermostat.setThermostatFanMode", icon: "st.thermostat.fan-auto"
			state "fanOn", label:'', action:"thermostat.setThermostatFanMode", icon: "st.thermostat.fan-on"
		}

//Temperature Set Point Controls
		controlTile("heatSliderControl", "device.heatingSetpoint", "slider", height: 1, width: 3, inactiveLabel: false, range:"(60..80)") {
			state "setHeatingSetpoint", action:"thermostat.setHeatingSetpoint", backgroundColor:"#d04e00", unit:""
		}
		controlTile("coolSliderControl", "device.coolingSetpoint", "slider", height: 1, width: 3, inactiveLabel: false, range:"(60..80)") {
			state "setCoolingSetpoint", action:"thermostat.setCoolingSetpoint", backgroundColor: "#003CEC", unit:""
		}

//Additional thermostat capabilities
		standardTile("refresh", "device.temperature", height: 1, width: 3, inactiveLabel: false, decoration: "flat") {
			state "default", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		standardTile("configure", "device.configure", height: 1, width: 3, inactiveLabel: false, decoration: "flat") {
			state "configure", label:'', action:"configuration.configure", icon:"st.secondary.configure"
		}
		valueTile("battery", "device.battery", decoration: "flat", inactiveLabel: false, width: 2, height: 1) {
			state "battery", label:'${currentValue}% Battery', unit:""
		}
		standardTile("holdMode", "device.thermostatHoldMode", height: 1, width: 2, inactiveLabel: false, decoration: "flat") {
			state "holdOff", label:'Hold Off', action:"setThermostatHoldMode", nextState:"holdOff"
			state "holdOn", label:'Hold On', action:"setThermostatHoldMode", nextState:"holdOn"
		}
		standardTile("powerMode", "device.powerSource", height: 1, width: 2, inactiveLabel: false, decoration: "flat") {
			state "24VAC", label:'AC', icon: "st.switches.switch.on", backgroundColor: "#79b821"
			state "Battery", label:'Battery', icon: "https://raw.githubusercontent.com/jsconstantelos/SmartThings/master/img/battery-icon-614x460.png", backgroundColor:"#ffb3ff"
		}
        
//Miscellaneous tiles used in this DTH
		valueTile("thermostatMode", "device.thermostatMode", inactiveLabel: false, decoration: "flat", width: 3, height: 1) {
			state "off", label:'Mode is OFF', icon:"st.Home.home1"
            state "heat", label:'MODE is Heat', icon:"st.Home.home1"
            state "cool", label:'MODE is Cool', icon:"st.Home.home1"
            state "emergencyHeat", label:'AUX Heat', icon:"st.Home.home1"
		}
		valueTile("temperature", "device.temperature", width: 2, height: 2) {
			state("temperature", label:'${currentValue}°', icon:"st.thermostat.ac.air-conditioning", backgroundColor:"#38a815")
		}

//Tiles to display in the mobile app.  Main is used for the Room and Things view, and Details is for the Device view.
		main(["temperature"])
        details(["summary", "thermostatMode", "fanMode", "heatSliderControl", "coolSliderControl", "modeheat", "modecool", "modeheatemrgcy", "modeoff", "battery", "powerMode", "holdMode", "refresh", "configure"])
	}
}

// parse events into attributes and do something with the data we got from the thermostat
def parse(String description) {
//	log.debug "Parse description : $description"
	if (description?.startsWith("read attr -")) {
		def descMap = parseDescriptionAsMap(description)
//		log.debug "Desc Map : $descMap"
        // TEMPERATURE
		if (descMap.cluster == "0201" && descMap.attrId == "0000") {
        	if (descMap.value != null) {
            	def trimvalue = descMap.value[-4..-1]
                def celsius = Integer.parseInt(trimvalue, 16) / 100
                def fahrenheit = String.format("%3.1f",celsiusToFahrenheit(celsius))
                sendEvent("name": "temperature", "value": fahrenheit, "displayed": true)
                log.debug "TEMPERATURE is : $fahrenheit"
            }
        // COOLING SETPOINT
		} else if (descMap.cluster == "0201" && descMap.attrId == "0011") {
        	if (descMap.value != null) {
            	def trimvalue = descMap.value[-4..-1]
                def celsius = Integer.parseInt(trimvalue, 16) / 100
                def fahrenheit = String.format("%3.1f",celsiusToFahrenheit(celsius))
                sendEvent("name": "coolingSetpoint", "value": fahrenheit, "displayed": true)
                log.debug "COOLING SETPOINT is : $fahrenheit"
                if (device.currentValue("thermostatMode") == "cool") {
                    sendEvent("name": "thermostatSetpoint", "value": fahrenheit, "displayed": false)
                }
            }
        // HEATING SETPOINT
		} else if (descMap.cluster == "0201" && descMap.attrId == "0012") {
        	if (descMap.value != null) {
            	def trimvalue = descMap.value[-4..-1]
                def celsius = Integer.parseInt(trimvalue, 16) / 100
                def fahrenheit = String.format("%3.1f",celsiusToFahrenheit(celsius))
                sendEvent("name": "heatingSetpoint", "value": fahrenheit, "displayed": true)
                log.debug "HEATING SETPOINT is : $fahrenheit"
                if (device.currentValue("thermostatMode") == "heat" || "emergencyHeat") {
                    sendEvent("name": "thermostatSetpoint", "value": fahrenheit, "displayed": false)
                }
            }
        // THERMOSTAT MODE
		} else if (descMap.cluster == "0201" && descMap.attrId == "001c") {
        	def trimvalue = descMap.value[-2..-1]
			def modeValue = getModeMap()[trimvalue]
            sendEvent("name": "thermostatMode", "value": modeValue, "displayed": true)
            log.debug "THERMOSTAT MODE is : $modeValue"
        // THERMOSTAT FAN MODE
		} else if (descMap.cluster == "0202" && descMap.attrId == "0000") {
        	def trimvalue = descMap.value[-2..-1]
			def modeValue = getFanModeMap()[trimvalue]
            sendEvent("name": "thermostatFanMode", "value": modeValue, "displayed": true)
            log.debug "THERMOSTAT FAN MODE is : $modeValue"
        // BATTERY LEVEL
		} else if (descMap.cluster == "0001" && descMap.attrId == "0020") {
            def vBatt = Integer.parseInt(descMap.value,16) / 10
            def batteryValue =  ((vBatt - 2.1) / (3.0 - 2.1) * 100) as int
            sendEvent("name": "battery", "value": batteryValue, "displayed": true)
            log.debug "BATTERY LEVEL is : $batteryValue"
        // THERMOSTAT RUN MODE
		} else if (descMap.cluster == "0201" && descMap.attrId == "001e") {
        	def trimvalue = descMap.value[-2..-1]
			def runValue = getThermostatRunModeMap()[trimvalue]
            sendEvent("name": "thermostatRunMode", "value": runValue, "displayed": false)
            log.debug "THERMOSTAT RUN MODE is : $runValue"
        // THERMOSTAT OPERATING STATE
		} else if (descMap.cluster == "0201" && descMap.attrId == "0029") {
        	def trimvalue = descMap.value[-4..-1]
            def stateValue = getThermostatOperatingState()[trimvalue]
            sendEvent("name": "thermostatOperatingState", "value": stateValue, "displayed": true)
            log.debug "THERMOSTAT OPERATING STATE is : $stateValue"
        // THERMOSTAT HOLD MODE
		} else if (descMap.cluster == "0201" && descMap.attrId == "0023") {
        	def trimvalue = descMap.value[-2..-1]
            def modeValue = getHoldModeMap()[trimvalue]
            sendEvent("name": "thermostatHoldMode", "value": modeValue, "displayed": true)
            log.debug "THERMOSTAT HOLD MODE is : $modeValue"
        // POWER SOURCE
		} else if (descMap.cluster == "0001" && descMap.attrId == "003e") {
        	def trimvalue = descMap.value[-2..-1]
            def sourceValue = getPowerSourceMap()[trimvalue]
            sendEvent("name": "powerSource", "value": sourceValue, "displayed": true)
            log.debug "POWER SOURCE is : $sourceValue"
		} else {
        	log.debug "UNKNOWN Cluster and Attribute : $descMap"
        }
	} else {
    	log.debug "UNKNOWN data from device : $description"
    }
}

def parseDescriptionAsMap(description) {
	(description - "read attr - ").split(",").inject([:]) { map, param -> 
		def nameAndValue = param.split(":")
		map += [(nameAndValue[0].trim()):nameAndValue[1].trim()]
	}
}

def getOpsReport() {["st rattr 0x${device.deviceNetworkId} 1 0x201 0x29"]}

def getModeMap() {["00":"off","03":"cool","04":"heat","05":"emergencyHeat"]}

def getHoldModeMap() {["00":"holdOff","01":"holdOn"]}

def getPowerSourceMap() {["00000000":"24VAC","00000001":"Battery"]}

def getFanModeMap() {["04":"fanOn","05":"fanAuto"]}

def getThermostatRunModeMap() {["00":"Off","03":"Cool","04":"Heat"]}

def getThermostatOperatingState() {["0000":"Unit is Idle","0001":"Unit is Heating","0004":"Unit is running the Fan","0005":"Unit is Heating","0006":"Unit is Cooling"]}

def setLevelUp(){
	log.debug "Adjusting temperature UP one degree"
    if (device.currentValue("thermostatMode") == "heat") {
    	int nextLevel = device.currentValue("heatingSetpoint") + 1
    	setHeatingSetpoint(nextLevel)
	} else if (device.currentValue("thermostatMode") == "emergencyHeat") {   
        int nextLevel = device.currentValue("heatingSetpoint") + 1
        setHeatingSetpoint(nextLevel)
	} else if (device.currentValue("thermostatMode") == "cool") {   
        int nextLevel = device.currentValue("coolingSetpoint") + 1
        setCoolingSetpoint(nextLevel)
	} else {
    	log.debug "Can't adjust set point when unit isn't in heat, e-heat, or cool mode."
    }
}

def setLevelDown(){
	log.debug "Adjusting temperature DOWN one degree"
    if (device.currentValue("thermostatMode") == "heat") {
    	int nextLevel = device.currentValue("heatingSetpoint") - 1
    	setHeatingSetpoint(nextLevel)
	} else if (device.currentValue("thermostatMode") == "emergencyHeat") {   
        int nextLevel = device.currentValue("heatingSetpoint") - 1
        setHeatingSetpoint(nextLevel)
	} else if (device.currentValue("thermostatMode") == "cool") {   
        int nextLevel = device.currentValue("coolingSetpoint") - 1
        setCoolingSetpoint(nextLevel)
	} else {
    	log.debug "Can't adjust set point when unit isn't in heat, e-heat, or cool mode."
    }
}

def setHeatingSetpoint(degrees) {
	if (device.currentValue("thermostatMode") == "heat" || "emergencyHeat") {
        if (degrees != null) {
        	log.debug "Setting HEAT set point to $degrees"
            def temperatureScale = getTemperatureScale()
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
	if (device.currentValue("thermostatMode") == "cool") {
        if (degrees != null) {
        	log.debug "Setting COOL set point to $degrees"
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
	sendEvent("name":"thermostatMode", "value":"off")
    [
		"st wattr 0x${device.deviceNetworkId} 1 0x201 0x1C 0x30 {00}", "delay 10000",
       	"st rattr 0x${device.deviceNetworkId} 1 0x201 0x29"
	]
}

def cool() {
	log.debug "Setting mode to COOL"
	sendEvent("name":"thermostatMode", "value":"cool")
    sendEvent("name": "thermostatSetpoint", "value": device.currentState("coolingSetpoint")?.value)
    [
		"st wattr 0x${device.deviceNetworkId} 1 0x201 0x1C 0x30 {03}", "delay 10000",
       	"st rattr 0x${device.deviceNetworkId} 1 0x201 0x29"
	]
}

def heat() {
	log.debug "Setting mode to HEAT"
	sendEvent("name":"thermostatMode", "value":"heat")
    sendEvent("name": "thermostatSetpoint", "value": device.currentState("heatingSetpoint")?.value)
    [
		"st wattr 0x${device.deviceNetworkId} 1 0x201 0x1C 0x30 {04}", "delay 10000",
       	"st rattr 0x${device.deviceNetworkId} 1 0x201 0x29",
	]
}

def emergencyHeat() {
	log.debug "Setting mode to EMERGENCY HEAT"
	sendEvent("name":"thermostatMode", "value":"emergencyHeat")
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

def configure() {
	log.debug "Configuration starting..."
	// Device-Watch allows 2 check-in misses from device + ping (plus 1 min lag time)
	// enrolls with default periodic reporting until newer 5 min interval is confirmed
    // zigbee.configureReporting(0x0201, 0x0029, DataType.BITMAP16, 60, 0, null)
    // "zcl global send-me-a-report 0x201 0x29 0x19 0 0", "delay 3000",
	sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 1 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
	[
		"zdo bind 0x${device.deviceNetworkId} 1 1 0x000 {${device.zigbeeId}} {}", "delay 1000",
        "zdo bind 0x${device.deviceNetworkId} 1 1 0x001 {${device.zigbeeId}} {}", "delay 1000",
		"zdo bind 0x${device.deviceNetworkId} 1 1 0x201 {${device.zigbeeId}} {}", "delay 1000",
		"zdo bind 0x${device.deviceNetworkId} 1 1 0x202 {${device.zigbeeId}} {}", "delay 1000",
		"zcl global send-me-a-report 1 0x20 0x20 3600 86400 {01}", "delay 1000", 				//battery report request
		"send 0x${device.deviceNetworkId} 1 1"
	]
    zigbee.configureReporting(0x0201, 0x0029, 0x19, 60, 0, null)
}

// PING is used by Device-Watch in attempt to reach the Device
def ping() {
	refresh()
}

def refresh()
{
	log.debug "Refreshing values..."
	[
		"st rattr 0x${device.deviceNetworkId} 1 0x000 0x07", "delay 200",
		"st rattr 0x${device.deviceNetworkId} 1 0x201 0", "delay 200",
		"st rattr 0x${device.deviceNetworkId} 1 0x201 0x11", "delay 200",
  		"st rattr 0x${device.deviceNetworkId} 1 0x201 0x12", "delay 200",
		"st rattr 0x${device.deviceNetworkId} 1 0x201 0x1C", "delay 200",
		"st rattr 0x${device.deviceNetworkId} 1 0x201 0x1E", "delay 200",
		"st rattr 0x${device.deviceNetworkId} 1 0x201 0x23", "delay 200",
        "st rattr 0x${device.deviceNetworkId} 1 0x201 0x29", "delay 200",
		"st rattr 0x${device.deviceNetworkId} 1 0x001 0x20", "delay 200",
        "st rattr 0x${device.deviceNetworkId} 1 0x001 0x3e", "delay 200",
		"st rattr 0x${device.deviceNetworkId} 1 0x202 0"
	]
    zigbee.configureReporting(0x0201, 0x0029, 0x19, 60, 0, null)
}

private hex(value) {
	new BigInteger(Math.round(value).toString()).toString(16)
}