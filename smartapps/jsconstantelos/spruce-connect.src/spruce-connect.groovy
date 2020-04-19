/**
 *  Spruce Web Connect Cloud-to-Cloud
 *  v1.0 - 04/01/18 - convert to cloud-cloud
 *  v1.1 - 06/05/18 - correct IOS error, rename page to pageController
 *  v1.2 - 06/20/19 - temperature units, add time resume delay for contact
 *
 *  Copyright 2018 Plaid Systems
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
 */
definition(
    name: "Spruce Connect",
    namespace: "jsconstantelos",
    author: "Plaid Systems",
    description: "Connect Spruce Controller and Sensors to Samsung SmartThings",
    category: "",
    iconUrl: "http://www.plaidsystems.com/smartthings/st_spruce_leaf_250f.png",
    iconX2Url: "http://www.plaidsystems.com/smartthings/st_spruce_leaf_250f.png",
    iconX3Url: "http://www.plaidsystems.com/smartthings/st_spruce_leaf_250f.png",
    oauth: true,
    singleInstance: true)
{
	//appSetting "clientId"
	//appSetting "clientSecret"
    //appSetting "serverUrl"
    
    atomicState.clientid = "smartthings-beta"
    atomicState.clientSecret = "rf1ce71eec8361fdab2253d1d8y819a5"
}

//-----------------pages----------------------------
preferences (oauthPage: "authPage") {
	page(name: "authPage")
 	page(name: "pageConnect")
	page(name: "pageController")
    page(name: "pageDevices")
    page(name: "pageUnsetKey")
}

def authPage(){
    if(!atomicState.accessToken) atomicState.accessToken = createAccessToken()	//set = so token is saved to atomicState 
    
    if(!atomicState.authToken){
    	log.debug "pageConnect"
		pageConnect()
    }
    else if (!controllerSelected()){
    	log.debug "pageController"
    	pageController()
    }
    else pageDevices()
}

def controllerSelected(){	
	if (settings.controller != null) return true
    return false
}

def pageConnect(){
    if(!atomicState.authToken){		
        def redirectUrl = "https://graph.api.smartthings.com/oauth/initialize?appId=${app.id}&access_token=${atomicState.accessToken}&apiServerUrl=${getApiServerUrl()}"
		
        dynamicPage(name: "pageConnect", title: "Connect Account",  uninstall: false, install:false) {
            section {
                href url: redirectUrl, style:"embedded", required:false, title:"Connect Spruce Account", image: 'http://www.plaidsystems.com/smartthings/st_spruce_leaf_250f.png', description: "Login to grant access"
            }
        }
    }    
    else pageController()
}

def pageController(){
    if (atomicState.authToken && getControllerList()){
    	def select_device = getSpruceDevices()
        dynamicPage(name: "pageController", uninstall: true, install:false, nextPage: "pageDevices") {            
            section("Select Spruce Controller\n to connect with SmartThings") {
            	input(name: "controller", title:"Select Spruce Controller:", type: "enum", required:true, multiple:false, description: "Tap to choose", metadata:[values:select_device])                
    		}
            section("Spruce-Connect v1.2\n 6.20.2019")
        }
    }    
    else pageDevices()
}

def pageDevices(){
	if (atomicState.authToken && controllerSelected() && getControllerSettings()){
      log.debug atomicState.zoneUpdate
      log.debug "pageDevices"
        dynamicPage(name: "pageDevices", uninstall: true, install:true) {
        	if(atomicState.zoneUpdate == true) section("Device changes found, device tiles will be updated! \n\nErrors will occur if devices are assigned to Automations and SmartApps, please remove before updating.\n"){}
         	section("Select settings for connected devices\nConnected controller: ${settings.controller}\nConnected zones: ${zoneList()}") {
                input(name: "notifications", title:"Select Notifications used in SmartThings:", type: "enum", required:false, multiple:true, description: "Tap to choose", metadata:[values: ['Schedule','Zone','Valve Fault']])
                input(name: "pause", title:"Turn on to create Spruce Pause control that is accesible from automation routines to pause and resume water", type: 'bool')
            }
            section("SmartThings Spruce Sensors that will report to Spruce Cloud:") {
                input "sensors", "capability.relativeHumidityMeasurement", title: "Spruce Moisture sensors:", required: false, multiple: true
            }
            section("SmartThings Contact Sensors that will pause and resume the schedule:") {
            	input "contacts", "capability.contactSensor", title: "Contact sensors will pause or resume water:", required: false, multiple: true
                input "delay", "number", title: "The delay in minutes that water will resume after the contact is closed, default=5, max=119", required: false, range: '0..119'
            }
            section("Spruce-Connect v1.2\n 6.20.2019")
        }   
    }    
    else {
    	atomicState.authToken = null
    	authPage()
    }
}

