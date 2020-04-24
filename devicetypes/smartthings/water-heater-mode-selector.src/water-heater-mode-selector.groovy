metadata {
	definition (name: "Water Heater Mode Selector", namespace: "smartthings", author: "Reid Keith") {
		capability "Actuator"
		capability "Switch"
		capability "Refresh"
		capability "Sensor"
        
        attribute "vacationState", "string"
        attribute "hybridState", "string"
        
        command "eco"
        command "highdemand"
        command "vacation"
        command "hybrid"
	}

	tiles {
		standardTile("toggle", "device.switch", inactiveLabel: true, width: 3, height: 3) {
			state("off", label:"ECO", action:"highdemand", icon:"st.Outdoor.outdoor2", backgroundColor:"#33FF57")
			state("on", label:"High Demand", action:"eco", icon:"st.thermostat.heat", backgroundColor:"#FF0000")			
		}
		standardTile("vacation", "device.vacationState", inactiveLabel: false, decoration: "flat") {
			state("on", label:"Vacation On", action:"vacation", icon:"st.Weather.weather3")
            state("off", label:"Vacation Off", action:"vacation", icon:"st.Weather.weather3")
		}
		standardTile("hybrid", "device.hybridState", inactiveLabel: false, decoration: "flat") {
			state("on", label:"Hybrid On", action:"hybrid", icon:"st.tesla.tesla-hvac")
            state("off", label:"Hybrid Off", action:"hybrid", icon:"st.tesla.tesla-hvac")
		}

		main "toggle"
		details(["toggle", "hybrid", "vacation"])
	}
}

def parse(String description) {
	log.trace "parse($description)"
}


def eco() {
	log.debug "Tapped on High Demand to toggle back to ECO, and turning off Vacation and Hybrid"
    sendEvent(name: "vacationState", value: "off")
    sendEvent(name: "hybridState", value: "off")
    sendEvent(name: "switch", value: "off")
}

def highdemand() {
	log.debug "Tapped on ECO to toggle back to High Demand, and turning off Vacation and Hybrid"
    sendEvent(name: "vacationState", value: "off")
    sendEvent(name: "hybridState", value: "off")
    sendEvent(name: "switch", value: "on")
}

def vacation() {
    if (device.currentState("vacationState")?.value == "on") {
    	log.debug "Tapped on Vacation, so toggling from on to off"
		sendEvent(name: "vacationState", value: "off")
    } else {
    	log.debug "Tapped on Vacation, so toggling from off to on, and turning off Hybrid"
        sendEvent(name: "hybridState", value: "off")
        sendEvent(name: "vacationState", value: "on")
    }
}

def hybrid() {
    if (device.currentState("hybridState")?.value == "on") {
    	log.debug "Tapped on Hybrid, so toggling from on to off"
		sendEvent(name: "hybridState", value: "off")
    } else {
    	log.debug "Tapped on Hybrid, so toggling from off to on, and turning off Vacation"
        sendEvent(name: "vacationState", value: "off")
        sendEvent(name: "hybridState", value: "on")
    }
}