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
 *  04-09-2019 : Cleaned up code, added additional reporting configs for mode and fan.
 *  04-19-2019 : Added command "setTemperature" that gets executed by a SmartApp via a virtual dimmer switch. This is a workaround because Alexa isn't playing nice with this DTH for some reason. Cleaned up code too.
 *  05-13-2019 : Added 6 "quick change" temperature tiles.
 *  05-15-2019 : Added code for Power Source reporting.  Still a work in progress.
 *  06-16-2019 : Overhauled to support new app.  Based off of ST's Zigbee Thermostat DTH.
 *
 */

import groovy.json.JsonOutput
import physicalgraph.zigbee.zcl.DataType
 
metadata {
	definition (name: "My Centralite Thermostat", namespace: "jsconstantelos", author: "SmartThings", mnmn: "SmartThings", vid: "generic-thermostat-1", genericHandler: "Zigbee") {
		capability "Actuator"
        capability "Switch"
		capability "Temperature Measurement"
		capability "Thermostat"
		capability "Thermostat Mode"
		capability "Thermostat Fan Mode"
		capability "Thermostat Cooling Setpoint"
		capability "Thermostat Heating Setpoint"
		capability "Thermostat Operating State"
		capability "Configuration"
		capability "Battery"
		capability "Power Source"
		capability "Health Check"
		capability "Refresh"
		capability "Sensor"

        command "holdOn"
        command "holdOff"

        command "getPowerSource"

		fingerprint profileId: "0104", inClusters: "0000,0001,0003,0004,0005,0020,0201,0202,0204,0B05", outClusters: "000A, 0019",  manufacturer: "LUX", model: "KONOZ", deviceJoinName: "LUX KONOz Thermostat"
	}

	tiles {
		multiAttributeTile(name:"thermostatMulti", type:"thermostat", width:3, height:2, canChangeIcon: true) {
			tileAttribute("device.temperature", key: "PRIMARY_CONTROL") {
				attributeState("temperature", label:'${currentValue}°', icon: "st.alarm.temperature.normal",
					backgroundColors: [
						// Celsius
						[value: 0, color: "#153591"],
						[value: 7, color: "#1e9cbb"],
						[value: 15, color: "#90d2a7"],
						[value: 23, color: "#44b621"],
						[value: 28, color: "#f1d801"],
						[value: 35, color: "#d04e00"],
						[value: 37, color: "#bc2323"],
						// Fahrenheit
						[value: 40, color: "#153591"],
						[value: 44, color: "#1e9cbb"],
						[value: 59, color: "#90d2a7"],
						[value: 74, color: "#44b621"],
						[value: 84, color: "#f1d801"],
						[value: 95, color: "#d04e00"],
						[value: 96, color: "#bc2323"]
					]
				)
			}
			tileAttribute("device.thermostatOperatingState", key: "OPERATING_STATE") {
				attributeState("idle", backgroundColor: "#cccccc")
				attributeState("heating", backgroundColor: "#E86D13")
				attributeState("cooling", backgroundColor: "#00A0DC")
			}
			tileAttribute("device.thermostatMode", key: "THERMOSTAT_MODE") {
				attributeState("off", action: "setThermostatMode", label: "Off", icon: "st.thermostat.heating-cooling-off")
				attributeState("cool", action: "setThermostatMode", label: "Cool", icon: "st.thermostat.cool")
				attributeState("heat", action: "setThermostatMode", label: "Heat", icon: "st.thermostat.heat")
				attributeState("emergency heat", action:"setThermostatMode", label: "Emergency heat", icon: "st.thermostat.emergency-heat")
			}
			tileAttribute("device.heatingSetpoint", key: "HEATING_SETPOINT") {
				attributeState("default", label: '${currentValue}', unit: "°", defaultState: true)
			}
			tileAttribute("device.coolingSetpoint", key: "COOLING_SETPOINT") {
				attributeState("default", label: '${currentValue}', unit: "°", defaultState: true)
			}
		}
		controlTile("thermostatMode", "device.thermostatMode", "enum", width: 2 , height: 2, supportedStates: "device.supportedThermostatModes") {
			state("off", action: "setThermostatMode", label: 'Off', icon: "st.thermostat.heating-cooling-off")
			state("cool", action: "setThermostatMode", label: 'Cool', icon: "st.thermostat.cool")
			state("heat", action: "setThermostatMode", label: 'Heat', icon: "st.thermostat.heat")
			state("emergency heat", action:"setThermostatMode", label: 'Emergency heat', icon: "st.thermostat.emergency-heat")
		}
		controlTile("heatingSetpoint", "device.heatingSetpoint", "slider",
				sliderType: "HEATING",
				debouncePeriod: 1500,
				range: "device.heatingSetpointRange",
				width: 2, height: 2) {
					state "default", action:"setHeatingSetpoint", label:'${currentValue}', backgroundColor: "#E86D13"
				}
		controlTile("coolingSetpoint", "device.coolingSetpoint", "slider",
				sliderType: "COOLING",
				debouncePeriod: 1500,
				range: "device.coolingSetpointRange",
				width: 2, height: 2) {
					state "default", action:"setCoolingSetpoint", label:'${currentValue}', backgroundColor: "#00A0DC"
				}
		controlTile("thermostatFanMode", "device.thermostatFanMode", "enum", width: 2 , height: 2, supportedStates: "device.supportedThermostatFanModes") {
			state "auto", action: "setThermostatFanMode", label: 'Auto', icon: "st.thermostat.fan-auto"
			state "on",	action: "setThermostatFanMode", label: 'On', icon: "st.thermostat.fan-on"
		}
		standardTile("refresh", "device.thermostatMode", width: 2, height: 1, inactiveLabel: false, decoration: "flat") {
			state "default", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		valueTile("powerSource", "device.powerSource", width: 2, heigh: 1, inactiveLabel: true, decoration: "flat") {
			state "powerSource", label: 'Power Source: ${currentValue}', action:"configuration.configure", backgroundColor: "#ffffff"
		}
		valueTile("battery", "device.battery", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "battery", label:'${currentValue}% battery', unit:""
		}
		main "thermostatMulti"
		details(["thermostatMulti", "thermostatMode", "heatingSetpoint", "coolingSetpoint", "thermostatFanMode", "battery", "powerSource", "refresh"])
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
                    sendEvent("name": "temperature", "value": fahrenheit, "unit": temperatureScale, "displayed": true)
                    log.debug "TEMPERATURE is : ${fahrenheit}${temperatureScale}"
                } else {
                    sendEvent("name": "temperature", "value": celsius, "unit": temperatureScale, "displayed": true)
                    log.debug "TEMPERATURE is : ${celsius}${temperatureScale}"
                }
            }
        // COOLING SETPOINT
		} else if (descMap.cluster == "0201" && descMap.attrId == "0011") {
        	if (descMap.value != null) {
            	def trimvalue = descMap.value[-4..-1]
                def celsius = Integer.parseInt(trimvalue, 16) / 100
                def fahrenheit = String.format("%3.1f",celsiusToFahrenheit(celsius))
                if (tempScale == "F") {
                    sendEvent("name": "coolingSetpoint", "value": fahrenheit, "unit": temperatureScale, "displayed": true)
                    log.debug "COOLING SETPOINT is : ${fahrenheit}${temperatureScale}"
                } else {
                    sendEvent("name": "coolingSetpoint", "value": celsius, "unit": temperatureScale, "displayed": true)
                    log.debug "COOLING SETPOINT is : ${celsius}${temperatureScale}"
                }
            }
        // HEATING SETPOINT
		} else if (descMap.cluster == "0201" && descMap.attrId == "0012") {
        	if (descMap.value != null) {
            	def trimvalue = descMap.value[-4..-1]
                def celsius = Integer.parseInt(trimvalue, 16) / 100
                def fahrenheit = String.format("%3.1f",celsiusToFahrenheit(celsius))
                if (tempScale == "F") {
                    sendEvent("name": "heatingSetpoint", "value": fahrenheit, "unit": temperatureScale, "displayed": true)
                    log.debug "HEATING SETPOINT is : ${fahrenheit}${temperatureScale}"
                } else {
                    sendEvent("name": "heatingSetpoint", "value": celsius, "unit": temperatureScale, "displayed": true)
                    log.debug "HEATING SETPOINT is : ${celsius}${temperatureScale}"
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
        // POWER SOURCE
		} else if (descMap.cluster == "0000" && descMap.attrId == "0007") {
        	getPowerSourceMap(descMap.value)
		} else {
//        	log.debug "UNKNOWN Cluster and Attribute : $descMap"
        }
	} else {
    	log.debug "UNKNOWN data from device : $description"
    }
}

