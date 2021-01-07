/**
 *  FortrezZ Flow Meter DTH for the new ST mobile app.
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
 *  12-22-2020 : Original commit.
 *  01-07-2021 : Added Power Source capability and relative code.
 *
 */
metadata {
	definition (name: "My FortrezZ Flow Meter", namespace: "jsconstantelos", author: "John Constantelos") {
        capability "Power Meter"        
		capability "Energy Meter"
		capability "Temperature Measurement"
        capability "Water Sensor"
        capability "Sensor"
		capability "Battery"
        capability "Power Source"
        capability "Configuration"
        capability "Actuator"        
        capability "Polling"
        capability "Refresh"
        capability "Health Check"
        
		attribute "gpm", "number" // support for FortrezZ's legacy smartapps
        attribute "cumulative", "number" // support for FortrezZ's legacy smartapps
        attribute "flowHighValue", "number"
        attribute "gallonsLastUsed", "number"
        attribute "gallonsHighValue", "number"
        attribute "alarmState", "string"

        command "resetMeter"
        command "reset"

	    fingerprint deviceId: "0x2101", inClusters: "0x5E, 0x86, 0x72, 0x5A, 0x73, 0x71, 0x85, 0x59, 0x32, 0x31, 0x70, 0x80, 0x7A"
	}
    
    preferences {
       input "debugOutput", "boolean", title: "Enable debug logging?", defaultValue: false, displayDuringSetup: true
       input "reportThreshhold", "decimal", title: "Reporting Rate Threshhold", description: "The time interval between meter reports while water is flowing. 6 = 60 seconds, 1 = 10 seconds. Options are 1, 2, 3, 4, 5, or 6.", defaultValue: 1, range: "1..6", required: false, displayDuringSetup: true
       input "gallonThreshhold", "decimal", title: "High Flow Rate Threshhold", description: "Flow rate (in gpm) that will trigger a notification.", defaultValue: 5, required: false, displayDuringSetup: true
    }

	tiles(scale: 2) {
		valueTile("power", "device.power") {
			state "default", label:'${currentValue}'
		}
		valueTile("energy", "device.energy") {
			state "default", label:'${currentValue}'
		}
		valueTile("temperature", "device.temperature") {
            state("temperature", label:'${currentValue}'
            )
        }
		valueTile("battery", "device.battery") {
			state "battery", label:'${currentValue}'
		}

		main (["power"])
		details(["power", "energy", "temperature", "battery", "configure"])
	}
}

def installed() {
    state.debug = ("true" == debugOutput)
    sendEvent(name: "gallonsLastUsed", value: 0, displayed: false)
    sendEvent(name: "energy", value: 0, displayed: false)
    sendEvent(name: "cumulative", value: 0, displayed: false)
    sendEvent(name: "flowHighValue", value: 0)
    sendEvent(name: "gallonsHighValue", value: 0)
}

