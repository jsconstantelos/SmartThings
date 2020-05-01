definition(
    name: "Alerts - Left it On, Open, or Unlocked Parent",
    singleInstance: true,
    namespace: "jsconstantelos",
    author: "jsconstantelos",
    description: "Alerts - Left it On, Open, or Unlocked",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    page(name: "mainPage", title: "Alerts - Left it On, Open, or Unlocked Parent", install: true, uninstall: true,submitOnChange: true) {
		section {app(name: "childRules", appName: "Alerts - Left It On Child", namespace: "jsconstantelos", title: "Create alert for something left on...", multiple: true)}
        section {app(name: "childRules", appName: "Alerts - Left It Open Child", namespace: "jsconstantelos", title: "Create alert for something left open...", multiple: true)}
        section {app(name: "childRules", appName: "Alerts - Left It Unlocked Child", namespace: "jsconstantelos", title: "Create alert for something left unlocked...", multiple: true)}
        section {app(name: "childRules", appName: "Alerts - Left It Running Child", namespace: "jsconstantelos", title: "Create alert for something left running...", multiple: true)}
        section {app(name: "childRules", appName: "Alerts - Thermostat Mode Monitor Child", namespace: "jsconstantelos", title: "Create alert for a stuck thermostat mode...", multiple: true)}
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