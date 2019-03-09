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
 *  03-06-2019 : Initial commit
 *  03-08-2019 : Tweaks for the new ST app, and more thermostat capabilities (operating state).
 *
 */
 
import physicalgraph.zigbee.zcl.DataType
 
metadata {
	definition (name: "My Zigbee Thermostat", namespace: "jsconstantelos", author: "SmartThings", mnmn: "SmartThings", ocfDeviceType: "oic.d.thermostat", vid: "generic-thermostat-1") {
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
		command "setLevelUp"
		command "setLevelDown"

		attribute "temperatureScale", "string"
		attribute "thermostatHoldMode", "string"
		attribute "powerSource", "string"
        attribute "thermostatSetpoint", "number"
        attribute "thermostatRunMode", "number"
        attribute "thermostatOperatingState", "number"
        attribute "currentMode", "string"
		
        fingerprint profileId: "0104", inClusters: "0000,0001,0003,0004,0005,0020,0201,0202,0204,0B05", outClusters: "000A, 0019"
	}

	tiles(scale: 2) {
        multiAttributeTile(name:"summary", type: "", width: 6, height: 4) {
        	tileAttribute("device.temperature", key: "PRIMARY_CONTROL") {
				attributeState("temperature", action:"refresh.refresh", label:'${currentValue}°', unit:"F",
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
        standardTile("fanauto", "device.thermostatFanMode", width: 3, height: 1, inactiveLabel: false, decoration: "flat") {
            state "fanAuto", label:'', action:"fanAuto", icon:"st.thermostat.fan-auto"
        }
        standardTile("fanon", "device.thermostatFanMode", width: 3, height: 1, inactiveLabel: false, decoration: "flat") {
            state "fanOn", label:'', action:"fanOn", icon:"st.thermostat.fan-on"
        }

//Temperature Set Point Controls
		controlTile("heatSliderControl", "device.heatingSetpoint", "slider", height: 1, width: 3, inactiveLabel: false, range:"(60..80)") {
			state "setHeatingSetpoint", action:"thermostat.setHeatingSetpoint", backgroundColor:"#d04e00", unit:"F"
		}
		controlTile("coolSliderControl", "device.coolingSetpoint", "slider", height: 1, width: 3, inactiveLabel: false, range:"(60..80)") {
			state "setCoolingSetpoint", action:"thermostat.setCoolingSetpoint", backgroundColor: "#003CEC", unit:"F"
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

// parse events into attributes
def parse(String description) {
//	log.debug "Parse description : $description"
	def map = [:]
	if (description?.startsWith("read attr -")) {
		def descMap = parseDescriptionAsMap(description)
//		log.debug "Desc Map : $descMap"
		if (descMap.cluster == "0201" && descMap.attrId == "0000") {			// OK
        	"st rattr 0x${device.deviceNetworkId} 1 0x201 0x29"
			map.name = "temperature"
			map.value = getTemperature(descMap.value)
		} else if (descMap.cluster == "0201" && descMap.attrId == "0011") {		// OK
        	"st rattr 0x${device.deviceNetworkId} 1 0x201 0x29"
			map.name = "coolingSetpoint"
			map.value = getCoolSetTemp(descMap.value)
            if (device.currentValue("thermostatMode") == "cool") {
	            sendEvent("name": "thermostatSetpoint", "value": map.value)
                if (device.currentState('coolingSetpoint')?.value > device.currentState('temperature')?.value) {
                	sendEvent(name: "thermostatOperatingState", value: "Unit is Idle (not running)", displayed: true)
                }
                if (device.currentState('coolingSetpoint')?.value <= device.currentState('temperature')?.value) {
                	sendEvent(name: "thermostatOperatingState", value: "Unit is currently Cooling" as String, displayed: false)
                }
			}
		} else if (descMap.cluster == "0201" && descMap.attrId == "0012") {		// OK
        	"st rattr 0x${device.deviceNetworkId} 1 0x201 0x29"
			map.name = "heatingSetpoint"
			map.value = getHeatSetTemp(descMap.value)
            if (device.currentValue("thermostatMode") == "heat" || "emergencyHeat") {
	            sendEvent("name": "thermostatSetpoint", "value": map.value)
                if (device.currentState('heatingSetpoint')?.value > device.currentState('temperature')?.value) {
                	sendEvent(name: "thermostatOperatingState", value: "Unit is Idle (not running)", displayed: true)
                }
                if (device.currentState('heatingSetpoint')?.value > device.currentState('temperature')?.value) {
                	sendEvent(name: "thermostatOperatingState", value: "Unit is currently Heating", displayed: false)
                }
			}
		} else if (descMap.cluster == "0201" && descMap.attrId == "001c") {		// OK
        	"st rattr 0x${device.deviceNetworkId} 1 0x201 0x29"
			map.name = "thermostatMode"
			map.value = getModeMap()[descMap.value]
		} else if (descMap.cluster == "0202" && descMap.attrId == "0000") {		// OK
        	"st rattr 0x${device.deviceNetworkId} 1 0x201 0x29"
			map.name = "thermostatFanMode"
			map.value = getFanModeMap()[descMap.value]
		} else if (descMap.cluster == "0001" && descMap.attrId == "0020") {		// OK
			map.name = "battery"
			map.value = getBatteryLevel(descMap.value)
		} else if (descMap.cluster == "0201" && descMap.attrId == "001e") {		// OK
			map.name = "thermostatRunMode"
			map.value = getThermostatRunMode(descMap.value)
            map.displayed = "false"
		} else if (descMap.cluster == "0201" && descMap.attrId == "0029") {		// OK
        	"st rattr 0x${device.deviceNetworkId} 1 0x201 0x29"
			map.name = "thermostatOperatingState"
			map.value = getThermostatOperatingState()[descMap.value]
		} else if (descMap.cluster == "0201" && descMap.attrId == "0023") {		// OK
			map.name = "thermostatHoldMode"
			map.value = getHoldModeMap()[descMap.value]			
		} else if (descMap.cluster == "0001" && descMap.attrId == "003e") {		// OK
			map.name = "powerSource"
			map.value = getPowerSource()[descMap.value]			
		}
	}
	def result = null
	if (map) {
		result = createEvent(map)
	}
	log.debug "Parse returned : $map"
	return result
}

def parseDescriptionAsMap(description) {
	(description - "read attr - ").split(",").inject([:]) { map, param -> 
		def nameAndValue = param.split(":")
		map += [(nameAndValue[0].trim()):nameAndValue[1].trim()]
	}
}

def getModeMap() {
    [
	"00":"off",
	"03":"cool",
	"04":"heat",
	"05":"emergencyHeat"]
}

def getHoldModeMap() {
    [
	"00":"holdOff",
	"01":"holdOn"
    ]
}

def getPowerSource() {
	[
	"00000000":"24VAC",
	"00000001":"Battery"
	]
}

def getFanModeMap() {
    [
	"04":"fanOn",
	"05":"fanAuto"
    ]
}

def getThermostatRunMode(value) {
	"st rattr 0x${device.deviceNetworkId} 1 0x201 0x29"
	if (value != null) {
	    def RunModeValue = Integer.parseInt(value, 16)
        sendEvent("name":"thermostatRunMode", "value":RunModeValue, displayed: false)
	}
}

def getThermostatOperatingState(value) {
    [
	"0000":"Unit is Idle (not running)",
	"0001":"Unit is currently Heating",
	"0004":"Unit is running the Fan",
    "0005":"Unit is currently Heating",
	"0006":"Unit is currently Cooling"
	]
}

def getTemperature(value) {
	if (value != null) {
		def celsius = Integer.parseInt(value, 16) / 100
		if (getTemperatureScale() == "C") {
			return celsius
		} else {
//			return Math.round(celsiusToFahrenheit(celsius))
            return String.format("%3.1f",celsiusToFahrenheit(celsius))
		}
	}
}

def getCoolSetTemp(value) {
	if (value != null) {
		def celsius = Integer.parseInt(value, 16) / 100
		if (getTemperatureScale() == "C") {
			return celsius
		} else {
			return Math.round(celsiusToFahrenheit(celsius))
		}
	}
}

def getHeatSetTemp(value) {
	if (value != null) {
		def celsius = Integer.parseInt(value, 16) / 100
		if (getTemperatureScale() == "C") {
			return celsius
		} else {
			return Math.round(celsiusToFahrenheit(celsius))
		}
	}
}

def setLevelUp(){
    if (device.currentValue("thermostatMode") == "heat") {
    	int nextLevel = device.currentValue("heatingSetpoint") + 1
    	setHeatingSetpoint(nextLevel)
	} else if (device.currentValue("thermostatMode") == "emergencyHeat") {   
        int nextLevel = device.currentValue("heatingSetpoint") + 1
        setHeatingSetpoint(nextLevel)
	} else if (device.currentValue("thermostatMode") == "cool") {   
        int nextLevel = device.currentValue("coolingSetpoint") + 1
        setCoolingSetpoint(nextLevel)
	}  
}

def setLevelDown(){
    if (device.currentValue("thermostatMode") == "heat") {
    	int nextLevel = device.currentValue("heatingSetpoint") - 1
    	setHeatingSetpoint(nextLevel)
	} else if (device.currentValue("thermostatMode") == "emergencyHeat") {   
        int nextLevel = device.currentValue("heatingSetpoint") - 1
        setHeatingSetpoint(nextLevel)
	} else if (device.currentValue("thermostatMode") == "cool") {   
        int nextLevel = device.currentValue("coolingSetpoint") - 1
        setCoolingSetpoint(nextLevel)
	}   
}

def setHeatingSetpoint(degrees) {
	if (device.currentValue("thermostatMode") == "heat" || "emergencyHeat") {
        if (degrees != null) {
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
            def degreesInteger = Math.round(degrees)
            sendEvent("name": "coolingSetpoint", "value": degreesInteger)
            sendEvent("name": "thermostatSetpoint", "value": degreesInteger)
            def celsius = (getTemperatureScale() == "C") ? degreesInteger : (fahrenheitToCelsius(degreesInteger) as Double).round(2)
            ["st wattr 0x${device.deviceNetworkId} 1 0x201 0x11 0x29 {" + hex(celsius * 100) + "}", "delay 10000",
	       	"st rattr 0x${device.deviceNetworkId} 1 0x201 0x29"]
        }
    }
}

def modes() {
	["off", "cool", "heat", "emergencyHeat"]
}

def setThermostatMode() {
	def currentMode = device.currentState("thermostatMode")?.value
	def modeOrder = modes()
	def index = modeOrder.indexOf(currentMode)
    def next = index >= 0 && index < modeOrder.size() - 1 ? modeOrder[index + 1] : modeOrder[0]
	"$next"()
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

def setThermostatMode(String value) {
	"$value"()
}

def setThermostatFanMode(String value) {
	"$value"()
}

def setThermostatHoldMode(String value) {
	"$value"()
}

def offmode() {
	sendEvent("name":"thermostatMode", "value":"off")
    [
		"st wattr 0x${device.deviceNetworkId} 1 0x201 0x1C 0x30 {00}", "delay 10000",
       	"st rattr 0x${device.deviceNetworkId} 1 0x201 0x29"
	]
}

def cool() {
	sendEvent("name":"thermostatMode", "value":"cool")
    sendEvent("name": "thermostatSetpoint", "value": device.currentState("coolingSetpoint")?.value)
    [
		"st wattr 0x${device.deviceNetworkId} 1 0x201 0x1C 0x30 {03}", "delay 10000",
       	"st rattr 0x${device.deviceNetworkId} 1 0x201 0x29"
	]
}

def heat() {
	sendEvent("name":"thermostatMode", "value":"heat")
    sendEvent("name": "thermostatSetpoint", "value": device.currentState("heatingSetpoint")?.value)
    [
		"st wattr 0x${device.deviceNetworkId} 1 0x201 0x1C 0x30 {04}", "delay 10000",
       	"st rattr 0x${device.deviceNetworkId} 1 0x201 0x29",
	]
}

def emergencyHeat() {
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
	sendEvent("name":"thermostatFanMode", "value":"fanOn")
    [
		"st wattr 0x${device.deviceNetworkId} 1 0x202 0 0x30 {04}", "delay 10000",
       	"st rattr 0x${device.deviceNetworkId} 1 0x201 0x29"
	]
}

def auto() {
	fanAuto()
}

def fanAuto() {
	sendEvent("name":"thermostatFanMode", "value":"fanAuto")
    [
		"st wattr 0x${device.deviceNetworkId} 1 0x202 0 0x30 {05}", "delay 10000",
       	"st rattr 0x${device.deviceNetworkId} 1 0x201 0x29"
	]
}

def holdOn() {
	sendEvent("name":"thermostatHoldMode", "value":"holdOn")
    [
		"st wattr 0x${device.deviceNetworkId} 1 0x201 0x23 0x30 {01}", "delay 5000",
       	"st rattr 0x${device.deviceNetworkId} 1 0x201 0x29"
	]
}

def holdOff() {
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
	log.debug "Configuration..."
	// Device-Watch allows 2 check-in misses from device + ping (plus 1 min lag time)
	// enrolls with default periodic reporting until newer 5 min interval is confirmed
	sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 1 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
	[
		"zdo bind 0x${device.deviceNetworkId} 1 1 0x000 {${device.zigbeeId}} {}", "delay 200",
		"zdo bind 0x${device.deviceNetworkId} 1 1 0x201 {${device.zigbeeId}} {}", "delay 200",
		"zdo bind 0x${device.deviceNetworkId} 1 1 0x202 {${device.zigbeeId}} {}", "delay 200",
		"zcl global send-me-a-report 1 0x20 0x20 3600 86400 {01}", "delay 100", //battery report request
		"send 0x${device.deviceNetworkId} 1 1", "delay 200"
	]
}

// PING is used by Device-Watch in attempt to reach the Device
def ping() {
	refresh()
}

def refresh()
{
	log.debug "refresh called"
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
}

private getBatteryLevel(rawValue) {
	def intValue = Integer.parseInt(rawValue,16)
	def min = 2.1
    def max = 3.0
    def vBatt = intValue / 10
    return ((vBatt - min) / (max - min) * 100) as int
}

private hex(value) {
	new BigInteger(Math.round(value).toString()).toString(16)
}