/**
 *  Copyright 2015 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Left It Running Child
 *
 */
definition(
    name: "Alerts - Left It Running Child",
    namespace: "jsconstantelos",
    author: "SmartThings",
    description: "Notifies you when you have left something running longer than a specified amount of time.",
    category: "My Apps",
    parent: "jsconstantelos:Alerts - Left it On, Open, or Unlocked Parent",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
    section("Monitor this Power Meter device") {
      input "meter", "capability.powerMeter"
    }
    section("And notify if it's been running (greater than 0 watts) for more than this many minutes (default 10)") {
      input "runThreshold", "number", description: "Number of minutes", required: false
    }
    section("Repeat notifications every how many minutes (default 15, or 0 to use running minutes above") {
      input "repeatTime", "number", title: "Number of minutes", description: "", required: false
    }
    section("Via text message at this number (or via push notification if not specified") {
      input "phone", "phone", title: "Phone number (optional)", required: false
    }
}

def installed() {
    log.trace "installed()"
    unschedule()
    subscribe()
}

def updated() {
    log.trace "updated()"
    unschedule()
    unsubscribe()
    subscribe()
}

def subscribe() {
	log.trace "Subscribing to ${meter.displayName} metering events."
	subscribe(meter, "power", runningLong)
}

def runningLong(evt) {
//    log.trace "runningLong($evt.name: $evt.value)"
    def runningTime = (runThreshold != null && runThreshold != "") ? runThreshold * 60 : 600
    def powerValue = meter.currentValue("power")
    if (powerValue > 0) {
            log.debug "Power meter is running, scheduling notifications to run if not already scheduled."
            runIn(runningTime, sendMessage, [overwrite: false]) //setting overwrite to false will cause us to use the first scheduled run vs a new schedule x minutes away every time a power value changes.  If we don't, this will never run.
      } else {
            log.debug "Power meter not running anymore, so stopping scheduled notifications."
			unschedule()
      }
    }

def sendMessage() {
    def freq = (repeatTime != null && repeatTime != "") ? repeatTime * 60 : 900
	def minutes = (runThreshold != null && runThreshold != "") ? runThreshold : 10
    def msg = "${meter.displayName} has been running for ${minutes} minutes."
    log.info msg
    if (phone) {
        sendSms phone, msg
    } else {
        sendPush msg
    }
    if (freq == 0) {
	    log.debug "Scheduling repeat notifications to run in ${minutes} minutes."
        // no need to add runIn again because it retains the original schedule.
    } else {
 	    log.debug "Scheduling repeat notifications to run in ${repeatTime} minutes."
        unschedule()
	    runIn(freq, sendMessage, [overwrite: false])
    }
}