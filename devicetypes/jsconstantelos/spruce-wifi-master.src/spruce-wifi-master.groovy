/**
 *  Spruce Controller wifi master *
 *  Copyright 2018 Plaid Systems
 *
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
 * Spruce Controller wifi master control tile
 * Manual Schedule tiles
 */
 
 metadata {
	definition (name: 'Spruce wifi master', namespace: 'jsconstantelos', author: 'Plaid Systems') {
		capability "Switch"
        capability "Switch Level"
        capability "Actuator"
        capability "Valve"
        capability "Refresh"
        
        attribute 'connected', 'string'
        attribute 'pause', 'string'
        
        command "on"
        command "off"
        command "online"
        command "offline"
        command "resume"
        command "pause"
        command "setLevel"
        command "setMoisture"
        command "generateEvent"
        command "update_settings"
        command "activeLabel"
        
	}
    preferences {
    	//input "debugOutput", "boolean", title: "Enable debug logging?", defaultValue: false, displayDuringSetup: false
    	input description: "1.0 5/2018 beta release", displayDuringSetup: false, type: "paragraph", element: "paragraph", title: "Spruce Connect version"
    	input description: "Zone settings are configured in the Spruce app.\n\nRefresh the configuration changes by opening the Spruce Connect SmartApp and saving.\n\nThe refresh button can be used to update the manual schedule list, but will error if a manual schedule is used in any automations.", displayDuringSetup: false, type: "paragraph", element: "paragraph", title: "Update Settings and Names"
    }
    tiles (scale: 2){
        multiAttributeTile(name:"sliderTile", type: "generic", width:6, height:4) {
            tileAttribute('device.status', key: 'PRIMARY_CONTROL') {
                attributeState 'ready', label: 'Ready', icon: 'http://www.plaidsystems.com/smartthings/st_spruce_leaf_225_top.png'
                attributeState 'active', label: "Active", icon: 'st.Outdoor.outdoor12', backgroundColor: '#46c2e8'
                //attributeState 'finished', label: 'Finished', icon: 'st.Outdoor.outdoor5', backgroundColor: '#46c2e8'            

                attributeState 'raintoday', label: 'Rain Today', icon: 'http://www.plaidsystems.com/smartthings/st_rain.png', backgroundColor: '#d65fe3'
                attributeState 'rainy', label: 'Rain', icon: 'http://www.plaidsystems.com/smartthings/st_rain.png', backgroundColor: '#d65fe3'
                attributeState 'raintom', label: 'Rain Tomorrow', icon: 'http://www.plaidsystems.com/smartthings/st_rain.png', backgroundColor: '#d65fe3'

                attributeState 'skipping', label: 'Skip', icon: 'st.Outdoor.outdoor20', backgroundColor: '#46c2e8'
                attributeState 'pause', label: 'PAUSE', icon: 'st.contact.contact.open', backgroundColor: '#e86d13'
                attributeState 'delayed', label: 'Delayed', icon: 'st.contact.contact.open', backgroundColor: '#e86d13'

                attributeState 'disable', label: 'Off', icon: 'st.secondary.off', backgroundColor: '#cccccc'
                attributeState 'warning', label: 'Warning', icon: 'http://www.plaidsystems.com/smartthings/st_spruce_leaf_225_top_yellow.png'
                attributeState 'alarm', label: 'Alarm', icon: 'http://www.plaidsystems.com/smartthings/st_spruce_leaf_225_s_red.png', backgroundColor: '#e66565'
            }
            
            tileAttribute ("device.level", key: "SLIDER_CONTROL") {
                attributeState "level", action:"switch level.setLevel"
            }
            tileAttribute("device.tileMessage", key: "SECONDARY_CONTROL") {
                attributeState "tileMessage", label: '${currentValue}'
            }
        }
        standardTile('switch', 'switch', width:2, height:1, decoration: 'flat') {
            state 'off', label: 'Start all Zones', action: 'on', icon: 'st.Outdoor.outdoor12', backgroundColor: '#ffffff'
            state 'on', label: 'stop', action: 'off', icon: 'st.Outdoor.outdoor12', backgroundColor: '#00a0dc'
		}
        standardTile('refresh', 'device.switch', inactiveLabel: false, decoration: 'flat') {
			state 'default', action: 'refresh', icon:'st.secondary.refresh'
		}
        standardTile ("amp", "amp", width: 1, height: 1, icon:"st.Health & Wellness.health9") {
            state "Ok", label:'${name}', icon:"st.Health & Wellness.health9", backgroundColor:"#ffffff"
            state "Low", label:'${name}', icon:"st.Health & Wellness.health9", backgroundColor:"#f1d801"
            state "High", label:'${name}', icon:"st.Health & Wellness.health9", backgroundColor:"#e86d13"
            state "Error", label:'${name}', icon:"st.Health & Wellness.health9", backgroundColor:"#bc2323"
        }
        standardTile("contact", "device.contact", width: 1, height: 1, decoration: 'flat') {
			state "closed", label: '', action: 'pause', icon: "st.contact.contact.closed", decoration: 'flat', backgroundColor: "#ffffff"
            state "open",   label: '', action: 'resume', icon: "st.contact.contact.open", decoration: 'flat',   backgroundColor: "#e86d13"			
		}
        standardTile("rainsensor", "device.rainsensor", decoration: 'flat') {			
			state "off", label: 'sensor', icon: 'http://www.plaidsystems.com/smartthings/st_drop_on.png'
            state "on", label: 'sensor', icon: 'http://www.plaidsystems.com/smartthings/st_drop_fill_blue.png'//st_drop_on_blue_small.png'
            state "disable", label: 'sensor', icon: 'http://www.plaidsystems.com/smartthings/st_drop_slash.png'
            state "enable", label: 'sensor', icon: 'http://www.plaidsystems.com/smartthings/st_drop_on.png'
		}
        valueTile("static_left", "static", width: 1, height: 1) {
            state "static", label: ""
        }
        valueTile("static", "static", width: 4, height: 1) {
            state "static", label: "Manual Schedules:"
        }
        valueTile("static_right", "static", width: 1, height: 1) {
            state "static", label: ""
        }
        childDeviceTile('schedule1', 'schedule1', childTileName: "switch")
        childDeviceTile('schedule2', 'schedule2', childTileName: "switch")
        childDeviceTile('schedule3', 'schedule3', childTileName: "switch")
        childDeviceTile('schedule4', 'schedule4', childTileName: "switch")
        childDeviceTile('schedule5', 'schedule5', childTileName: "switch")
        main (["sliderTile"])
    	details(["sliderTile","refresh","contact","switch","amp","rainsensor","static_left","static","static_right","schedule1","schedule2","schedule3","schedule4","schedule5"])
    }
}