def zoneList(){
	def tempZoneMap = atomicState.zoneMap
    def zoneString = ""
    tempZoneMap.sort().each{
        def zone_name = "Zone ${it.key}"
        if ("${tempZoneMap[it.key]['zone_name']}" != 'null') zone_name = "${tempZoneMap[it.key]['zone_name']}"
    	zoneString += zone_name
        zoneString += ","
    }
    return zoneString;
}

mappings {
  path("/event/:command") {
    action: [
      POST: "event"
    ]
  }  
  path("/zonestate/:command") {  
    action: [
      POST: "zone_state"
    ]
  }
  path("/rain/:command") {
    action: [
      POST: "rain"
    ]
  }
  path("/pause/:command") {
    action: [
      POST: "pause"
    ]
  }
  path("/oauth/initialize") {
  	action: [
    	GET: "oauthInitUrl"
    ]
  }
  path("/oauth/callback") {
  	action: [
    	GET: "callback"
    ]
  }
}

//***************** install *******************************

def installed() {
	log.debug "Installed with settings: ${settings}"    
    state.counter = state.counter ? state.counter + 1 : 1  
    
    if (state.counter == 1) initialize()    
}

def updated() {
	log.debug "Updated with settings: ${settings}"	
	if (state.counter == 1){
    	unsubscribe()
    	initialize()
        }
}

def initialize() {	
    log.debug "initialize"    
    atomicState.zones_on = 0
    
    if (settings.sensors) getSensors()
    if (settings.contacts) getContacts()
    
    //add devices to web, check for schedules
    if(atomicState.accessToken){
    	log.debug getApiServerUrl()
        addDevices()
    	createChildDevices()        
	}    
}

//get zone device list
def getSpruceDevices(){	
   def controllers = []
   
   def tempSwitch = atomicState.switches
   int i=0
   tempSwitch.each{
   	controllers[i] = it.key    
    i++
   }   
   return controllers       
}

//sensor subscriptions
def getSensors(){    
    log.debug "getSensors: " + settings.sensors    
    
    def tempSensors = [:]
    settings.sensors.each{
    	tempSensors[it]= (it.device.zigbeeId)
        }
    atomicState.sensors = tempSensors
    
    subscribe(settings.sensors, "humidity", sensorHandler)
    subscribe(settings.sensors, "temperature", sensorHandler)
    subscribe(settings.sensors, "battery", sensorHandler)    
}

//contact subscriptions
def getContacts(){    
    log.debug "getContacts: " + settings.contacts    
    
    def tempContacts = [:]
    settings.contacts.each{
    	tempContacts[it]= (it.device.zigbeeId)
        }
    atomicState.contacts = tempContacts
    
    subscribe(settings.contacts, "contact", contactHandler)    
}

//------------------create and remove child tiles------------------------------

//create zone tiles children
private void createChildDevices(){
	log.debug "create zone children ${atomicState.zoneUpdate} with ${app.id}"
    if(atomicState.zoneUpdate == true){
    	removeChildDevices()
    
        //get Spruce Controller Name
        def master
        def tempSwitch = atomicState.switches
        tempSwitch.each{
            master = it.key
        }
        addChildDevice("Spruce wifi master", "${app.id}.0", null, [completedSetup: true, label: master, isComponent: false, componentName: "wifi", componentLabel: "Wifi"])

        //Create children   
        def tempZoneMap = atomicState.zoneMap
        tempZoneMap.each{            
            addChildDevice("Spruce wifi zone", "${app.id}.${it.key}", null, [completedSetup: true, label: "${tempZoneMap[it.key]['zone_name']}", isComponent: false, componentName: "wifi", componentLabel: "Wifi"])
        }
    }
    if (atomicState.manUpdate == true) child_schedules("${app.id}.0")
        
}

