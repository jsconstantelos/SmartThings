metadata {
	definition (name: "Water Heater Mode Selector", namespace: "smartthings", author: "Reid Keith") {
		capability "Actuator"
		capability "Switch"
		capability "Refresh"
		capability "Sensor"
        
        attribute "vacationState", "string"
        attribute "hybridState", "string"
        attribute "deviceSummary", "string"
        
        command "eco"
        command "highdemand"
        command "vacation"
        command "hybrid"
        command "modesoff"
	}

    tiles(scale: 2) {
		standardTile("toggle", "device.switch", inactiveLabel: false, decoration: "flat", width: 6, height: 3) {
			state("onECO", label:"ECO", action:"highdemand", icon:"st.Outdoor.outdoor2", backgroundColor:"#a8d170")
			state("onHD", label:"High\nDemand", action:"eco", icon:"st.thermostat.heat", backgroundColor:"#FF0000")
            state("toggleOFF", label:"ECO/HIGH\nOFF", action:"eco", icon:"st.thermostat.heating-cooling-off", backgroundColor:"#cccccc")
            state("allOFF", label:"All Off", action:"eco", icon:"st.thermostat.heating-cooling-off", backgroundColor:"#cccccc")
		}
		standardTile("toggleSmallView", "device.deviceSummary", inactiveLabel: false, decoration: "flat", width: 6, height: 3) {
			state("onECO", label:"ECO", icon:"st.Outdoor.outdoor2", backgroundColor:"#a8d170")
			state("onHD", label:"High Demand", icon:"st.thermostat.heat", backgroundColor:"#FF0000")
            state("onVAC", label:"Vacation", icon:"st.Weather.weather3", backgroundColor:"#a8d170")
            state("onHYB", label:"Hybrid", icon:"st.tesla.tesla-hvac", backgroundColor:"#a8d170")
            state("allOFF", label:"All Off", icon:"st.thermostat.heating-cooling-off", backgroundColor:"#cccccc")
		}
		standardTile("vacation", "device.vacationState", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state("on", label:"Vacation\nOn", action:"vacation", icon:"st.Weather.weather3", backgroundColor:"#a8d170")
            state("off", label:"Vacation Off", action:"vacation", icon:"st.Weather.weather3")
		}
		standardTile("hybrid", "device.hybridState", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state("on", label:"Hybrid\nOn", action:"hybrid", icon:"st.tesla.tesla-hvac", backgroundColor:"#a8d170")
            state("off", label:"Hybrid Off", action:"hybrid", icon:"st.tesla.tesla-hvac")
		}
		standardTile("modesOff", "device.modesOff", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state("default", label:"All Off", action:"modesoff", backgroundColor:"#cccccc")
		}
		main "toggleSmallView"
		details(["toggle", "hybrid", "modesOff", "vacation"])
	}
}

def parse(String description) {
	log.trace "parse($description)"
}

def modesoff() {
	log.debug "Tapped on ALL OFF, so toggling all modes to off"
    sendEvent(name: "vacationState", value: "off")
    sendEvent(name: "hybridState", value: "off")
    sendEvent(name: "deviceSummary", value: "allOFF")
    sendEvent(name: "switch", value: "allOFF")
}

def eco() {
	log.debug "Tapped on High Demand to toggle back to ECO, and turning off Vacation and Hybrid"
    sendEvent(name: "vacationState", value: "off")
    sendEvent(name: "hybridState", value: "off")
    sendEvent(name: "deviceSummary", value: "onECO")
    sendEvent(name: "switch", value: "onECO")
}

def highdemand() {
	log.debug "Tapped on ECO to toggle back to High Demand, and turning off Vacation and Hybrid"
    sendEvent(name: "vacationState", value: "off")
    sendEvent(name: "hybridState", value: "off")
    sendEvent(name: "deviceSummary", value: "onHD")
    sendEvent(name: "switch", value: "onHD")
}

def vacation() {
    if (device.currentState("vacationState")?.value == "on") {
    	log.debug "Tapped on Vacation, so toggling from on to off"
		sendEvent(name: "vacationState", value: "off")
    } else {
    	log.debug "Tapped on Vacation, so toggling from off to on, and turning off all other modes"
        sendEvent(name: "deviceSummary", value: "onVAC")
        sendEvent(name: "switch", value: "toggleOFF")
        sendEvent(name: "hybridState", value: "off")
        sendEvent(name: "vacationState", value: "on")
    }
}

def hybrid() {
    if (device.currentState("hybridState")?.value == "on") {
    	log.debug "Tapped on Hybrid, so toggling from on to off"
		sendEvent(name: "hybridState", value: "off")
    } else {
    	log.debug "Tapped on Hybrid, so toggling from off to on, and turning off all other modes"
        sendEvent(name: "deviceSummary", value: "onHYB")
        sendEvent(name: "switch", value: "toggleOFF")
        sendEvent(name: "vacationState", value: "off")
        sendEvent(name: "hybridState", value: "on")
    }
}