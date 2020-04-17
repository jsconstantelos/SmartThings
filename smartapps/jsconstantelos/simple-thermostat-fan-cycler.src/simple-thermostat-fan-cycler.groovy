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
    singleInstance: true,
    namespace: "jsconstantelos",
    author: "jscgs350",
    description: "This app does just one thing and that's to turn on and off your thermostat's fan based upon your settings.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    page(name: "mainPage", title: "Simple Thermostat Fan Cycler", install: true, uninstall: true,submitOnChange: true) {
            section {
                    app(name: "childRules", appName: "Simple Thermostat Fan Cycler Child", namespace: "jsconstantelos", title: "Create a new fan rule...", multiple: true)
            }
    }
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
}

def initialize() {
    childApps.each {child ->
            log.info "Installed Fan Rules: ${child.label}"
    }
}