def getPowerSource() { //This is only used for figuring out how/where to get power source data from to determine if we're on AC or batteries.  Work in progress.
	log.debug "Refresh power source info..."
	[
    "st rattr 0x${device.deviceNetworkId} 1 0x000 0x07"
    ]
}

def getCoolingSetpointRange() {
	(getTemperatureScale() == "C") ? [10, 35] : [50, 95]
}
def getHeatingSetpointRange() {
	(getTemperatureScale() == "C") ? [7, 32] : [45, 90]
}

def getModeMap() {
	[
    "00":"off",
    "01":"auto",
    "03":"cool",
    "04":"heat",
    "05":"emergency heat",
    "06":"precooling",
    "07":"fan only"
    ]
}

def getHoldModeMap() {
	[
    "00":"holdOff",
    "01":"holdOn"
    ]
}

def getPowerSourceMap(value) {
    if (value == "81") {
        sendEvent(name: "powerSource", value: "mains", "displayed": true)
        log.debug "POWER SOURCE is mains"
    } else {
      	sendEvent(name: "powerSource", value: "battery", "displayed": true)
        log.debug "POWER SOURCE is batteries"
    }
}

def getFanModeMap() {
	[
    "00":"off",
    "04":"on",
    "05":"auto"
    ]
}

def getThermostatOperatingState() {
	[
    "0000":"Idle",
    "0001":"Heating",
    "0002":"Cooling",
    "0004":"Fan is Running",
    "0005":"Heating",
    "0006":"Cooling",
    "0008":"Heating",
    "0009":"Heating",
    "000A":"Heating",
    "000D":"Heating",
    "0010":"Cooling",
    "0012":"Cooling",
    "0014":"Cooling",
    "0015":"Cooling"
    ]
}

