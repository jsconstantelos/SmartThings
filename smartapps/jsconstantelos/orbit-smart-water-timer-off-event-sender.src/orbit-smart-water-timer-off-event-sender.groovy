definition(
    name: "Orbit Smart Water Timer OFF Event Sender",
    namespace: "jsconstantelos",
    author: "jsconstantelos",
    description: "Sends OFF status events for the Orbit Water Timer",
    category: "My Apps",
    parent: "jsconstantelos:Orbit Smart Water Timer Manager",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
    section("Select the Orbit timer") {
      input "timerdevice", "capability.switch", multiple: false, required: true
    }
}

def installed() {
    log.debug "Installed with preferences: ${settings}"
    initialize()
}

def updated() {
    log.debug "Updated with preferences: ${settings}"
    unsubscribe()
    initialize()
}

def initialize() {
	log.debug "Subscribe to timer events..."
    subscribe(timerdevice, "switch", offHandler)
}

def offHandler(evt) {
	log.debug "Timer was turned ${evt.value}..."
	if (evt.value == "on") {
    	log.debug "Scheduling OFF event to run in 10 minutes..."
        runIn(600, sendOffEvent, [overwrite: true])  // schedule and overwrite any pending schedules
		// timerdevice.sendOffEvent() // This is only for debugging needs
	}  
	if (evt.value == "off") {
    	log.debug "Timer turned off so removing the schedule if there was one..."
		unschedule()
	}  
}

def sendOffEvent() {
	log.debug "10 minutes has passed so sending OFF events to the timer..."
	timerdevice.sendOffEvent()
}