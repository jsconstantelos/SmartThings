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
 *  Aeon Home Energy Meter
 *
 *  Author: SmartThings
 *
 *  Date: 2013-05-30
 *
 *	Updates:
 *	-------
 *	07-09-2019 : Initial commit.  Used ST's default handler and added parameters to adjust report type and intervals.
 */
metadata {
	definition (name: "My Aeon Gen1 Home Energy Meter", namespace: "jsconstantelos", author: "SmartThings", runLocally: false, minHubCoreVersion: '000.017.0012', executeCommandsLocally: false, ocfDeviceType: "x.com.st.d.energymeter") {
		capability "Energy Meter"
		capability "Power Meter"
		capability "Configuration"
		capability "Sensor"
		capability "Health Check"
		capability "Refresh"

		command "reset"

		fingerprint deviceId: "0x2101", inClusters: " 0x70,0x31,0x72,0x86,0x32,0x80,0x85,0x60"

	}

	preferences {
		input "reportType", "number", title: "ReportType: Send Watts data on a time interval (0), or on a change in wattage (1)? Enter a 0 or 1:", defaultValue: 1, range: "0..1", required: false, displayDuringSetup: true
		input "wattsChanged", "number", title: "For ReportType = 1, Don't send unless watts have changed by these many watts: (range 0 - 32,000W)", defaultValue: 50, range: "0..32000", required: false, displayDuringSetup: true
		input "wattsPercent", "number", title: "For ReportType = 1, Don't send unless watts have changed by this percent: (range 0 - 99%)", defaultValue: 10, range: "0..99", required: false, displayDuringSetup: true
		input "secondsWatts", "number", title: "For ReportType = 0, Send Watts data every how many seconds? (range 0 - 65,000 seconds)", defaultValue: 300, range: "0..65000", required: false, displayDuringSetup: true
		input "secondsKwh", "number", title: "Send kWh data every how many seconds? (range 0 - 65,000 seconds)", defaultValue: 300, range: "0..65000", required: false, displayDuringSetup: true
	}

	// simulator metadata
	simulator {
		for (int i = 0; i <= 10000; i += 1000) {
			status "power  ${i} W": new physicalgraph.zwave.Zwave().meterV1.meterReport(
					scaledMeterValue: i, precision: 3, meterType: 4, scale: 2, size: 4).incomingMessage()
		}
		for (int i = 0; i <= 100; i += 10) {
			status "energy  ${i} kWh": new physicalgraph.zwave.Zwave().meterV1.meterReport(
					scaledMeterValue: i, precision: 3, meterType: 0, scale: 0, size: 4).incomingMessage()
		}
	}

	// tile definitions
	tiles(scale: 2) {
		multiAttributeTile(name:"power", type: "generic", width: 6, height: 4){
			tileAttribute("device.power", key: "PRIMARY_CONTROL") {
				attributeState("default", label:'${currentValue} W')
			}
			tileAttribute("device.energy", key: "SECONDARY_CONTROL") {
				attributeState("default", label:'${currentValue} kWh')
			}
		}
		standardTile("reset", "device.energy", inactiveLabel: false, decoration: "flat",width: 2, height: 2) {
			state "default", label:'reset kWh', action:"reset"
		}
		standardTile("refresh", "device.power", inactiveLabel: false, decoration: "flat",width: 2, height: 2) {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		standardTile("configure", "device.power", inactiveLabel: false, decoration: "flat",width: 2, height: 2) {
			state "configure", label:'', action:"configuration.configure", icon:"st.secondary.configure"
		}

		main (["power","energy"])
		details(["power","energy", "reset","refresh", "configure"])
	}
}

def installed() {
	log.debug "installed()..."
	sendEvent(name: "checkInterval", value: 1860, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID, offlinePingable: "0"])
	response(refresh())
}

def updated() {
	log.debug "updated()..."
	response(refresh())
}

def ping() {
	log.debug "ping()..."
	refresh()
}

def parse(String description) {
	def result = null
	def cmd = zwave.parse(description, [0x31: 1, 0x32: 1, 0x60: 3])
	if (cmd) {
		result = createEvent(zwaveEvent(cmd))
	}
	log.debug "Parse returned ${result?.descriptionText}"
	return result
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
	def encapsulatedCommand = cmd.encapsulatedCommand(versions)
	if (encapsulatedCommand) {
		zwaveEvent(encapsulatedCommand)
	} else {
		log.warn "Unable to extract encapsulated cmd from $cmd"
		createEvent(descriptionText: cmd.toString())
	}
}

