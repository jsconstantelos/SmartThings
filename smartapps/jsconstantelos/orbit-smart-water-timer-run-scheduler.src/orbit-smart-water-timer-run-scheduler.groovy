definition(
    name: "Orbit Smart Water Timer Run Scheduler",
    namespace: "jsconstantelos",
    author: "jsconstantelos",
    description: "Create a schedule for a, Orbit water timer valve",
    category: "My Apps",
    parent: "jsconstantelos:Orbit Smart Water Timer Manager",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
    section("Select the Orbit timer") {
      input "timer", "capability.switch", multiple: false, required: true
    }
    section("Define a schedule") {
        input "timerTime", "time", title: "Select start time", description: "Select the start time (runs for 10 minutes by Orbit's default design)", required: true
        input "timerDays", "enum", title: "Select day(s) of week",description: "Select the day(s) to turn on", options: ['Monday','Tuesday','Wednesday','Thursday','Friday','Saturday','Sunday'], required: true, multiple: true
    }
}

def installed() {
    log.debug "Installed with schedule preferences: ${settings}"
    initialize()
}

def updated() {
    log.debug "Updated with schedule preferences: ${settings}"
    unschedule()
    initialize()
}

def initialize() {
	log.debug "Scheduling the water timer per user preferences..."
    schedule(timerTime, timerSchedule)
//	timerSchedule()  // remove comments for debugging only
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

void sendMessage() {
  def msg = "${timer.displayName} is now running for 10 minutes per your schedule."
  sendPush msg
}