//********************************
// Send commands to the thermostat
//********************************

//Gets executed by a SmartApp via a virtual dimmer switch.  For example, "Alexa, set Downstairs Temperature to 72"
def setTemperature(value) {
    log.debug "Setting Temperature by a SmartApp to ${value}"
    def int desiredTemp = value.toInteger()
    if (device.currentValue("thermostatMode") == "heat") {
    	setHeatingSetpoint(desiredTemp)
	} else if (device.currentValue("thermostatMode") == "emergency heat") {   
        setHeatingSetpoint(desiredTemp)
	} else if (device.currentValue("thermostatMode") == "cool") {   
        setCoolingSetpoint(desiredTemp)
	} else {
    	log.debug "Can't adjust set point when unit isn't in heat, e-heat, or cool mode."
    }
}

def setHeatingSetpoint(degrees) {
	log.debug "Setting HEAT set point to ${degrees}"
    if (degrees != null) {
        def degreesInteger = Math.round(degrees)
        sendEvent("name": "heatingSetpoint", "value": degreesInteger)
        def celsius = (getTemperatureScale() == "C") ? degreesInteger : (fahrenheitToCelsius(degreesInteger) as Double).round(2)
        ["st wattr 0x${device.deviceNetworkId} 1 0x201 0x12 0x29 {" + hex(celsius * 100) + "}"]
    }
}

def setCoolingSetpoint(degrees) {
	log.debug "Setting COOL set point to ${degrees}"
    if (degrees != null) {
        def degreesInteger = Math.round(degrees)
        sendEvent("name": "coolingSetpoint", "value": degreesInteger)
        def celsius = (getTemperatureScale() == "C") ? degreesInteger : (fahrenheitToCelsius(degreesInteger) as Double).round(2)
        ["st wattr 0x${device.deviceNetworkId} 1 0x201 0x11 0x29 {" + hex(celsius * 100) + "}"]
    }
}

def setThermostatFanMode(mode) {
	if (state.supportedFanModes?.contains(mode)) {
		switch (mode) {
			case "on":
				fanOn()
				break
			case "auto":
				fanAuto()
				break
		}
	} else {
		log.debug "Unsupported fan mode $mode"
	}
}

def setThermostatMode(mode) {
	log.debug "set mode $mode (supported ${state.supportedThermostatModes})"
	if (state.supportedThermostatModes?.contains(mode)) {
		switch (mode) {
			case "heat":
				heat()
				break
			case "cool":
				cool()
				break
			case "auto":
				auto()
				break
			case "emergency heat":
				emergencyHeat()
				break
			case "off":
				offmode()
				break
		}
	} else {
		log.debug "Unsupported mode $mode"
	}
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
		"st wattr 0x${device.deviceNetworkId} 1 0x201 0x1C 0x30 {00}"
	]
}

