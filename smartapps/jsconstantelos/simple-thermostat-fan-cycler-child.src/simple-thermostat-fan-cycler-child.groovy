definition(
    name: "Simple Thermostat Fan Cycler Child",
    namespace: "jsconstantelos",
    author: "jscgs350",
    description: "This app does just one thing and that's to turn on and off your thermostat's fan based upon your settings.",
    category: "My Apps",
    parent: "jsconstantelos:Simple Thermostat Fan Cycler",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    section("Use these thermostats") {
        input "thermostats", "capability.thermostat", title: "Which thermos?", multiple:true
    }
    section("Set ON and OFF minutes") {
        input "onMinutes", "number", title: "Stay on for how many minutes?"
        input "offMinutes", "number", title: "Stay off for how many minutes?"
    }
}

def installed() {
	log.debug "Installing.  Settings: ${settings}"
    initialize()
}

def updated() {
	log.debug "Updated..  Settings: ${settings}"
	unsubscribe()
    unschedule()
	initialize()
}

def initialize() {
	log.debug "Initializing..."
    onCycleTimer()
}

def onCycleTimer() {
	log.debug "Setting fan(s) to ON and scheduing OFF/AUTO timer..."
	runIn(onMinutes * 60, offCycleTimer)
	thermostats?.fanOn()
}

def offCycleTimer() {
	log.debug "Setting fan(s) to AUTO and scheduing ON timer..."
    runIn(offMinutes * 60, onCycleTimer)
    thermostats?.fanAuto()
}