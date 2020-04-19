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
    section("Only for this mode") {
        input "activeMode", "enum", title: "Which mode?", multiple:false, 
        metadata:[values:["Home","Away","Night"]]
    }
}

def installed() {
	log.debug "Installing.  Settings: ${settings}"
    initialize()
}

def updated() {
	log.debug "Updated..  Settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
	log.debug "Initializing..."
	subscribe(location, changedLocationMode)
    onCycleTimer()
}

def changedLocationMode(evt) {
	log.debug "changedLocationMode called..."
	onCycleTimer()
}

def onCycleTimer() {
	log.debug "onCycleTimer called..."
	log.debug "Check to see if modes match before turning on fan(s)..."
    def curMode = location.currentMode
    log.debug "Checking current mode: ${curMode}..."
	if (curMode == activeMode) {
    	log.debug "Setting fan(s) to ON and scheduing OFF/AUTO timer because the modes matched..."
		runIn(onMinutes * 60, offCycleTimer)
        thermostats?.fanOn()
    } else {
        log.debug "We're not scheduling anything because the modes didn't match so waiting until a mode change..."
	}
}

def offCycleTimer() {
	log.debug "offCycleTimer called..."
	log.debug "Setting fan(s) to AUTO and scheduing ON timer..."
    runIn(offMinutes * 60, onCycleTimer)
    thermostats?.fanAuto()
}