//remove zone tiles children
private removeChildDevices() {
	log.debug "remove children"
	
    //get and delete children avoids duplicate children
    def children = getChildDevices()  
    log.debug children
    if(children != null){
        children.each{
        	deleteChildDevice(it.deviceNetworkId)
        }
    }       
}

//add child device tiles to master
def child_schedules(dni){
	log.debug "get child tiles for master"
	def childDevice = childDevices.find{it.deviceNetworkId == dni}
    log.debug "${childDevice}"
    
    if(settings.pause == true) childDevice.createScheduleDevices("${app.id}", 99, 0, "Spruce Pause")
    
    //add manual schedules
    def manualschs = atomicState.manualMap    
    manualschs.each{        
    	childDevice.createScheduleDevices("${app.id}", it.key, manualschs[it.key]["scheduleid"], manualschs[it.key]["name"])        
        }
}

//add zone settings to child tile
def child_zones(dni){
	def childDevice = childDevices.find{it.deviceNetworkId == dni}
    log.debug "${childDevice.deviceNetworkId}"
        
    def tempZoneMap = atomicState.zoneMap    
    tempZoneMap.each{    	
        if("${app.id}.${it.key}" == childDevice.deviceNetworkId){
        	log.debug it.key
            childDevice.childSettings(it.key, tempZoneMap[it.key])
        }
    }    
}


//add devices to spruce webapp
def addDevices(){	
    //add sensors to web
    def key = atomicState.authToken
    def tempSensorMap = atomicState.sensors
    tempSensorMap.each{
    	log.debug "Add Sensor to DB ${it.key}"
    	def POSTparams = [
            uri: "https://api.spruceirrigation.com/v2/sensor",
            headers: [ 	"Authorization": "Bearer ${key}"],
            body: [
                device_id: it.value,
                sensor_name: it.key,                
                gateway: "smartthings"
                ]
        ]
        //sendPost(POSTparams)
        try{
            httpPost(POSTparams){
                resp -> //resp.data {
                    log.debug "${resp.data}"               
            }                
        } 
        catch (e) {
            log.debug "send DB error: $e"
        }
    }    

}

//***************** setup commands ******************************

//get controller list
def getControllerList(){
	def key = atomicState.authToken
    def tempMap = [:]
    def newuri =  "https://api.spruceirrigation.com/v2/controllers"    
    log.debug newuri
    
    def GETparams = [
        uri: newuri,		
        headers: [ 	"Authorization": "Bearer ${key}"],
    ]

    try{ httpGet(GETparams) { resp ->	
    	log.debug "resp-> ${resp.data}"        
        resp.data.each{        	
            tempMap += ["${resp.data[it.key]['controller_name']}": it.key]
        }        
      }
    }
    catch (e) {
        respMessage += "No schedules set, API error: $e"        
    }
    atomicState.switches = tempMap
    
    return true
}

