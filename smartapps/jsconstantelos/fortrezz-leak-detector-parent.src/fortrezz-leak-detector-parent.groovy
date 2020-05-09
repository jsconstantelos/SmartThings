/**
 *  Leak Detector for FortrezZ Water Meter
 *
 *  Copyright 2016 Daniel Kurin
 *
 *  Updated by John Constantelos
 */
definition(
    name: "FortrezZ Leak Detector Parent",
    namespace: "jsconstantelos",
    author: "FortrezZ, LLC",
    description: "Use the FortrezZ Water Meter to identify leaks in your home's water system.",
    category: "Green Living",
    iconUrl: "http://swiftlet.technology/wp-content/uploads/2016/05/logo-square-200-1.png",
    iconX2Url: "http://swiftlet.technology/wp-content/uploads/2016/05/logo-square-500.png",
    iconX3Url: "http://swiftlet.technology/wp-content/uploads/2016/05/logo-square.png")

preferences {
	page(name: "page2", title: "Plugin version 1.5\nSelect device and actions", install: true, uninstall: true)
}

def page2() {
    dynamicPage(name: "page2") {
        section("Choose a water meter to monitor:") {
            input(name: "meter", type: "capability.energyMeter", title: "Water Meter", description: null, required: true, submitOnChange: true)
        }
        if (meter) {
            section {
                app(name: "childRules", appName: "FortrezZ Leak Detector Child", namespace: "jsconstantelos", title: "Create New Leak Detector...", multiple: true)
            }
        }
        section("Send notifications through...") {
        	input(name: "pushNotification", type: "bool", title: "SmartThings App", required: false)
        	input(name: "smsNotification", type: "bool", title: "Text Message (SMS)", submitOnChange: true, required: false)
            if (smsNotification)
            {
            	input(name: "phone", type: "phone", title: "Phone number?", required: true)
            }
            input(name: "minutesBetweenNotifications", type: "number", title: "Minutes between notifications", required: true, defaultValue: 60)
        }
		log.debug "there are ${childApps.size()} child smartapps"
        def childRules = []
        childApps.each {child ->
            childRules << [id: child.id, rules: child.settings()]
        }
        state.rules = childRules
        log.debug "Parent Settings: ${settings}"
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
	state.startTime = 0
    state.startAccumulate = 0
	subscribe(meter, "cumulative", cumulativeHandler)
	subscribe(meter, "gpm", gpmHandler)
    log.debug("Subscribing to events and setting state variables...")
}

def cumulativeHandler(evt) {
	log.debug "Dropped into Cummulative handler method (used by Accumulated Gallons rules)..."
   	def daysOfTheWeek = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"]
    def today = new Date()
    today.clearTime()
    Calendar c = Calendar.getInstance();
    c.setTime(today);
    int dow = c.get(Calendar.DAY_OF_WEEK);
    def dowName = daysOfTheWeek[dow-1]
    def cumulative = new BigDecimal(evt.value)
    def rules = state.rules
    state.startTime = 0	//flow stopped when this method kicks off, so clean up GPM state variable
    rules.each { it ->
        def r = it.rules
        def childAppID = it.id
    	switch (r.type) {

			case "Accumulated Gallons":
				log.debug("Accumulated Gallons Evaluation: ${r}")
				def boolTime = timeOfDayIsBetween(r.startTime, r.endTime, new Date(), location.timeZone)
				def boolDay = !r.days || findIn(r.days, dowName) // Truth Table of this mess: http://swiftlet.technology/wp-content/uploads/2016/05/IMG_20160523_150600.jpg
				def boolMode = !r.modes || findIn(r.modes, location.currentMode)
				if (boolTime && boolDay && boolMode) {
                	log.debug "Cumulative value received, and met rule criteria..."
					def delta = 0
					if(state.startAccumulate != 0) {
						delta = cumulative - state.startAccumulate
					} else {
						state.startAccumulate = cumulative
					}
					log.debug("Difference from beginning of time period is ${delta} gallons, so checking to see if a notification needs to be sent...")
					if (delta > r.gallons) {
                    	log.debug("Need to send a notification! Threshold:${r.gallons} gallons, Actual:${delta} gallons")
						sendNotification(childAppID, r.gallons)
						if(r.dev) {
							def activityApp = getChildById(childAppID)
							activityApp.devAction(r.command)
						}
					} else {
                    	log.debug "Not sending a notification because the threshold of ${r.gallons} gallons hasn't happened yet..."
                    }
				} else {
					log.debug("Outside specified time, saving value")
					state.startAccumulate = cumulative
				}
			break

			default:
			break
		}
	}
}

def gpmHandler(evt) {
	log.debug "Dropped into GPM handler method (used by Mode, Water Valve, Continuous Flow, and Time Period rules)..."
   	def daysOfTheWeek = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"]
    def today = new Date()
    today.clearTime()
    Calendar c = Calendar.getInstance();
    c.setTime(today);
    int dow = c.get(Calendar.DAY_OF_WEEK);
    def dowName = daysOfTheWeek[dow-1]
    def gpm = new BigDecimal(evt.value)
    def rules = state.rules
    rules.each { it ->
        def r = it.rules
        def childAppID = it.id
    	switch (r.type) {

			case "Mode":
				log.debug("Mode Evaluation: ${location.currentMode} in ${r.modes}... ${findIn(r.modes, location.currentMode)}")
				if (findIn(r.modes, location.currentMode)) {
                	log.debug "Checking to see if a notification needs to be sent..."
					if(gpm > r.gpm) {
                    	log.debug("Threshold:${r.gpm} gpm, Actual:${gpm} gpm")
                        log.debug "Need to send a notification!"
						sendNotification(childAppID, r.gpm)
						if(r.dev) {
							def activityApp = getChildById(childAppID)
							activityApp.devAction(r.command)
						}
					}
				}
			break

            case "Time Period":
				log.debug("Time Period Evaluation: ${r}")
				def boolTime = timeOfDayIsBetween(r.startTime, r.endTime, new Date(), location.timeZone)
				def boolDay = !r.days || findIn(r.days, dowName) // Truth Table of this mess: http://swiftlet.technology/wp-content/uploads/2016/05/IMG_20160523_150600.jpg
				def boolMode = !r.modes || findIn(r.modes, location.currentMode)
				if(boolTime && boolDay && boolMode) {
                	log.debug "Checking to see if a notification needs to be sent..."
					if(gpm > r.gpm) {
                    	log.debug("Threshold:${r.gpm} gpm, Actual:${gpm} gpm")
                        log.debug "Need to send a notification!"
						sendNotification(childAppID, r.gpm)
						if(r.dev) {
							def activityApp = getChildById(childAppID)
							activityApp.devAction(r.command)
						}
					}
				}
			break

			case "Continuous Flow":
            	log.debug("Continuous Flow Evaluation: ${r}")
				def boolMode = !r.modes || findIn(r.modes, location.currentMode)
				if (evt.value == '0') {
					log.debug "Flow stopped, so clean up and get ready for another evaluation when flow starts again..."
					state.startTime = 0
				} else { 
                    if (state.startTime == 0) {
                        log.debug "Start monitoring GPM for continuous flow, so set up important variables..."
                        state.startTime = now()
                    }
                    def timeDelta = (now() - state.startTime)/60000
                    log.debug "Checking to see if a notification needs to be sent..."
                    if (timeDelta > r.flowMinutes && boolMode) {
                        log.debug("Threshold:${r.flowMinutes} minutes, Actual:${timeDelta} minutes")
                        log.debug "Need to send a notification!"
                        sendNotification(childAppID, Math.round(r.flowMinutes))
                        if (r.dev) {
                            def activityApp = getChildById(childAppID)
                            activityApp.devAction(r.command)
                        }
                    }
                }
			break

			case "Water Valve Status":
				log.debug("Water Valve Evaluation: ${r}")
				def child = getChildById(childAppID)
				if(child.isValveStatus(r.valveStatus)) {
                	log.debug "Checking to see if a notification needs to be sent..."
					if(gpm > r.gpm) {
                    	log.debug("Threshold:${r.gpm} gpm, Actual:${gpm} gpm")
                        log.debug "Need to send a notification!"
						sendNotification(childAppID, r.gpm)
					}
				}
			break

			default:
			break
        }
	}
}

def sendNotification(device, gpm) {
	def set = getChildById(device).settings()
	def msg = ""
    if(set.type == "Accumulated Gallons") {
    	msg = "Water Meter Warning: \"${set.ruleName}\" is over gallons exceeded threshold of ${gpm} gallons"
    } else if(set.type == "Continuous Flow") {
    	msg = "Water Meter Warning: \"${set.ruleName}\" is over the constant flow threshold of ${gpm} minutes"
    } else {
    	msg = "Water Meter Warning: \"${set.ruleName}\" is over GPM exceeded threshold of ${gpm}gpm"
    }
    log.debug(msg)
    // Only send notifications as often as the user specifies
    def lastNotification = 0
    if(state["notificationHistory${device}"]) {
    	lastNotification = Date.parse("yyyy-MM-dd'T'HH:mm:ssZ", state["notificationHistory${device}"]).getTime()
    }
    def td = now() - lastNotification
    if(td/(60*1000) > minutesBetweenNotifications.value) {
    	log.debug("Sending Notification")
        if (pushNotification) {
            sendPush(msg)
            state["notificationHistory${device}"] = new Date()
        }
        if (smsNotification) {
            sendSms(phone, msg)
            state["notificationHistory${device}"] = new Date()
        }
    }
    log.debug("Last Notification at ${state["notificationHistory${device}"]}... ${td/(60*1000)} minutes")
}

def getChildById(app) {
	return childApps.find{ it.id == app }
}

def findIn(haystack, needle) {
	def result = false
	haystack.each { it ->
    	if (needle == it) {
        	result = true
        }
    }
    return result
}