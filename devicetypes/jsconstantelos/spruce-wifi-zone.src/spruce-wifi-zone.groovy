/**
 *  Spruce Controller wifi zone child *
 *  Copyright 2017 Plaid Systems
 *
 *	Author: NC
 *	Date: 2017-6
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
 -------------5-2018 update---------------
 * zone child for wifi controller
 * 
 */
 
 import groovy.json.JsonOutput
 metadata {
	definition (name: 'Spruce wifi zone', namespace: 'jsconstantelos', author: 'Plaid Systems') {
		capability "Switch"
        capability "Switch Level"
        capability "Actuator"
        capability "Valve"
        capability "Health Check"
        capability "Sensor"
        capability "Refresh"
        
        command "on"
        command "off"
        command "updated"
        command "refresh"
        command "setLevel"
        command "setAmp"
        command "setGPM"
        command "setMoisture"
        command "setLastWater"
        command "generateEvent"
	}
    preferences {
    	input description: "Zone settings are configured in the Spruce app.\n\nRefresh the configuration changes by opening the Spruce Connect SmartApp and saving.", displayDuringSetup: false, type: "paragraph", element: "paragraph", title: "Update Settings and Names"
    }
    tiles (scale: 2){
        multiAttributeTile(name:"sliderTile", type: "generic", width:6, height:4){//, canChangeIcon: true) {
            tileAttribute ("switch", key: "PRIMARY_CONTROL") {
                attributeState "off", label:'${name}', action:"on", icon:"st.valves.water.closed", backgroundColor:"#ffffff", nextState:"turningOn"
                attributeState "que", label:'que', action:"off", icon:"st.valves.water.closed", backgroundColor:"#f1d801", nextState:"turningOff"
                attributeState "on", label:'${name}', action:"off", icon:"st.valves.water.open", backgroundColor:"#00a0dc", nextState:"turningOff"
            }            
            tileAttribute ("device.level", key: "SLIDER_CONTROL") {
                attributeState "level", action:"switch level.setLevel"
            }
            tileAttribute("device.tileMessage", key: "SECONDARY_CONTROL") {
                attributeState "tileMessage", label: '${currentValue}'     
            }
        }
        valueTile("lastwater", "device.lastwater", width: 5, height: 2) {
            state "lastwater", label: 'Last water:\n${currentValue}'
        }
        valueTile("nextwater", "nextwater", width: 3, height: 1) {
            state "nextwater", label: 'Next water:\n${currentValue}'
        }        
        valueTile("static", "static", width: 2, height: 1) {
            state "static", label: "Sensor:"
        }
        standardTile('sensorName', 'sensorName', inactiveLabel: false, decoration: 'flat', width: 2, height: 1) {
			state 'sensorName', action: 'refresh', label: '${currentValue}'
		}
        valueTile("humidity", "device.humidity", width: 2, height: 2) {
            state("humidity", label:'${currentValue}%', unit:"%",
                backgroundColors:[
                    [value: 31, color: "#153591"],
                    [value: 44, color: "#1e9cbb"],
                    [value: 59, color: "#90d2a7"],
                    [value: 74, color: "#44b621"],
                    [value: 84, color: "#f1d801"],
                    [value: 95, color: "#d04e00"],
                    [value: 96, color: "#bc2323"]
                ]
            )
        }
        valueTile("temperature", "device.temperature", width: 2, height: 2) {
            state("temperature", label:'${currentValue}°',
                backgroundColors:[
                    [value: 31, color: "#153591"],
                    [value: 44, color: "#1e9cbb"],
                    [value: 59, color: "#90d2a7"],
                    [value: 74, color: "#44b621"],
                    [value: 84, color: "#f1d801"],
                    [value: 95, color: "#d04e00"],
                    [value: 96, color: "#bc2323"]
                ]
            )
        }
        standardTile("nozzle", "nozzle", width: 2, height: 1, icon:"st.valves.water.closed") {
            state "nozzle", label: 'nozzle:\n${currentValue}'
        }
        standardTile("landscape", "landscape", width: 2, height: 1) {
            state "landscape", label: 'landscape:\n${currentValue}'
        }
        standardTile("soil", "soil", width: 2, height: 1) {
            state "soil", label: 'soil:\n${currentValue}'
        }        
        valueTile("zone_num", "zone_num", width: 1, height: 1) {
            state("zone_num", label:'Zone\n${currentValue}')
        }
        standardTile ("amp", "amp", width: 1, height: 1, icon:"st.Health & Wellness.health9") {
                state "Ok", label:'${name}', icon:"st.Health & Wellness.health9", backgroundColor:"#ffffff"
                state "Low", label:'${name}', icon:"st.Health & Wellness.health9", backgroundColor:"#f1d801"
                state "High", label:'${name}', icon:"st.Health & Wellness.health9", backgroundColor:"#e86d13"
                state "Error", label:'${name}', icon:"st.Health & Wellness.health9", backgroundColor:"#bc2323"
        }
		standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label: "", action: "refresh.refresh", icon: "st.secondary.refresh"
		}
        main (["sliderTile"])
    	details(["sliderTile","lastwater","zone_num","amp","nozzle","soil","landscape","static","humidity","temperature","sensorName","refresh"])
    }
}