//check for pre-set schedules
def getControllerSettings(){
	log.debug "-----------settings----------------"
    def respMessage = ""
    def key = atomicState.authToken

	def controller_id
   	def tempSwitch = atomicState.switches    
    tempSwitch.each{
        if (it.key == settings.controller) controller_id = it.value
    }
   
    def newuri =  "https://api.spruceirrigation.com/v2/controller_settings?controller_id="
	newuri += controller_id
    log.debug newuri
        
    def scheduleType
    def scheduleID = []
    def sensorMap = []
    def zoneID
    def tempSchMap = [:]    
    def tempManMap = [:]    
    def tempZoneMap = [:]
    def tempSensorMap = atomicState.sensors

    def GETparams = [
        uri: newuri,		
        headers: [ 	"Authorization": "Bearer ${key}"],
    ]

    try{ httpGet(GETparams) { resp ->	
        //get schedule list
        log.debug "Get setting for ${resp.data.controller_name}"
        def i = 1
        def j = 1
        
        //def schedules = resp.data.schedules
        resp.data.schedules.each{
        	def schPath = resp.data.schedules[it.key]
        	if(schPath['schedule_enabled'] == "1"){
                if(schPath['schedule_type'] == "manual"){            	
                    tempManMap[i] = ['scheduleid' : it.key, 'name' : schPath['schedule_name']]
                    i++
                }
                else {            	
                    tempSchMap[j] = ['scheduleid' : it.key, 'name' : schPath['schedule_name']]
                    j++
                }
            }
        }
        
        resp.data.zone.each{
        	if(resp.data.zone[it.key]['zenabled'] == '1'){
            	
                def zoneData = resp.data.zone[it.key]
            	def ks = "${it.key}"
            	if (ks.toInteger()<10) ks = "0${it.key}"
                def zoneName = "Zone ${ks}"
                if ("${zoneData['zone_name']}" != 'null') zoneName = zoneData['zone_name']
                
                tempZoneMap["${ks}"] = [ 'zone_name': zoneName, 'landscape_type': zoneData['landscape_type'], 'nozzle_type': zoneData['nozzle_type'], 'soil_type': zoneData['soil_type'], 'gpm': zoneData['gpm'] ]
                
                //add sensor assignment
                if (zoneData['sensor']){
                	tempZoneMap["${ks}"]['sensor'] = zoneData['sensor']
                	tempSensorMap.each{
                        if (it.value == zoneData['sensor']) tempZoneMap["${ks}"]['sensor_name'] = it.key
                    }
                }
                
           	}
        }      
        
    }
    
    
    }
    catch (e) {
        respMessage += "No schedules set, API error: $e"
        if (refreshAuthToken()) getControllerSettings()
        else return false
    }
    
    log.debug tempManMap
    
    if(atomicState.manualMap != null){
        if ("${tempManMap.sort()}" != "${atomicState.manualMap.sort()}") atomicState.manualUpdate = true
        else atomicState.manualUpdate = false

        //do we update zone child devices
        atomicState.zoneUpdate = false
        def tempMap = atomicState.zoneMap
        def names = ""
        def newnames = ""
        tempZoneMap.sort().each{
            newnames += tempZoneMap[it.key]['zone_name']
        }
        tempMap.sort().each{    
            names += tempMap[it.key]['zone_name']
        }    
        if(names != newnames) atomicState.zoneUpdate = true
    }
    else {
    	atomicState.zoneUpdate = true
        atomicState.manualUpdate = true
	}
        
    atomicState.scheduleMap = tempSchMap
    atomicState.manualMap = tempManMap    
    atomicState.zoneMap = tempZoneMap    
    
    return true
    
}


//***************** event handlers *******************************

def parse(description) {
	log.debug(description)
}

def getScheduleName(scheduleid){	
    def scheduleName = scheduleid
    
    def tempSchedules = atomicState.manualMap
    tempSchedules.each{
    	if(tempSchedules[it.key]['scheduleid'] == scheduleid) scheduleName = "${tempSchedules[it.key]['name']}"
    }
    
    tempSchedules = atomicState.scheduleMap
    tempSchedules.each{
    	if(tempSchedules[it.key]['scheduleid'] == scheduleid) scheduleName = "${tempSchedules[it.key]['name']}"
    }
        
	return scheduleName
}

