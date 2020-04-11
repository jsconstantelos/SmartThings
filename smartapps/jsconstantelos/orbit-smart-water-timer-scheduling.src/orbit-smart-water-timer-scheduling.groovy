definition(
    name: "Orbit Smart Water Timer Scheduling",
    singleInstance: true,
    namespace: "jsconstantelos",
    author: "jsconstantelos",
    description: "Orbit Smart Water Timer Scheduling",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    page(name: "mainPage", title: "Orbit Smart Water Timer Scheduling", install: true, uninstall: true,submitOnChange: true) {
	section {app(name: "childRules", appName: "Orbit Smart Water Timer Scheduling Child", namespace: "jsconstantelos", title: "Create schedule for a timer...", multiple: true)}
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