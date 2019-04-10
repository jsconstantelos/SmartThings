/**
 *  Simple Thermostat Fan Cycler SmartApp
 *
 *  Copyright 2019 J.Constantelos
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
 *  Revision History
 *  ----------------
 *  04-09-2019 : Initial release
 *
 */

definition(
    name: "Simple Thermostat Fan Cycler",
    namespace: "jsconstantelos",
    author: "jscgs350",
    description: "This app does just one thing and that's to turn on and off your thermostat's fan based upon your settings.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    section("Use these thermostats") {
        input "thermostats", "capability.thermostat", title: "Which thermos?", multiple:true
    }
    section("Set ON and OFF minutes") {
        input "onMinutes", "number", title: "Stay on for how many minutes?"
        input "offMinutes", "number", title: "Stay off for how many minutes?"
    }
}

def installed() {
	log.debug "Installing..."
    initialize()
}

def updated() {
	log.debug "Updated..."
	unsubscribe()
    unschedule()
	initialize()
}

def initialize() {
	log.debug "Initialize..."
    onCycleTimer()
}

def onCycleTimer() {
	log.debug "Setting ON timer..."
    runIn(onMinutes * 60, offCycleTimer)
	thermostats?.fanOn()
}

def offCycleTimer() {
	log.debug "Setting OFF timer..."
    runIn(offMinutes * 60, onCycleTimer)
    thermostats?.fanAuto()
}