//sensor evts
def sensorHandler(evt) {
    log.debug "sensorHandler: ${evt.device}, ${evt.name}, ${evt.value}"
    
    def device = atomicState.sensors["${evt.device}"]
    def value = evt.value
    def uri = "https://api.spruceirrigation.com/v2/"
    
    if (evt.name == "humidity") uri += "moisture"
    
    if (evt.name == "temperature"){
    	uri += "temperature"
        if (evt.unit == "C") value = evt.value.toInteger() * 9/5 + 32
    }
        
    //added for battery
    if (evt.name == "battery") value = evt.value.toInteger() * 5 + 2500
    
    def childDevice = ''
    def tempZoneMap = atomicState.zoneMap
    
    if (evt.name != "battery"){
        tempZoneMap.each{ zone->  	      
            if(tempZoneMap[zone.key]['sensor'] == device){                
                childDevice = childDevices.find{it.deviceNetworkId == "${app.id}.${zone.key}"}
                if (childDevice){
                    log.debug "Sensor ${childDevice} ${tempZoneMap[zone.key]['sensor']} ${app.id}.${zone.key}"
                    def result = [name: evt.name, value: value, descriptionText: evt.name, isStateChange: true, displayed: false]
                    log.debug result
                    childDevice.generateEvent(result)
                }
            }
        }
    }
    
    def POSTparams = [
                    uri: uri,
                    headers: [ 	"Authorization": "Bearer ${atomicState.authToken}"],
                    body: [
                        deviceid: device,
                        value: value                        
                    ]
                ]

	sendPost(POSTparams)
}

//contact evts
def contactHandler(evt) {
    log.debug "contactHandler: ${evt.device}, ${evt.name}, ${evt.value}"
    
    def device = atomicState.contacts["${evt.device}"]    
    def value = evt.value
        
    def childDevice = childDevices.find{it.deviceNetworkId == "${app.id}.0"}
    
    log.debug "Found ${childDevice}"
    if (childDevice != null){    	
        def result = [name: evt.name, value: value, descriptionText: evt.name, isStateChange: true, displayed: false]
        log.debug result
        childDevice.generateEvent(result)
    }
    
    int delay_secs = 0
    if (settings.delay) delay_secs = settings.delay * 60
    
    if (value == 'open') send_pause(0)
    else runIn(delay_secs, send_resume)
}




//**************************** incoming commands **************************************

//*************** master child ***************

//refresh settings
void getsettings(){
	log.debug "Update Settings"
    
	if (getControllerSettings()){
		def children = getChildDevices()
        children.each { child ->
            //log.debug child.deviceNetworkId
            child_zones(child.deviceNetworkId)
        }
    }
        
    def childDevice = childDevices.find{it.deviceNetworkId == "${app.id}.0"}
    childDevice.update_settings()
}

//big tile events
def event(){
	log.debug "cloud event: ${params.command}"
    def command = params.command
	def event = command.split(',')
    
	def masterDevice = childDevices.find{it.deviceNetworkId == "${app.id}.0"}
    if (masterDevice != null){  	
        def scheduleName = getScheduleName(event[2])
        def result = [name: 'status', value: "${event[0]}", descriptionText: "${scheduleName} starting\n${event[1]}", isStateChange: true, displayed: false]
        log.debug result        
        masterDevice.generateEvent(result)
        
        def tempManualMap = atomicState.manualMap        
        tempManualMap.each{
            if ("${tempManualMap[it.key]['scheduleid']}" == "${event[2]}") masterDevice.zoneon("${app.id}.${it.key}")
        }
    }
    return [error: false, return_value: 1]
}



//rain sensor onoff
def rain(){
	log.debug(params.command)
    // use the built-in request object to get the command parameter
    def command = params.command
    log.debug "Spruce incoming rain=>>  ${command}"
    
    def zoneonoff = command.split(',')
            
    def name = 'rainsensor'
    def value = (zoneonoff[1].toInteger() == 1 ? 'on' : 'off')
    def message = "rain sensor is ${value}"
            
    def masterDevice = childDevices.find{it.deviceNetworkId == "${app.id}.0"}
    
    def result = [name: name, value: value, descriptionText: "${masterDevice} ${message}", isStateChange: true, displayed: true]
    log.debug result
    masterDevice.generateEvent(result)
    
    return [error: false, return_value: 1]
}

