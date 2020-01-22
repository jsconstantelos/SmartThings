/**
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
 *  Left It On Child
 *
 */
definition(
    name: "Alerts - Left It On Child",
    namespace: "jsconstantelos",
    author: "SmartThings",
    description: "Notifies you when you have left something on longer that a specified amount of time.",
    category: "My Apps",
    parent: "jsconstantelos:Alerts - Left it On, Open, or Unlocked Parent",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {

  section("Monitor this switch for being ON too long") {
    input "contact", "capability.switch"
  }

  section("And notify me if it's ON for more than this many minutes (default 10)") {
    input "onThreshold", "number", description: "Number of minutes", required: false
  }

  section("Delay between notifications (default 10 minutes") {
    input "frequency", "number", title: "Number of minutes", description: "", required: false
  }

  section("Via text message at this number (or via push notification if not specified") {
    input("recipients", "contact", title: "Send notifications to") {
      input "phone", "phone", title: "Phone number (optional)", required: false
    }
  }
}

def installed() {
  log.trace "installed()"
  subscribe()
}

def updated() {
  log.trace "updated()"
  unsubscribe()
  subscribe()
}

def subscribe() {
  subscribe(contact, "switch.on", switchOn)
  subscribe(contact, "switch.off", switchOff)
}

def switchOn(evt) {
  log.trace "switchOn($evt.name: $evt.value)"
  def delay = (onThreshold != null && onThreshold != "") ? onThreshold * 60 : 600
  runIn(delay, switchOnTooLong, [overwrite: true])
}

def switchOff(evt) {
  log.trace "switchOff($evt.name: $evt.value)"
  unschedule(switchOnTooLong)
}

def switchOnTooLong() {
  def contactState = contact.currentState("switch")
  def freq = (frequency != null && frequency != "") ? frequency * 60 : 600

  if (contactState.value == "on") {
    def elapsed = now() - contactState.rawDateCreated.time
    def threshold = ((onThreshold != null && onThreshold != "") ? onThreshold * 60000 : 60000) - 1000
    if (elapsed >= threshold) {
      log.debug "Switch has stayed ON long enough since last check ($elapsed ms):  calling sendMessage()"
      sendMessage()
      runIn(freq, switchOnTooLong, [overwrite: false])
    } else {
      log.debug "Switch has not stayed ON long enough since last check ($elapsed ms):  doing nothing"
    }
  } else {
    log.warn "switchOnTooLong() called but switch is OFF:  doing nothing"
  }
}

void sendMessage() {
  def minutes = (onThreshold != null && onThreshold != "") ? onThreshold : 10
  def msg = "${contact.displayName} has been left ON for ${minutes} minutes."
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