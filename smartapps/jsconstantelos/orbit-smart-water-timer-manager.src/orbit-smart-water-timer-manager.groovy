definition(
    name: "Orbit Smart Water Timer Manager",
    singleInstance: true,
    namespace: "jsconstantelos",
    author: "jsconstantelos",
    description: "Orbit Smart Water Timer Manager",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    page(name: "mainPage", title: "Orbit Smart Water Timer Manager", install: true, uninstall: true,submitOnChange: true) {
	section {app(name: "childRules", appName: "Orbit Smart Water Timer Run Scheduler", namespace: "jsconstantelos", title: "Create schedule for a timer...", multiple: true)}
    section {app(name: "childRules", appName: "Orbit Smart Water Timer OFF Event Sender", namespace: "jsconstantelos", title: "Force OFF events...", multiple: true)}
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
            log.info "Installed schedules: ${child.label}"
    }
}