//pause onoff
def pause(){
	log.debug(params.command)
    // use the built-in request object to get the command parameter
    def command = params.command
    log.debug "Spruce incoming pause=>>  ${command}"
    
    def zoneonoff = command.split(',')
            
    def name = 'pause'
    def value = (zoneonoff[1].toInteger() == 1 ? 'on' : 'off')
    def message = "pause is ${value}"
            
    def masterDevice = childDevices.find{it.deviceNetworkId == "${app.id}.0"}
    
    def result = [name: name, value: value, descriptionText: "${masterDevice} ${message}", isStateChange: true, displayed: true]
    log.debug result
    masterDevice.generateEvent(result)
    
    return [error: false, return_value: 1]
}

//*********** child zone devices *************

//turn on/off zones of child devices
def zone_state(){
	log.debug "cloud zone_state: ${params.command}"
    // use the built-in request object to get the command parameter
    def command = params.command
    log.debug "Spruce incoming zone=>>  ${command}"
    
    //check sz to filter out command
    def zoneonoff = command.split(',')
    def sz = zoneonoff.size()
    
    def ks
    def scheduleid
    def scheduleName
    def tempSchMap = atomicState.scheduleMap
    if (zoneonoff[1].toInteger() == 0) ks = "0"
    else if (zoneonoff[1].toInteger()<10) ks = "0${zoneonoff[1]}"
   	else ks = zoneonoff[1]

    def childDevice = childDevices.find{it.deviceNetworkId == "${app.id}.${ks}"}
    def masterDevice = childDevices.find{it.deviceNetworkId == "${app.id}.0"}    
    def name = 'switch'
    def value
    def message
    def gpm
    def amp    
        
    int zone_on = atomicState.zones_on
    switch(zoneonoff[0]) {
        case "zon":            
            value = 'on'
            message = "on"
            //if (ks != "0")
            zone_on++
            if (zoneonoff[2].toInteger() != 0) message += " for ${Math.round(zoneonoff[2].toInteger()/60)} mins"            
            break
        case "zoff":
            value = 'off'
            message = "off"
            //if (ks != "0")
            zone_on--
            break        
    }
    
    if (zone_on < 0) zone_on = 0
    log.debug "zone_on count: ${zone_on}"
    /*
    if (atomicState.zones_on == 0 && zone_on == 1) masterDevice.generateEvent([name: 'switch', value: "on", descriptionText: "${settings.controller} ${message}", isStateChange: true, displayed: true])
    else if (atomicState.zones_on >= 1 && zone_on == 0) masterDevice.generateEvent([name: 'switch', value: "off", descriptionText: "${settings.controller} ${message}", isStateChange: true, displayed: true])    
    */
    atomicState.zones_on = zone_on
	 
    if (zoneonoff[0] == "zoff" && sz >= 5){
    	gpm = [name: 'gpm', value: "${zoneonoff[3]}", descriptionText: "${childDevice} gpm flow ${zoneonoff[3]}", isStateChange: true, displayed: true]
        amp = [name: 'amp', value: "${zoneonoff[4]}", descriptionText: "${childDevice} valve check ${zoneonoff[4]}", isStateChange: true, displayed: true]
        childDevice.generateEvent(gpm)
        childDevice.generateEvent(amp)
        
        masterDevice.generateEvent(amp)
        if ("${zoneonoff[3]}" != 'ok') note('Valve Fault', "${childDevice} gpm flow ${zoneonoff[3]}")
        if ("${zoneonoff[4]}" != 'ok') note('Valve Fault', "${childDevice} valve check ${zoneonoff[4]}")
    }
    
    def zoneResult = [name: name, value: value, descriptionText: "${childDevice} ${message}", isStateChange: true, displayed: true]
    
    log.debug zoneResult
    childDevice.generateEvent(zoneResult)
    
    def masterResult = [name: name, value: value, descriptionText: "${childDevice} ${message}", isStateChange: true, displayed: true]
    if (ks == "0"){
    	if (value == 'off') masterResult = [name: 'status', value: 'ready', descriptionText: "${childDevice} ${message}", isStateChange: true, displayed: true]
    	else if (value == 'on') masterResult = [name: 'status', value: 'active', descriptionText: "${childDevice} ${message}", isStateChange: true, displayed: true]
        masterDevice.generateEvent(masterResult)
    }
    else if (ks != "0"){
    	masterResult = [name: 'status', value: value, descriptionText: "${childDevice} ${message}", isStateChange: true, displayed: true]
    	masterDevice.generateEvent(masterResult)
    }
    
    if (sz >= 5) note('Zone',"${childDevice} ${message}")
    return [error: false, return_value: 1]    
}