def installed(){
	setLevel(10)
    setMoisture(0)
    setPause(closed)
    setRain(off)
    
    update_settings()
}

def updated() {
	log.debug "Updated"
}

def update_settings(){
	log.debug "update_settings master"
	//get and delete children avoids duplicate children
    
    try {
    	def children = getChildDevices()
        children.each{
        log.debug it
        	deleteChildDevice(it.deviceNetworkId)
        }
    }
    catch (e) {
    	log.debug "no children"
        }
        
	parent.child_schedules(device.deviceNetworkId)
}

//add schedule child devices
void createScheduleDevices(id, i, schedule, schName){
	log.debug "master child devices"
    log.debug schedule
    //def device = "${device.deviceNetworkId}".split('.')
    log.debug "${id}.${i}"
    
    //add children
    addChildDevice("Spruce wifi schedule", "${id}.${i}", null, [completedSetup: true, label: "${schName}", isComponent: true, componentName: "schedule${i}", componentLabel: "${schName}"])
}


def generateEvent(Map results) {
    def currentStatus = device.currentValue('status')
    log.debug "master status: ${currentStatus}"
    log.debug "master results: ${results}"
    
	//status dependent events
    if(currentStatus == 'active'){
        def messageCurrent = device.latestValue('tileMessage')
        def message = messageCurrent.split('\n')
        log.debug "message[0] ${message[0]}"
        sendEvent(name: "tileMessage", value: "${message[0]}\n${results.descriptionText}", displayed: false)
    }    
    else if (results.name == 'status'){
    	sendEvent(name: "${results.name}", value: "${results.value}", descriptionText: "${results.descriptionText}", displayed: true)
        sendEvent(name: "tileMessage", value: "${results.descriptionText}", displayed: false)
    }
    
	//all events
    if (results.name == "amp"){
        sendEvent(name: "${results.name}", value: "${results.value}", descriptionText: "${results.descriptionText}", displayed: false)
    }  
    if (results.name == "rainsensor"){
        sendEvent(name: "${results.name}", value: "${results.value}", descriptionText: "${results.descriptionText}", displayed: true)
    }
    if (results.name == "pause"){
        sendEvent(name: "${results.name}", value: "${results.value}", descriptionText: "${results.descriptionText}", displayed: true)
        if(results.value == 'on')sendEvent(name: "status", value: "pause", displayed: false)
        if(results.value == 'off')sendEvent(name: "status", value: "active", displayed: false)
    }
    if (results.name == "switch"){
        if (results.value == "on") switchon(results)
        else off()
        sendEvent(name: "${results.name}", value: "${results.value}", descriptionText: "${results.descriptionText}", displayed: true)
        if(currentStatus != 'active')sendEvent(name: "tileMessage", value: "${results.descriptionText}", displayed: false)
    }
    if (results.name == "contact"){
        sendEvent(name: "${results.name}", value: "${results.value}", descriptionText: "${results.descriptionText}", displayed: true)
    } 

  
}

