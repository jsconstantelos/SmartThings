definition(
    name: "Simulated Presence Manager",
    namespace: "jsconstantelos",
    author: "jsconstantelos",
    description: "Will set states for a simulated presence sensor based upon the states of another mobile presence sensor.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Select Presence Sensor Group") {
		input "presenceSensors", "capability.presenceSensor", title: "Presence Sensors", multiple: false, required: true
        input "simulatedPresence", "capability.presenceSensor", title: "Simulated Presence Sensor", multiple: false, required: true
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	setPresence()
	subscribe(presenceSensors, "presence", "presenceHandler")
}

def presenceHandler(evt) {
	setPresence()
}

def setPresence(){
	def presentCounter = 0
    
    presenceSensors.each {
    	if (it.currentValue("presence") == "present") {
        	presentCounter++
        }
    }
    
    log.debug("presentCounter: ${presentCounter}, simulatedPresence: ${simulatedPresence.currentValue("presence")}")
    
    if (presentCounter > 0) {
    	if (simulatedPresence.currentValue("presence") != "present") {
    		simulatedPresence.arrived()
            log.debug("Arrived")
        }
    } else {
    	if (simulatedPresence.currentValue("presence") != "not present") {
    		simulatedPresence.departed()
            log.debug("Departed")
        }
    }
}