//*************************** outgoing commands ***************************

//turn on/off zones to cloud
void zoneOnOff(dni, onoff, level) {
    log.debug "Cloud zoneOnOff ${dni} ${onoff} ${level}"
   	//def zone = 2
    String zone_dni = "${dni}"
    def zone = zone_dni[-2..-1]
    
    log.debug zone

    def POSTparams = [
        uri: 'https://api.spruceirrigation.com/v2/zone',
        headers: [ 	"Authorization": "Bearer ${atomicState.authToken}"],
        body: [
            zone: zone,
            zonestate: onoff,
            zonetime: level*60
        ]
    ]

    sendPost(POSTparams)
    
}

void scheduleOnOff(name, onoff){
	def OnOff = 'stopped'
    if (onoff.toInteger() == 1) OnOff = 'started'
    def message = "Schedule ${name} ${OnOff}"
    log.debug "scheduleOnOff ${name} ${OnOff}"
    
    note('Schedule',message)
    
    def tempManualMap = atomicState.manualMap
    def scheduleID
    tempManualMap.each{
    	if ("${tempManualMap[it.key]['name']}" == "${name}") scheduleID = "${tempManualMap[it.key]['scheduleid']}"
	}
    
    def POSTparams = [
                    uri: 'https://api.spruceirrigation.com/v2/schedule',
                    headers: [ 	"Authorization": "Bearer ${atomicState.authToken}"],
                    body: [
                        scheduleID: scheduleID,
                        onoff: onoff
                    ]
                ]

    sendPost(POSTparams)

}

void runAll(runtime){
	def POSTparams = [
                    uri: 'https://api.spruceirrigation.com/v2/runall',
                    headers: [ 	"Authorization": "Bearer ${atomicState.authToken}"],
                    body: [
                        zonetime: runtime
                    ]
                ]

    sendPost(POSTparams)
}

void send_pause(pausetime){
	if (pausetime != 0) pausetime = 0;
	def POSTparams = [
                    uri: 'https://api.spruceirrigation.com/v2/pause',
                    headers: [ 	"Authorization": "Bearer ${atomicState.authToken}"],
                    body: [
                        pausetime: pausetime*60
                    ]
                ]

    sendPost(POSTparams)
}

void send_resume(){
	def POSTparams = [
                    uri: 'https://api.spruceirrigation.com/v2/resume',
                    headers: [ 	"Authorization": "Bearer ${atomicState.authToken}"],
                    body: [                        
                    ]
                ]

    sendPost(POSTparams)
}

void send_stop(){	
	def POSTparams = [
                    uri: 'https://api.spruceirrigation.com/v2/stop',
                    headers: [ 	"Authorization": "Bearer ${atomicState.authToken}"],
                    body: [                        
                    ]
                ]

    sendPost(POSTparams)
}


//************* notifications to device, pushed if requested ******************
def note(type, message){
	//no error notifications?
	log.debug "note ${type} ${message}"
	if(settings.notifications && settings.notifications.contains(type)) sendPush "${message}"
}

//************* post ******************

def sendPost(POSTparams) {
	try{
        httpPost(POSTparams){
            resp -> //resp.data {
            log.debug "${resp.data}"
            if ("${resp.data.error}" == 'true') note('error', "${resp.data.message}")
        }                
    } 
    catch (error) {
        log.debug "send DB error: $error"        
        refreshAuthToken()
        retryInitialRequest(POSTparams)
    }
   	
}

def retryInitialRequest(POSTparams) {
	POSTparams.headers = [ 	"Authorization": "Bearer ${atomicState.authToken}"]
    try{
        httpPost(POSTparams){
            resp -> //resp.data {
            log.debug "${resp.data}"               
        }                
    } 
    catch (error) {
        log.debug "send DB error: $error"
        
    }
}

