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
	page(name: "page2", title: "Plugin version 1.5\nSelect device and rules", install: true, uninstall: true)
}

def page2() {
    dynamicPage(name: "page2") {
        section("Choose a water meter to monitor:") {
            input(name: "meter", type: "capability.energyMeter", title: "Water Meter", description: null, required: true, submitOnChange: true)
        }
        if (meter) {
            section {
                app(name: "childRules", appName: "FortrezZ Leak Detector Child", namespace: "jsconstantelos", title: "Create New Rule...", multiple: true)
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
    state.cumulativeBaseline = meter.latestValue("cumulative")	// used by Continuous Flow (Gallons and time based) rule
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
    state.cumulativeBaseline = meter.latestValue("cumulative")	// used by Continuous Flow (Gallons and time based) rule
	unsubscribe()
	initialize()
}

def initialize() {
	log.debug("Subscribing to events...")
	subscribe(meter, "cumulative", cumulativeHandler)
	subscribe(meter, "gpm", gpmHandler)
}

def cumulativeHandler(evt) {
   	def daysOfTheWeek = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"]
    def today = new Date()
    today.clearTime()
    Calendar c = Calendar.getInstance();
    c.setTime(today);
    int dow = c.get(Calendar.DAY_OF_WEEK);
    def dowName = daysOfTheWeek[dow-1]
    def cumulative = new BigDecimal(evt.value)
    def rules = state.rules
    rules.each { it ->
        def r = it.rules
        def childAppID = it.id
    	switch (r.type) {

			case "Continuous Flow (Gallons and time based)":
				log.debug("Continuous Flow (Gallons and time based) Evaluation: ${r}")
				def boolTime = timeOfDayIsBetween(r.startTime, r.endTime, new Date(), location.timeZone)
				def boolDay = !r.days || findIn(r.days, dowName) // Truth Table of this mess: http://swiftlet.technology/wp-content/uploads/2016/05/IMG_20160523_150600.jpg
				def boolMode = !r.modes || findIn(r.modes, location.currentMode)
				if (boolTime && boolDay && boolMode) {
                	log.debug "Cumulative value received, and met rule criteria, so let's process data points..."
					def delta = 0
					if(state["startAccumulate${childAppID}"] != null) {
                    	log.debug ("Using previously saved cumulative data point : ${state["startAccumulate${childAppID}"]} and newest data point $cumulative...")
						delta = cumulative - state["startAccumulate${childAppID}"]
					} else {
                    	log.debug ("Using baseline cumulative data point : ${state.cumulativeBaseline} and newest data point $cumulative...")
                        delta = cumulative - state.cumulativeBaseline
					}
					log.debug("Difference from last cumulative data point is ${delta} gallons, so checking to see if a notification needs to be sent...")
					if (delta > r.gallons) {
                    	log.debug("Need to send a notification! Threshold:${r.gallons} gallons, Actual:${delta} gallons...")
                        state["startAccumulate${childAppID}"] = cumulative
						sendNotification(childAppID, r.gallons, delta)
						if(r.dev) {
							def activityApp = getChildById(childAppID)
							activityApp.devAction(r.command)
						}
					} else {
                    	log.debug "Not sending a notification because the threshold of ${r.gallons} gallons hasn't happened, so reset and wait again..."
                        state["startAccumulate${childAppID}"] = cumulative
                    }
				} else {
					log.debug("Outside specified time, saving value...")
					state["startAccumulate${childAppID}"] = cumulative
				}
			break

			case "Total Flow (Gallons since last reset)":
				log.debug("Total Flow (Gallons since last reset) Evaluation: ${r}")
                def boolMode = !r.modes || findIn(r.modes, location.currentMode)
				if (boolMode) {
                	log.debug "Checking to see if a notification needs to be sent..."
					if(cumulative > r.gallons) {
                    	log.debug("Threshold:${r.gallons} gallons, Actual:${cumulative} gallons")
                        log.debug "Need to send a notification!"
						sendNotification(childAppID, r.gallons, cumulative)
						if(r.dev) {
							def activityApp = getChildById(childAppID)
							activityApp.devAction(r.command)
						}
					}
				}
			break

			default:
			break
		}
	}
}

def gpmHandler(evt) {
   	def daysOfTheWeek = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"]
    def today = new Date()
    today.clearTime()
    Calendar c = Calendar.getInstance();
    c.setTime(today);
    int dow = c.get(Calendar.DAY_OF_WEEK);
    def dowName = daysOfTheWeek[dow-1]
    def gpm = new BigDecimal(evt.value)
    def rules = state.rules
    state.cumulativeBaseline = meter.latestValue("cumulative")	// used by accumulated gallons rule
    rules.each { it ->
        def r = it.rules
        def childAppID = it.id
    	switch (r.type) {

			case "Mode (GPM and mode based)":
				log.debug("Mode (GPM and mode based) Evaluation: ${location.currentMode} in ${r.modes}... ${findIn(r.modes, location.currentMode)}")
				if (findIn(r.modes, location.currentMode)) {
                	log.debug "Checking to see if a notification needs to be sent..."
					if(gpm > r.gpm) {
                    	log.debug("Threshold:${r.gpm} gpm, Actual:${gpm} gpm")
                        log.debug "Need to send a notification!"
						sendNotification(childAppID, r.gpm, gpm)
						if(r.dev) {
							def activityApp = getChildById(childAppID)
							activityApp.devAction(r.command)
						}
					}
				}
			break

            case "Time Period (GPM and time based)":
				log.debug("Time Period (GPM and time based) Evaluation: ${r}")
				def boolTime = timeOfDayIsBetween(r.startTime, r.endTime, new Date(), location.timeZone)
				def boolDay = !r.days || findIn(r.days, dowName) // Truth Table of this mess: http://swiftlet.technology/wp-content/uploads/2016/05/IMG_20160523_150600.jpg
				def boolMode = !r.modes || findIn(r.modes, location.currentMode)
				if(boolTime && boolDay && boolMode) {
                	log.debug "Checking to see if a notification needs to be sent..."
					if(gpm > r.gpm) {
                    	log.debug("Threshold:${r.gpm} gpm, Actual:${gpm} gpm")
                        log.debug "Need to send a notification!"
						sendNotification(childAppID, r.gpm, gpm)
						if(r.dev) {
							def activityApp = getChildById(childAppID)
							activityApp.devAction(r.command)
						}
					}
				}
			break

			case "Continuous Flow (GPM over time)":
            	log.debug("Continuous Flow (GPM over time) Evaluation: ${r}")
				def boolMode = !r.modes || findIn(r.modes, location.currentMode)
				if (evt.value == '0') {
					log.debug "Flow stopped, so clean up and get ready for another evaluation when flow starts again..."
					state["startTime${childAppID}"] = 0
				} else { 
					if (state["startTime${childAppID}"] != null) {
                    	if (state["startTime${childAppID}"] == 0) {
                            log.debug "Start monitoring GPM for continuous flow, so set up important variables..."
                            state["startTime${childAppID}"] = now()
                        } else {
                        	log.debug "Monitoring GPM for continuous flow (flow value detected: ${evt.value})..."
                        }
                    } else {
                        log.debug "Start monitoring GPM for continuous flow, so set up important variables..."
                        state["startTime${childAppID}"] = now()
                    }
					def timeDelta = (now() - state["startTime${childAppID}"])/60000
                    log.debug "Checking to see if a notification needs to be sent..."
                    if (timeDelta > r.flowMinutes && boolMode) {
                        log.debug("Threshold:${r.flowMinutes} minutes, Actual:${timeDelta} minutes")
                        log.debug "Need to send a notification!"
                        sendNotification(childAppID, r.flowMinutes, timeDelta)
                        if (r.dev) {
                            def activityApp = getChildById(childAppID)
                            activityApp.devAction(r.command)
                        }
                    }
                }
			break

			case "Water Valve Status (GPM and valve state)":
				log.debug("Water Valve Status (GPM and valve state) Evaluation: ${r}")
				def child = getChildById(childAppID)
				if(child.isValveStatus(r.valveStatus)) {
                	log.debug "Checking to see if a notification needs to be sent..."
					if(gpm > r.gpm) {
                    	log.debug("Threshold:${r.gpm} gpm, Actual:${gpm} gpm")
                        log.debug "Need to send a notification!"
						sendNotification(childAppID, r.gpm, gpm)
					}
				}
			break

			default:
			break
        }
	}
}

def sendNotification(device, gpm, actual) {
	def set = getChildById(device).settings()
	def msg = ""
    if (set.type == "Continuous Flow (Gallons and time based)") {
    	msg = "Water Meter Warning: \"${set.ruleName}\" is over gallons exceeded threshold of ${gpm} gallons, actual is ${actual} gallons"
    } else if (set.type == "Continuous Flow (GPM over time)") {
    	msg = "Water Meter Warning: \"${set.ruleName}\" is over the constant flow threshold of ${gpm} minutes, actual is ${actual} minutes"
    } else if (set.type == "Total Flow (Gallons since last reset)") {
    	msg = "Water Meter Warning: \"${set.ruleName}\" is over gallons exceeded threshold of ${gpm} gallons since last meter reset, actual is ${actual} gallons"
    } else {
    	msg = "Water Meter Warning: \"${set.ruleName}\" is over GPM exceeded threshold of ${gpm}gpm, actual is ${actual} gpm"
    }
    log.debug(msg)
    // Only send notifications as often as the user specifies
    def lastNotification = 0
    if(state["notificationHistory${device}"]) {
    	lastNotification = Date.parse("yyyy-MM-dd'T'HH:mm:ssZ", state["notificationHistory${device}"]).getTime()
    }
    def td = now() - lastNotification
    if(td/(60*1000) > minutesBetweenNotifications.value) {
        if (pushNotification) {
        	log.debug("Sending PUSH notification")
            sendPush(msg)
            state["notificationHistory${device}"] = new Date()
        }
        if (smsNotification) {
        	log.debug("Sending SMS notification")
            sendSms(phone, msg)
            state["notificationHistory${device}"] = new Date()
        }
    } else {
    	log.debug "NOT sending notification because we haven't waited long enough (${minutesBetweenNotifications.value} minutes) since the last notifiction was sent..."
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