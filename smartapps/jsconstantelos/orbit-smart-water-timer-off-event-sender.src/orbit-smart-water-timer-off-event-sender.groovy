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
      input "timer", "capability.switch", multiple: false, required: true
    }
}

def installed() {
    log.debug "Installed with preferences: ${settings}"
    initialize()
}

def updated() {
    log.debug "Updated with preferences: ${settings}"
    unschedule()
    initialize()
}

def initialize() {
	log.debug "Scheduling the water timer per user preferences..."
    schedule(timerTime, timerSchedule)
}

def timerSchedule() {
    log.debug "Running timerSchedule..."
	def turnOnTimer = false
    Calendar timerCalendar = Calendar.getInstance(location.timeZone)
    int currentDayOfWeek = timerCalendar.get(Calendar.DAY_OF_WEEK) // 1= Sunday...7=Saturday
	if (timerDays.contains('Sunday') && currentDayOfWeek == 1) {
		turnOnTimer = true
    } else if (timerDays.contains('Monday') && currentDayOfWeek == 2) {
    	turnOnTimer = true
    } else if (timerDays.contains('Tuesday') && currentDayOfWeek == 3) {
    	turnOnTimer = true
    } else if (timerDays.contains('Wednesday') && currentDayOfWeek == 4) {
    	turnOnTimer = true
    } else if (timerDays.contains('Thursday') && currentDayOfWeek == 5) {
    	turnOnTimer = true
    } else if (timerDays.contains('Friday') && currentDayOfWeek == 6) {
    	turnOnTimer = true
    } else if (timerDays.contains('Saturday') && currentDayOfWeek == 7) {
    	turnOnTimer = true
    }
    if(turnOnTimer == true){
        log.debug "Turning on the water timer per schedule..."
        settings.timer.on()
        sendMessage()
    }
    else {
        log.debug "Today is not on the schedule to turn on the water timer..."
    }
}