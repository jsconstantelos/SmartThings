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
 *  Left It Unlocked Child
 *
 *  Author: SmartThings
 *  Date: 2013-05-09
 */
definition(
    name: "Alerts - Left It Unlocked Child",
    namespace: "jsconstantelos",
    author: "SmartThings",
    description: "Notifies you when you have left a lock unlocked longer that a specified amount of time.",
    category: "My Apps",
    parent: "jsconstantelos:Alerts - Left it On, Open, or Unlocked Parent",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {

  section("Monitor this lock") {
    input "contact", "capability.lock"
  }

  section("And notify me if it's unlocked for more than this many minutes (default 10)") {
    input "openThreshold", "number", description: "Number of minutes", required: false
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
  subscribe(contact, "lock.unlocked", doorOpen)
  subscribe(contact, "lock.locked", doorClosed)
}

def doorOpen(evt) {
  log.trace "doorOpen($evt.name: $evt.value)"
  def delay = (openThreshold != null && openThreshold != "") ? openThreshold * 60 : 600
  runIn(delay, doorOpenTooLong, [overwrite: true])
}

def doorClosed(evt) {
  log.trace "doorClosed($evt.name: $evt.value)"
  unschedule(doorOpenTooLong)
}

def doorOpenTooLong() {
  def contactState = contact.currentState("lock")
  def freq = (frequency != null && frequency != "") ? frequency * 60 : 600

  if (contactState.value == "unlocked") {
    def elapsed = now() - contactState.rawDateCreated.time
    def threshold = ((openThreshold != null && openThreshold != "") ? openThreshold * 60000 : 60000) - 1000
    if (elapsed >= threshold) {
      log.debug "Lock has stayed unlocked long enough since last check ($elapsed ms):  calling sendMessage()"
      sendMessage()
      runIn(freq, doorOpenTooLong, [overwrite: false])
    } else {
      log.debug "Lock has not stayed unlocked long enough since last check ($elapsed ms):  doing nothing"
    }
  } else {
    log.warn "doorOpenTooLong() called but lock is locked:  doing nothing"
  }
}

void sendMessage() {
  def minutes = (openThreshold != null && openThreshold != "") ? openThreshold : 10
  def msg = "${contact.displayName} has been left unlocked for ${minutes} minutes."
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