def installed(){
	setLevel(10)	//default zone on time
    sendEvent(name: "lastwater", value: "this tile will display the last time that the zone was watered", displayed: false)
    updated()
}

//when device preferences are changed
def updated(){	
    log.debug "device updated"
    sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "cloud", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
	sendEvent(name: "DeviceWatch-Enroll", value: JsonOutput.toJson([protocol: "cloud", scheme:"untracked"]), displayed: false)
    parent.child_zones(device.deviceNetworkId)	//get child zone settings
}

def refresh(){
    sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "cloud", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
	sendEvent(name: "DeviceWatch-Enroll", value: JsonOutput.toJson([protocol: "cloud", scheme:"untracked"]), displayed: false)
	parent.getsettings()
}

def ping(){
	parent.getsettings()
}

def poll(){
	parent.getsettings()
}

def generateEvent(Map results) {
  log.debug results
  
  sendEvent(name: "${results.name}", value: "${results.value}", descriptionText: "${results.descriptionText}", isStateChange: true, displayed: "${results.displayed}")
  if (results.name == "switch") setLastWater(results)
  
  return null
}

def setLastWater(results){
	def df = new java.text.SimpleDateFormat("EEEE hh:mm a")
    // Ensure the new date object is set to hub local time zone
    df.setTimeZone(location.timeZone)
    def day = df.format(new Date())
    
    def runtime = device.latestValue('level')
    if(results.value == 'on') day += "\nwatering now"
    else{    
        use(groovy.time.TimeCategory){
            def lastOnDate = device.latestState('lastwater').date.time
            def offDate = new Date(now()).time
                    
            def duration = Math.round((offDate - lastOnDate)/60000)
            log.debug duration.toInteger()
            log.debug "${offDate} - ${lastOnDate} = ${duration}"
            day += "\n${duration} minutes"
        }
    }
	sendEvent(name: "tileMessage", value: "${results.descriptionText}", displayed: false)
    sendEvent(name: "lastwater", value: day, isStateChange: true, displayed: false)
}

void on(){
	def runtime = device.latestValue('level')
	log.debug runtime
	parent.zoneOnOff(device.deviceNetworkId, 1, runtime)
}

void off(){
    parent.zoneOnOff(device.deviceNetworkId, 0, 0)
}

//settings from cloud
def childSettings(zone_num, Map results){
	log.debug "Spruce Zone ${zone_num} settings ${results}"
    sendEvent(name: "zone_num", value: zone_num, isStateChange: true, displayed: false)
    
    if (results.sensor_name != null) sendEvent(name: "sensorName", value: results.sensor_name, isStateChange: true, displayed: false)
    else sendEvent(name: "sensorName", value: "not set", isStateChange: true, displayed: false)
    if (results.soil_type != null) sendEvent(name: "soil", value: results.soil_type, isStateChange: true, displayed: false)
    else sendEvent(name: "soil", value: "not set", isStateChange: true, displayed: false)
    if (results.nozzle_type != null) sendEvent(name: "nozzle", value: results.nozzle_type, isStateChange: true, displayed: false)
    else sendEvent(name: "nozzle", value: "not set", isStateChange: true, displayed: false)
    if (results.landscape_type != null) sendEvent(name: "landscape", value: results.landscape_type, isStateChange: true, displayed: false)
    else sendEvent(name: "landscape", value: "not set", isStateChange: true, displayed: false)
    
}

//------not used?-----------

//set minutes
def setLevel(percent) {
	log.debug "setLevel: ${percent}"
	sendEvent(name: "level", value: percent, displayed: false)
}

//set moisture
def setHumidity(percent) {
	log.debug "setLevel: ${percent}"
	sendEvent(name: "humidity", value: percent, displayed: false)
}

//update settings
def getSettings() {
	log.debug "getSettings"
	
}