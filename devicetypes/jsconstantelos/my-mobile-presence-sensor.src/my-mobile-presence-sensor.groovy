/**
 *  Copyright 2014 SmartThings
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
 */
metadata {
    definition (name: "My Mobile Presence Sensor", namespace: "jsconstantelos", author: "jsconstantelos", mnmn: "SmartThingsCommunity", vid: "9ebd7d4a-c807-366d-9244-5b5cc74ebe1f") {
		capability "Presence Sensor"
		capability "Occupancy Sensor"
		capability "Sensor"
        command "arrived"
        command "departed"
    }

    tiles {
        standardTile("presence", "device.presence", width: 2, height: 2, canChangeBackground: true) {
            state("not present", label:'not present', icon:"st.presence.tile.not-present", backgroundColor:"#ffffff", action:"arrived")
            state("present", label:'present', icon:"st.presence.tile.present", backgroundColor:"#00A0DC", action:"departed")
        }
        main "presence"
        details "presence"
    }
}

def parse(String description) {
    def pair = description.split(":")
    createEvent(name: pair[0].trim(), value: pair[1].trim())
}

def installed() {
    initialize()
}

def updated() {
    initialize()
}

def initialize() {
    log.debug "Initializing..."
    sendEvent(name: "presence", value: "unknown")
    sendEvent(name: "occupancy", value: "unknown")
}

// handle commands
def arrived() {
    log.trace "Executing 'arrived'"
    sendEvent(name: "presence", value: "present")
    sendEvent(name: "occupancy", value: "occupied")
}

def departed() {
    log.trace "Executing 'departed'"
    sendEvent(name: "presence", value: "not present")
    sendEvent(name: "occupancy", value: "unoccupied")
}