//set minutes
def setLevel(percent) {
	log.debug "setLevel: ${percent}"
	sendEvent(name: "level", value: percent, displayed: false)
}

//set moisture
def setMoisture(percent) {
	log.debug "setLevel: ${percent}"
	sendEvent(name: "moisture", value: percent, displayed: false)
}

//set rainSensor
def setRain(value) {
	log.debug "setRain: ${value}"
	sendEvent(name: "rainsensor", value: value, displayed: false)
}

//set Pause
def setPause(value) {
	log.debug "setPause: ${value}"
	sendEvent(name: "pause", value: value, displayed: false)
}

//set Connected
def setConnected(value) {
	log.debug "setConnected: ${value}"
	sendEvent(name: "connected", value: value, displayed: false)
}

//************* Commands to/from pause and schedule children *******************
def zoneon(dni) {	
    log.debug "step 1"
   	def childDevice = childDevices.find{it.deviceNetworkId == dni}    
    
    if (childDevice.currentValue('switch') != 'on'){
    	log.debug "master zoneon ${childDevice} ${dni} on"
    	def result = [name: 'switch', value: 'on', descriptionText: "zone is on", isStateChange: true, displayed: true]    
    	childDevice.sendEvent(result)
        
        if("${childDevice}" != "Spruce Pause") parent.scheduleOnOff(childDevice, 1)
    	else pause()
    }
}

def zoneoff(dni) {    
    def childDevice = childDevices.find{it.deviceNetworkId == dni}
    
    if (childDevice.currentValue('switch') != 'off'){
    	log.debug "master zoneoff ${childDevice} off"
    	def result = [name: 'switch', value: 'off', descriptionText: "zone is off", isStateChange: true, displayed: true]
    	childDevice.sendEvent(result)
        
        if("${childDevice}" != "Spruce Pause") parent.scheduleOnOff(childDevice, 0)
    	else resume()
    }
}

void switchon(results){
	sendEvent(name: "status", value: 'active', descriptionText: "${results.descriptionText}", displayed: false)
    //sendEvent(name: "tileMessage", value: 'watering...', descriptionText: "Spruce is watering", displayed: false)
}

void on(){
	def runtime = device.latestValue('level') * 60
	parent.runAll(runtime)
}

void off(){	
    sendEvent(name: "switch", value: 'off', descriptionText: "${device.label} is off", displayed: false)
    sendEvent(name: "status", value: 'ready', descriptionText: "${device.label} is off", displayed: true)
    sendEvent(name: "tileMessage", value: 'Idle', descriptionText: "${device.label} is off", displayed: false)
    allSchedulesOff()
    parent.send_stop()
}

void allSchedulesOff(){
	def children = getChildDevices()
    children.each { child ->
        //log.debug "child ${child.displayName} has deviceNetworkId ${child.deviceNetworkId}"
        def result = [name: 'switch', value: 'off', descriptionText: "${child.displayName} is off", isStateChange: true, displayed: true]    
    	child.sendEvent(result)
    }    	
}

void refresh(){	   
    parent.getsettings()
}

void online(){
	sendEvent(name: "connected", value: 'online', descriptionText: "Spruce is Online", displayed: true)    
    parent.getsettings()
}

void offline(){
	sendEvent(name: "connected", value: 'offline', descriptionText: "Spruce is Offline", displayed: true)
    parent.getsettings()
}


void pause(){
	//def runtime = device.latestValue('level')
	sendEvent(name: "contact", value: 'open', descriptionText: "Contact Pause", displayed: true)
    parent.send_pause()
}

void resume(){	
    sendEvent(name: "contact", value: 'closed', descriptionText: "Resume", displayed: true)
    parent.send_resume()
}