def updated(){
	state.debug = ("true" == debugOutput)
	// Device-Watch simply pings if no device events received for 32min(checkInterval)
	sendEvent(name: "checkInterval", value: 2 * 30 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
    response(configure())
}

// parse events into attributes
def parse(String description) {
	def results = []
	if (description.startsWith("Err")) {
	    results << createEvent(descriptionText:description, displayed:true)
	} else {
		def cmd = zwave.parse(description, [ 0x80: 1, 0x84: 1, 0x71: 2, 0x72: 1 ])
		if (cmd) {
			results << createEvent( zwaveEvent(cmd) )
		}
	}
//    log.debug "Data parsed to : ${results.inspect()}"
	return results
}

def poll() {
    refresh()
}

def reset() {
    resetMeter()
}

def resetMeter() {
	log.debug "Resetting water meter..."
    sendEvent(name: "gallonsLastUsed", value: 0, displayed: false)
    sendEvent(name: "energy", value: 0, displayed: false)
    sendEvent(name: "cumulative", value: 0, displayed: false)
    sendEvent(name: "flowHighValue", value: 0)
    sendEvent(name: "gallonsHighValue", value: 0)
    sendEvent(name: "alarmState", value: "The meter was just reset", displayed: false)
    def cmds = delayBetween([
	    zwave.meterV3.meterReset().format()
    ])
    return cmds
}

def zwaveEvent(physicalgraph.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd) {
	if (state.debug) log.debug "Getting temperature data..."
	def map = [:]
	if(cmd.sensorType == 1) {
		map = [name: "temperature"]
        if(cmd.scale == 0) {
        	map.value = getTemperature(cmd.scaledSensorValue)
        } else {
	        map.value = cmd.scaledSensorValue
        }
        map.unit = location.temperatureScale
	}
	return map
}

def zwaveEvent(physicalgraph.zwave.commands.meterv3.MeterReport cmd) {
	if (state.debug) log.debug "scaledMeterValue is ${cmd.scaledMeterValue}"
    if (state.debug) log.debug "scaledPreviousMeterValue is ${cmd.scaledPreviousMeterValue}"
    def delta = Math.round((((cmd.scaledMeterValue - cmd.scaledPreviousMeterValue) / (reportThreshhold*10)) * 60)*100)/100 //rounds to 2 decimal positions
    if (delta < 0) { //There should never be any negative values
			if (state.debug) log.debug "We just detected a negative delta value that won't be processed: ${delta}"
            return
    } else if (delta > 60) { //There should never be any crazy high gallons as a delta, even at 1 minute reporting intervals.  It's not possible unless you're a firetruck.
    		if (state.debug) log.debug "We just detected a crazy high delta value that won't be processed: ${delta}"
            return
    } else if (delta == 0) {
    		if (state.debug) log.debug "Flow has stopped, so process what the meter collected."
            if (cmd.scaledMeterValue == device.currentState('energy')?.doubleValue) {
            	if (state.debug) log.debug "Current and previous gallon values were the same, so skip processing."
                return
            }
            if (cmd.scaledMeterValue < device.currentState('energy')?.doubleValue) {
            	if (state.debug) log.debug "Current gallon value is less than the previous gallon value and that should never happen, so skip processing."
                return
            }
			def prevCumulative = cmd.scaledMeterValue - device.currentState('energy')?.doubleValue
			if (prevCumulative > device.currentState('gallonsHighValue')?.doubleValue) {
                sendEvent(name: "gallonsHighValue", value: String.format("%3.1f",prevCumulative), displayed: false)
            }
            sendEvent(name: "power", value: delta, displayed: true)
            sendEvent(name: "energy", value: cmd.scaledMeterValue, displayed: true)
            sendEvent(name: "gpm", value: delta, displayed: true)
            sendEvent(name: "cumulative", value: cmd.scaledMeterValue, displayed: true)
            sendEvent(name: "gallonsLastUsed", value: String.format("%3.1f",prevCumulative), displayed: true)
            sendEvent(name: "water", value: "dry", displayed: true)
            sendEvent(name: "alarmState", value: "Normal Operation", displayed: true)
            if (state.debug) log.debug "Data for the new app to display: ${delta}gpm (Power meter), ${cmd.scaledMeterValue}gals (Energy Consumption)"
            return
    	} else {
            sendEvent(name: "power", value: delta, displayed: true)
            sendEvent(name: "gpm", value: delta, displayed: true)
            if (state.debug) log.debug "flowing at ${delta} gpm"
            if (delta > device.currentState('flowHighValue')?.doubleValue) {
                sendEvent(name: "flowHighValue", value: String.format("%3.1f",delta), displayed: true)
            }
        	if (delta > gallonThreshhold) {
                sendEvent(name: "water", value: "wet", displayed: false)
                sendEvent(name: "alarmState", value: "High Flow Detected!", displayed: true)
        	} else {
                sendEvent(name: "water", value: "dry", displayed: false)
                sendEvent(name: "alarmState", value: "Water is currently flowing", displayed: true)
			}
            return
    }
	return
}

def zwaveEvent(physicalgraph.zwave.commands.alarmv2.AlarmReport cmd) {
	def map = [:]
    if (cmd.zwaveAlarmType == 8) { // Power Alarm
        if (cmd.zwaveAlarmEvent == 2) { // AC Mains Disconnected
            sendEvent(name: "alarmState", value: "Mains Disconnected!", descriptionText: text, displayed: true)
            sendEvent(name: "powerSource", value: "Power Loss!", displayed: true)
        } else if (cmd.zwaveAlarmEvent == 3) { // AC Mains Reconnected
            sendEvent(name: "alarmState", value: "Mains Reconnected", descriptionText: text, displayed: true)
            sendEvent(name: "powerSource", value: "AC Power", displayed: true)
        } else if (cmd.zwaveAlarmEvent == 0x0B) { // Replace Battery Now
            sendEvent(name: "alarmState", value: "Replace Battery Now", descriptionText: text, displayed: true)
        } else if (cmd.zwaveAlarmEvent == 0x00) { // Battery Replaced
            sendEvent(name: "alarmState", value: "Battery Replaced", descriptionText: text, displayed: true)
        }
    }
    else if (cmd.zwaveAlarmType == 4) {
    	map.name = "heatState"
        if (cmd.zwaveAlarmEvent == 0) {
            sendEvent(name: "alarmState", value: "Normal Operation", descriptionText: text, displayed: true)
        } else if (cmd.zwaveAlarmEvent == 1) {
            map.value = "overheated"
            sendEvent(name: "alarmState", value: "Overheating Detected!", descriptionText: text, displayed: true)
        } else if (cmd.zwaveAlarmEvent == 5) {
            map.value = "freezing"
            sendEvent(name: "alarmState", value: "Freezing Detected!", descriptionText: text, displayed: true)
        }
    }
	return map
}

def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd) {
	def map = [:]
	if(cmd.batteryLevel == 0xFF) {
		map.name = "battery"
		map.value = 1
		map.descriptionText = "${device.displayName} has a low battery"
		map.displayed = true
	} else {
		map.name = "battery"
		map.value = cmd.batteryLevel > 0 ? cmd.batteryLevel.toString() : 1
		map.unit = "%"
		map.displayed = false
	}
	return map
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
	log.debug "COMMAND CLASS: $cmd"
}

def getTemperature(value) {
	if(location.temperatureScale == "C"){
		return value
    } else {
        return Math.round(celsiusToFahrenheit(value))
    }
}

// PING is used by Device-Watch in attempt to reach the Device
def ping() {
    refresh()
}

def refresh() {
    if (state.debug) log.debug "${device.label} refresh"
	delayBetween([
        zwave.sensorMultilevelV5.sensorMultilevelGet().format(),
        zwave.manufacturerSpecificV2.manufacturerSpecificGet().format()
	])
}

def configure() {
	log.debug "Configuring FortrezZ flow meter interface (FMI)..."
	log.debug "Setting reporting interval to ${reportThreshhold}"
	log.debug "Setting gallon threshhold to ${gallonThreshhold}"
    def cmds = delayBetween([
		zwave.configurationV2.configurationSet(configurationValue: [(int)Math.round(reportThreshhold)], parameterNumber: 4, size: 1).format(),
    	zwave.configurationV2.configurationSet(configurationValue: [(int)Math.round(gallonThreshhold*10)], parameterNumber: 5, size: 1).format()
    ],200)
    log.debug "Configuration report for FortrezZ flow meter interface (FMI): '${cmds}'"
    cmds
}