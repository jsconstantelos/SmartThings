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
 *  Thermostat Mode Monitor Child
 *
 */
definition(
    name: "Alerts - Thermostat Mode Monitor Child",
    namespace: "jsconstantelos",
    author: "SmartThings",
    description: "Notifies you when your thermostat is been stuck in a certain mode for too long.",
    category: "My Apps",
    parent: "jsconstantelos:Alerts - Left it On, Open, or Unlocked Parent",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {

  section("Monitor this thermostat for a stuck mode") {
    input "thermostat", "capability.thermostat", title: "Which thermostat?", required: true, multiple: false
  }
  
  section("Which operating state to monitor?") {
	input "monitorState", "enum", title: "Which operating state?", options: ['Idle','Cooling','Heating','Fan is Running','Unknown'], required: true, multiple: false
  }

  section("And notify me if this hasn't changed for more than this many minutes (default 10)") {
    input "delayThreshold", "number", description: "Number of minutes", required: false
  }

  section("Frequency between notifications (default 10 minutes") {
    input "frequency", "number", title: "Number of minutes", description: "Number of minutes", required: false
  }

  section("Via text message at this number (or via push notification if not specified") {
    input("recipients", "contact", title: "Send notifications to") {
    input "phone", "phone", title: "Phone number (optional)", required: false
    }
  }
}

def installed() {
  log.debug "installed() with Settings: ${settings}"
  subscribe()
}

def updated() {
  log.debug "updated() with Settings: ${settings}"
  unsubscribe()
  unschedule()
  subscribe()
}

def subscribe() {
  subscribe(thermostat, "thermostatOperatingState", modeTooLong)
}

def modeTooLong(evt) {
  log.debug "Checking to see if mode is stuck..."
  def freq = (frequency != null && frequency != "") ? frequency * 60 : 600
  def delay = (delayThreshold != null && delayThreshold != "") ? delayThreshold * 60 : 600
  def modeState = thermostat.currentState("thermostatOperatingState")
  log.debug "Current mode is $modeState.value, compared to monitor mode $monitorState"
  if (modeState.value == monitorState) {
    def elapsed = now() - modeState.rawDateCreated.time
    if (elapsed >= delay) {
      log.debug "Thermostat has stayed in $modeState.value mode long enough since last check ($elapsed ms vs. $delay ms): calling sendMessage()"
      sendMessage()
      runIn(freq, modeTooLong, [overwrite: true])
    } else {
      log.debug "Thermostat has not stayed in $modeState.value mode long enough since last check ($elapsed ms vs. $delay): doing nothing"
      runIn(delay, modeTooLong, [overwrite: false])
    }
  } else {
    log.debug "modeTooLong() called but doing nothing because current mode is $modeState.value, compared to monitor mode $monitorState"
    unschedule()
  }
}

void sendMessage() {
  def modeState = thermostat.currentState("thermostatOperatingState")
  def minutes = (delayThreshold != null && delayThreshold != "") ? delayThreshold : 10
  def msg = "${thermostat.displayName} has been stuck in $modeState.value for $minutes minutes."
  log.info msg
  if (location.contactBookEnabled) {
    sendNotificationToContacts(msg, recipients)
  } else {
    if (phone) {
      sendSms phone, msg
    } else {
      sendPush msg
    }
  }
}