def zwaveEvent(physicalgraph.zwave.commands.crc16encapv1.Crc16Encap cmd) {
	def version = versions[cmd.commandClass as Integer]
	def ccObj = version ? zwave.commandClass(cmd.commandClass, version) : zwave.commandClass(cmd.commandClass)
	def encapsulatedCommand = ccObj?.command(cmd.command)?.parse(cmd.data)
	if (encapsulatedCommand) {
		zwaveEvent(encapsulatedCommand)
	} else {
		[:]
	}
}

def zwaveEvent(physicalgraph.zwave.commands.meterv1.MeterReport cmd) {
	meterReport(cmd.scale, cmd.scaledMeterValue)
}

private meterReport(scale, value) {
	if (scale == 0) {
		[name: "energy", value: value, unit: "kWh"]
	} else if (scale == 1) {
		[name: "energy", value: value, unit: "kVAh"]
	} else {
		[name: "power", value: Math.round(value), unit: "W"]
	}
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
	// Handles all Z-Wave commands we aren't interested in
	[:]
}

def refresh() {
	log.debug "refresh()..."
	delayBetween([
			encap(zwave.meterV2.meterGet(scale: 0)),
			encap(zwave.meterV2.meterGet(scale: 2))
	])
}

def reset() {
	log.debug "reset()..."
	// No V1 available
	delayBetween([
			encap(zwave.meterV2.meterReset()),
			encap(zwave.meterV2.meterGet(scale: 0))
	])
}

def configure() {
	log.debug "configure()..."
	delayBetween([
		encap(zwave.configurationV1.configurationSet(parameterNumber: 255, size: 4, scaledConfigurationValue: 1)), // Reset the device to the default settings
		encap(zwave.configurationV1.configurationSet(parameterNumber: 101, size: 4, scaledConfigurationValue: 4)), // combined power in watts
		encap(zwave.configurationV1.configurationSet(parameterNumber: 102, size: 4, scaledConfigurationValue: 8)), // combined energy in kWh
		encap(zwave.configurationV1.configurationSet(parameterNumber: 103, size: 4, scaledConfigurationValue: 0)), // no third report (battery reporting)
        encap(zwave.configurationV1.configurationSet(parameterNumber: 3, size: 1, scaledConfigurationValue: reportType)), // Send watts data based on a time interval (0), or based on a change in watts (1). 0 is default. 1 enables parameters 4 and 8.
        encap(zwave.configurationV1.configurationSet(parameterNumber: 4, size: 2, scaledConfigurationValue: wattsChanged)), // If parameter 3 is 1, don't send unless watts have changed by xx amount (50 is default) for the whole device.
        encap(zwave.configurationV1.configurationSet(parameterNumber: 8, size: 1, scaledConfigurationValue: wattsPercent)), // If parameter 3 is 1, don't send unless watts have changed by xx% (10% is default) for the whole device.
		encap(zwave.configurationV1.configurationSet(parameterNumber: 111, size: 4, scaledConfigurationValue: secondsWatts)), // Report watts every xx seconds (default is 5 minutes)
		encap(zwave.configurationV1.configurationSet(parameterNumber: 112, size: 4, scaledConfigurationValue: secondsKwh)), // Report kWh every xx seconds (default is 5 minutes)
		encap(zwave.configurationV1.configurationSet(parameterNumber: 113, size: 4, scaledConfigurationValue: 300)) // Report battery every 300 seconds (default)
	], 500)
}

private encap(physicalgraph.zwave.Command cmd) {
	if (zwaveInfo.zw.contains("s")) {
		secEncap(cmd)
	} else if (zwaveInfo.cc.contains("56")){
		crcEncap(cmd)
	} else {
		cmd.format()
	}
}

private secEncap(physicalgraph.zwave.Command cmd) {
	zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
}

private crcEncap(physicalgraph.zwave.Command cmd) {
	zwave.crc16EncapV1.crc16Encap().encapsulate(cmd).format()
}

private getVersions() {
	[
			0x32: 1,  // Meter
			0x70: 1,  // Configuration
			0x72: 1,  // ManufacturerSpecific
	]
}