//********************** OAUTH ***************************/

def oauthInitUrl(){
	// Generate a random ID to use as a our state value. This value will be used to verify the response we get back from the third-party service.
    atomicState.oauthInitState = UUID.randomUUID().toString()
    //log.debug atomicState.oauthInitState
	def oauthParams = [
        response_type: "code",
        scope: "basic",
        client_id: atomicState.clientid,
        client_secret: atomicState.clientSecret,
        state: atomicState.oauthInitState,
        redirect_uri: "https://graph.api.smartthings.com/oauth/callback"
    ]
	def apiEndpoint = "https://app.spruceirrigation.com/oauth"
    redirect(location: "${apiEndpoint}/authorize?${toQueryString(oauthParams)}")
    
}

// The toQueryString implementation simply gathers everything in the passed in map and converts them to a string joined with the "&" character.
String toQueryString(Map m) {
        return m.collect { k, v -> "${k}=${URLEncoder.encode(v.toString())}" }.sort().join("&")
}

def callback() {
    log.debug "callback()>> params: $params, params.code ${params.code}"

    def code = params.code
    def oauthState = params.state

    // Validate the response from the third party by making sure oauthState == state.oauthInitState as expected
    if (oauthState == atomicState.oauthInitState){
        def tokenParams = [
            uri: "https://app.spruceirrigation.com/oauth/token",        
            body: [
                    grant_type: "authorization_code",
                    code      : code,
                    scope: "basic",            
                    client_id : atomicState.clientid,
                    client_secret: atomicState.clientSecret,
                    redirect_uri: "https://graph.api.smartthings.com/oauth/callback"        
                ]
        ]
        
        httpPost(tokenParams) { resp ->
        	atomicState.refreshToken = resp.data.refresh_token
            atomicState.authToken = resp.data.access_token           
        }
        
        //send access_token to spruce
        if (atomicState.authToken && !atomicState.accessTokenPut) {
        	atomicState.accessTokenPut = true
            def accessToken_url = "https://api.spruceirrigation.com/smartthings/accesstoken"
            log.debug accessToken_url
            
            def accessParams = [
            	uri: accessToken_url,
                headers: [ 	"Authorization": "Bearer ${atomicState.authToken}"],
                body: [
                		smartthings_token: atomicState.accessToken
                ]
            ]            
            httpPost(accessParams) { resp ->
            	log.debug resp.data.error
                if (resp.data.error == false) success()
                else fail()
            }           
            
        } 
        else fail()
    } 
    else log.error "callback() failed. Validation of state did not match. oauthState != atomicState.oauthInitState"
    
}

private refreshAuthToken() {
    def refreshParams = [        
        uri: "https://app.spruceirrigation.com/oauth/token",
        body: [grant_type: "refresh_token", refresh_token: atomicState.refreshToken, client_id: atomicState.clientid, client_secret: atomicState.clientSecret]
    ]
    try{
        def jsonMap
        httpPost(refreshParams) { resp ->
            log.debug resp.data
            if(resp.status == 200)
            {
                jsonMap = resp.data
                if (resp.data) {
                    atomicState.refreshToken = resp.data.refresh_token
                    atomicState.authToken = resp.data.access_token
            	}
                
        	}
    	}
    }
    catch (error) {
        log.debug "token refresh error: $error"
        return false
    }
    return true
}

// Example success method
def success() {
        def message = """
                <h2>Your account is now connected to SmartThings!</h2>
                <h2>Click 'Done' to finish setup.</h2>
        """
        displayMessageAsHtml(message)
}

// Example fail method
def fail() {
    def message = """
        <h2>There was an error connecting your account with SmartThings</h2>
        <h2>Please try again.</h2>
    """
    displayMessageAsHtml(message)
}

def displayMessageAsHtml(message) {
    def html = """
        <!DOCTYPE html>
        <html>
            <head>
            </head>
            <body>            	
                <div>                
                    ${message}
                </div>
            </body>
        </html>
    """
    
    render contentType: 'text/html', data: html
}