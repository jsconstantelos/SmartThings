definition(
    name: "Virtual Thermostat Parent App",
    singleInstance: true,
    namespace: "jsconstantelos",
    author: "jscgs350",
    description: "Virtual Thermostat Parent App",
    category: "My Apps",
    iconUrl: "https://graph.api.smartthings.com/api/devices/icons/st.Weather.weather9-icn",
    iconX2Url: "https://graph.api.smartthings.com/api/devices/icons/st.Weather.weather9-icn?displaySize=2x")

preferences {
    page(name: "mainPage", title: "Virtual Thermostat Parent App", install: true, uninstall: true,submitOnChange: true) {
            section {
                    app(name: "childRules", appName: "Virtual Thermostat Child App", namespace: "jsconstantelos", title: "Create Virtual Thermostat...", multiple: true)
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
            log.info "Installed Monitors: ${child.label}"
    }
}