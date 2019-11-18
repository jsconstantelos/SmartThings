definition(
    name: "Humidity Monitor Parent App",
    singleInstance: true,
    namespace: "jsconstantelos",
    author: "jscgs350",
    description: "Humidity Monitor Parent App",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    page(name: "mainPage", title: "Humidity Monitor Parent App", install: true, uninstall: true,submitOnChange: true) {
            section {
                    app(name: "childRules", appName: "Humidity Monitor Child App", namespace: "jsconstantelos", title: "Create Humidity Monitor...", multiple: true)
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