def cool() {
	log.debug "Setting mode to COOL"
	sendEvent("name":"thermostatMode", "value":"cool")
    [
		"st wattr 0x${device.deviceNetworkId} 1 0x201 0x1C 0x30 {03}"
	]
}

def heat() {
	log.debug "Setting mode to HEAT"
	sendEvent("name":"thermostatMode", "value":"heat")
    [
		"st wattr 0x${device.deviceNetworkId} 1 0x201 0x1C 0x30 {04}"
	]
}

def emergencyHeat() {
	log.debug "Setting mode to EMERGENCY HEAT"
	sendEvent("name":"thermostatMode", "value":"emergency heat")
    [
		"st wattr 0x${device.deviceNetworkId} 1 0x201 0x1C 0x30 {05}"
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
	sendEvent("name":"thermostatFanMode", "value":"on")
    [
		"st wattr 0x${device.deviceNetworkId} 1 0x202 0 0x30 {04}"
	]
}

def fanAuto() {
	log.debug "Setting fan to AUTO"
	sendEvent("name":"thermostatFanMode", "value":"auto")
    [
		"st wattr 0x${device.deviceNetworkId} 1 0x202 0 0x30 {05}"
	]
}

def holdOn() {
	log.debug "Setting hold to ON"
	sendEvent("name":"thermostatHoldMode", "value":"holdOn")
    [
		"st wattr 0x${device.deviceNetworkId} 1 0x201 0x23 0x30 {01}"
	]
}

def holdOff() {
	log.debug "Setting hold to OFF"
	sendEvent("name":"thermostatHoldMode", "value":"holdOff")
    [
		"st wattr 0x${device.deviceNetworkId} 1 0x201 0x23 0x30 {00}"
	]
}

// Commment out below if no C-wire since it will kill the batteries.
def poll() {
//	refresh()
	log.debug "Poll..."
	"st rattr 0x${device.deviceNetworkId} 1 0x201 0x29"
}

// PING is used by Device-Watch in attempt to reach the Device
def ping() {
//	refresh()
	log.debug "Ping..."
    "st rattr 0x${device.deviceNetworkId} 1 0x201 0x29"
}

def configure() {
	log.debug "Configuration starting..."
	sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 1 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
	sendEvent(name: "coolingSetpointRange", value: coolingSetpointRange, displayed: false)
	sendEvent(name: "heatingSetpointRange", value: heatingSetpointRange, displayed: false)
	state.supportedThermostatModes = ["off", "heat", "cool", "emergency heat"]
	state.supportedFanModes = ["on", "auto"]
	sendEvent(name: "supportedThermostatModes", value: JsonOutput.toJson(state.supportedThermostatModes), displayed: false)
	sendEvent(name: "supportedThermostatFanModes", value: JsonOutput.toJson(state.supportedFanModes), displayed: false)
	[
		"zdo bind 0x${device.deviceNetworkId} 1 1 0x000 {${device.zigbeeId}} {}", "delay 1000",
        "zdo bind 0x${device.deviceNetworkId} 1 1 0x001 {${device.zigbeeId}} {}", "delay 1000",
		"zdo bind 0x${device.deviceNetworkId} 1 1 0x201 {${device.zigbeeId}} {}", "delay 1000",
		"zdo bind 0x${device.deviceNetworkId} 1 1 0x202 {${device.zigbeeId}} {}", "delay 1000",
		"zcl global send-me-a-report 1 0x20 0x20 3600 86400 {01}", "delay 1000", // Battery report
		"send 0x${device.deviceNetworkId} 1 1"
	]
    [
    	zigbee.configureReporting(0x0201, 0x0029, 0x19, 0, 0, null), "delay 1000",	// Thermostat Operating State report to send whenever it changes (no min or max, or change threshold).  This is also known as Running State (Zen).
        zigbee.configureReporting(0x0201, 0x001c, 0x30, 0, 0, null), "delay 1000",
        zigbee.configureReporting(0x0000, 0x0007, 0x30, 0, 0, null)
	]
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
    [
    	zigbee.configureReporting(0x0201, 0x0029, 0x19, 0, 0, null), "delay 1000",
        zigbee.configureReporting(0x0201, 0x001c, 0x30, 0, 0, null), "delay 1000",
        zigbee.configureReporting(0x0000, 0x0007, 0x30, 0, 0, null)
	]
}

private hex(value) {
	new BigInteger(Math.round(value).toString()).toString(16)
}