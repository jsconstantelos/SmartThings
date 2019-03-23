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
 *	Author: Halo/SmartThings (original), MMaxwell (from Hubitat), JSConstantelos (modifications/updates)
 *
 *  Updates:
 *  -------
 *  03-14-2019 : Initial commit.
 *
 */

//************************************************
// THIS IS NOT COMPLETE, DO NOT USE.  TESTING ONLY
//************************************************

import physicalgraph.zigbee.clusters.iaszone.ZoneStatus
import physicalgraph.zigbee.zcl.DataType

metadata {
    definition (name: "My Halo Smoke Alarm", namespace: "jsconstantelos", author: "ManyContributors") {
        capability "Configuration"
        capability "Switch"
        capability "Switch Level"
        capability "Color Control"
        capability "Relative Humidity Measurement"
        capability "Temperature Measurement"
        capability "Smoke Detector"
        capability "Carbon Monoxide Detector"
        capability "Refresh"
        capability "Sensor"
        capability "Actuator"

        fingerprint inClusters: "0000,0001,0003,0402,0403,0405,0500,0502", manufacturer: "HaloSmartLabs", model: "halo", deviceJoinName: "Halo Smoke Alarm"
    }

	tiles(scale: 2) {
		multiAttributeTile(name:"switch", type: "lighting", width: 1, height: 1, canChangeIcon: true) {
			tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
				attributeState("on", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#00a0dc")
				attributeState("off", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff")
			}
			tileAttribute ("device.level", key: "SLIDER_CONTROL") {
				attributeState "level", action:"switch level.setLevel"
			}
			tileAttribute ("device.color", key: "COLOR_CONTROL") {
				attributeState "color", action:"setColor"
			}
		}
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", action: "refresh.refresh", icon: "st.secondary.refresh"
		}
	}

	main(["switch"])
	details(["switch", "refresh"])
}

def installed(){
//    sendEvent(name:"smoke", value:"clear")
//    sendEvent(name:"carbonMonoxide", value:"clear")
}

def parse(String description) {
	log.debug "description(): $description"
	def map = zigbee.getEvent(description)
	if (!map) {
		if (description?.startsWith('zone status')) {
			map = parseIasMessage(description)
		} else {
			map = parseAttrMessage(description)
		}
	}
	log.debug "Parse returned $map"
	def result = map ? createEvent(map) : [:]
	if (description?.startsWith('enroll request')) {
		List cmds = zigbee.enrollResponse()
		log.debug "enroll response: ${cmds}"
		result = cmds?.collect { new physicalgraph.device.HubAction(it) }
	}
	return result
}

def parseAttrMessage(String description){
	def descMap = zigbee.parseDescriptionAsMap(description)
	def map = [:]
	if (descMap?.clusterInt == zigbee.POWER_CONFIGURATION_CLUSTER && descMap.commandInt != 0x07 && descMap.value) {
		map = getBatteryPercentageResult(Integer.parseInt(descMap.value, 16))
	} else if (descMap?.clusterInt == zigbee.IAS_ZONE_CLUSTER && descMap.attrInt == zigbee.ATTRIBUTE_IAS_ZONE_STATUS) {
		def zs = new ZoneStatus(zigbee.convertToInt(descMap.value, 16))
		map = translateZoneStatus(zs)
	}
	return map;
}

def parseIasMessage(String description) {
	ZoneStatus zs = zigbee.parseZoneStatus(description)
	return getDetectedResult(zs.isAlarm1Set() || zs.isAlarm2Set())
}

private Map translateZoneStatus(ZoneStatus zs) {
	return getDetectedResult(zs.isAlarm1Set() || zs.isAlarm2Set())
}

private Map getBatteryPercentageResult(rawValue) {
	log.debug "Battery Percentage rawValue = ${rawValue} -> ${rawValue / 2}%"
	def result = [:]

	if (0 <= rawValue && rawValue <= 200) {
		result.name = 'battery'
		result.translatable = true
		result.value = Math.round(rawValue / 2)
		result.descriptionText = "${device.displayName} battery was ${result.value}%"
	}

	return result
}

def getDetectedResult(value) {
	def detected = value ? 'detected': 'clear'
	String descriptionText = "${device.displayName} smoke ${detected}"
	return [name:'smoke',
			value: detected,
			descriptionText:descriptionText,
			translatable:true]
}

private getAlarmResult(rawValue){
    if (rawValue == null) return
    def value
    def name
    def descriptionText = "${device.displayName} "
    switch (rawValue) {
        case "00": //cleared
            if (device.currentValue("smoke") == "detected") {
                descriptionText = "${descriptionText} smoke is clear"
                sendEvent(name:"smoke", value:"clear",descriptionText:descriptionText)
            } else if (device.currentValue("carbonMonoxide") == "detected") {
                descriptionText = "${descriptionText} carbon monoxide is clear"
                sendEvent(name:"carbonMonoxide", value:"clear",descriptionText:descriptionText)
            } else {
                descriptionText = "${descriptionText} smoke and carbon monoxide are clear"
                sendEvent(name:"smoke", value:"clear")
                sendEvent(name:"carbonMonoxide", value:"clear")
            }
            break
    /*
    case "04":	//Elevated Smoke Detected (pre)
        descriptionText = "${descriptionText} smoke was detected (pre alert)"
        sendEvent(name:"smoke", value:"detected",descriptionText:"${descriptionText} smoke was detected")
        descriptionText = "${descriptionText} smoke was detected (pre alert)"
        break
    case "07":	//Smoke Detected, send again, force state change
        sendEvent(name:"smoke", value:"detected",descriptionText:"${descriptionText} smoke was detected", isStateChange: true)
        descriptionText = "${descriptionText} smoke was detected"
        break
    */
        case "09":	//Silenced
            log.debug "getAlarmResult- Silenced"
            break
        case "AL1":
            descriptionText = "${descriptionText} smoke was detected"
            sendEvent(name:"smoke", value:"detected",descriptionText:descriptionText, isStateChange: true)
            break
        case "AL2":
            descriptionText = "${descriptionText} carbon monoxide was detected"
            sendEvent(name:"carbonMonoxide", value:"detected",descriptionText:descriptionText, isStateChange: true)
            break
        default :
            descriptionText = "${descriptionText} getAlarmResult- skipped:${value}"
            return
    }
    if (txtEnable) log.info "${descriptionText}"
}

private getHumidityResult(valueRaw){
    if (valueRaw == null) return
    def value = Integer.parseInt(valueRaw, 16) / 100
    def descriptionText = "${device.displayName} RH is ${value}%"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name:"humidity", value:value, descriptionText:descriptionText, unit:"%")
}

private getPressureResult(hex){
    if (hex == null) return
    def valueRaw = hexStrToUnsignedInt(hex)
    def value = valueRaw / 10
    def descriptionText = "${device.displayName} pressure is ${value}kPa"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name:"pressure", value:value, descriptionText:descriptionText, unit:"kPa")
}

private getTemperatureResult(valueRaw){
    if (valueRaw == null) return
    def tempC = hexStrToSignedInt(valueRaw) / 100
    def value = convertTemperatureIfNeeded(tempC.toFloat(),"c",2)
    def unit = "°${location.temperatureScale}"
    def descriptionText = "${device.displayName} temperature is ${value}${unit}"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name:"temperature",value:value, descriptionText:descriptionText, unit:unit)
}

private getSwitchResult(rawValue){
    def value = rawValue == "01" ? "on" : "off"
    def name = "switch"
    if (device.currentValue("${name}") == value){
        descriptionText = "${device.displayName} is ${value}"
    } else {
        descriptionText = "${device.displayName} was turned ${value}"
    }
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name:name,value:value,descriptionText:descriptionText)
}

private getLevelResult(rawValue){
    def unit = "%"
    def value = Math.round(Integer.parseInt(rawValue,16) / 2.55)
    def name = "level"
    if (device.currentValue("${name}") == value){
        descriptionText = "${device.displayName} is ${value}${unit}"
    } else {
        descriptionText = "${device.displayName} was set to ${value}${unit}"
    }
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name:name,value:value,descriptionText:descriptionText,unit:unit)
}

private getColorResult(rawValue,attrInt){
    def unit = "%"
    def value = Math.round(Integer.parseInt(rawValue,16) / 2.55)
    def name
    switch (attrInt){
        case 0: //hue
            name = "hue"
            if (device.currentValue("${name}")?.toInteger() == value){
                descriptionText = "${device.displayName} ${name} is ${value}${unit}"
            } else {
                descriptionText = "${device.displayName} ${name} was set to ${value}${unit}"
            }
            state.lastHue = rawValue
            break
        case 1: //sat
            name = "saturation"
            if (device.currentValue("${name}")?.toInteger() == value){
                descriptionText = "${device.displayName} ${name} is ${value}${unit}"
            } else {
                descriptionText = "${device.displayName} ${name} was set to ${value}${unit}"
            }
            state.lastSaturation = rawValue
            break
        default :
            return
    }
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name:name,value:value,descriptionText:descriptionText,unit:unit)
}

def on() {
    def cmd = [
            "st cmd 0x${device.deviceNetworkId} 0x02 0x0006 1 {}","delay 200",
            "st rattr 0x${device.deviceNetworkId} 2 0x0006 0 {}"
    ]
    return cmd
}

def off() {
    def cmd = [
            "st cmd 0x${device.deviceNetworkId} 0x02 0x0006 0 {}","delay 200",
            "st rattr 0x${device.deviceNetworkId} 2 0x0006 0 {}"
    ]
    return cmd
}

def setLevel(value) {
    setLevel(value,(transitionTime?.toBigDecimal() ?: 1000) / 1000)
}

def setLevel(value,rate) {
    rate = rate.toBigDecimal()
    def scaledRate = (rate * 10).toInteger()
    def cmd = []
    def isOn = device.currentValue("switch") == "on"
    value = (value.toInteger() * 2.55).toInteger()
    if (isOn){
        cmd = [
                "st cmd 0x${device.deviceNetworkId} 2 0x0008 4 {0x${intTo8bitUnsignedHex(value)} 0x${intTo16bitUnsignedHex(scaledRate)}}",
                "delay ${(rate * 1000) + 400}",
                "st rattr 0x${device.deviceNetworkId} 2 0x0008 0 {}"
        ]
    } else {
        cmd = [
                "st cmd 0x${device.deviceNetworkId} 2 0x0008 4 {0x${intTo8bitUnsignedHex(value)} 0x0100}", "delay 200",
                "st rattr 0x${device.deviceNetworkId} 2 0x0006 0 {}", "delay 200",
                "st rattr 0x${device.deviceNetworkId} 2 0x0008 0 {}"
        ]
    }
    return cmd
}


def setColor(value){
    if (value.hue == null || value.saturation == null) return

    def rate = transitionTime?.toInteger() ?: 1000
    def isOn = device.currentValue("switch") == "on"

    def hexSat = zigbee.convertToHexString(Math.round(value.saturation.toInteger() * 254 / 100).toInteger(),2)
    def level
    if (value.level) level = (value.level.toInteger() * 2.55).toInteger()
    def cmd = []
    def hexHue

    hexHue = zigbee.convertToHexString(Math.round(value.hue * 254 / 100).toInteger(),2)

    if (isOn){
        if (value.level){
            cmd = [
                    "st cmd 0x${device.deviceNetworkId} 2 0x0300 0x06 {${hexHue} ${hexSat} ${intTo16bitUnsignedHex(rate / 100)}}","delay 200",
                    "st cmd 0x${device.deviceNetworkId} 2 0x0008 4 {0x${intTo8bitUnsignedHex(level)} 0x${intTo16bitUnsignedHex(rate / 100)}}",
                    "delay ${rate + 400}",
                    "st rattr 0x${device.deviceNetworkId} 2 0x0300 0x0000 {}", "delay 200",
                    "st rattr 0x${device.deviceNetworkId} 2 0x0300 0x0001 {}", "delay 200",
                    "st rattr 0x${device.deviceNetworkId} 2 0x0008 0 {}"
            ]
        } else {
            cmd = [
                    "st cmd 0x${device.deviceNetworkId} 2 0x0300 0x06 {${hexHue} ${hexSat} ${intTo16bitUnsignedHex(rate / 100)}}",
                    "delay ${rate + 400}",
                    "st rattr 0x${device.deviceNetworkId} 2 0x0300 0x0000 {}", "delay 200",
                    "st rattr 0x${device.deviceNetworkId} 2 0x0300 0x0001 {}"
            ]
        }
    } else if (level){
        cmd = [
                "st cmd 0x${device.deviceNetworkId} 2 0x0300 0x06 {${hexHue} ${hexSat} 0x0100}", "delay 200",
                "st cmd 0x${device.deviceNetworkId} 2 0x0008 4 {0x${intTo8bitUnsignedHex(level)} 0x0100}", "delay 200",
                "st rattr 0x${device.deviceNetworkId} 2 0x0006 0 {}", "delay 200",
                "st rattr 0x${device.deviceNetworkId} 2 0x0008 0 {}", "delay 200",
                "st rattr 0x${device.deviceNetworkId} 2 0x0300 0x0000 {}", "delay 200",
                "st rattr 0x${device.deviceNetworkId} 2 0x0300 0x0001 {}"
        ]
    } else {
        cmd = [
                "st cmd 0x${device.deviceNetworkId} 2 0x0300 0x06 {${hexHue} ${hexSat} 0x0100}", "delay 200",
                "st cmd 0x${device.deviceNetworkId} 2 0x0006 1 {}","delay 200",
                "st rattr 0x${device.deviceNetworkId} 2 0x0006 0 {}","delay 200",
                "st rattr 0x${device.deviceNetworkId} 2 0x0300 0x0000 {}", "delay 200",
                "st rattr 0x${device.deviceNetworkId} 2 0x0300 0x0001 {}"
        ]
    }
    state.lastSaturation = hexSat
    state.lastHue = hexHue
    return cmd
}

def setHue(value) {
    def hexHue
    def rate = 1000
    def isOn = device.currentValue("switch") == "on"
    hexHue = zigbee.convertToHexString(Math.round(value * 254 / 100).toInteger(),2)
    def hexSat = state.lastSaturation
    def cmd = []
    if (isOn){
        cmd = [
                "st cmd 0x${device.deviceNetworkId} 2 0x0300 0x06 {${hexHue} ${hexSat} ${intTo16bitUnsignedHex(rate / 100)}}",
                "delay ${rate + 400}",
                "st rattr 0x${device.deviceNetworkId} 2 0x0300 0x0000 {}"
        ]
    } else {
        cmd = [
                "st cmd 0x${device.deviceNetworkId} 2 0x0300 0x06 {${hexHue} ${hexSat} 0x0100}", "delay 200",
                "st cmd 0x${device.deviceNetworkId} 2 0x0006 1 {}","delay 200",
                "st rattr 0x${device.deviceNetworkId} 2 0x0006 0 {}","delay 200",
                "st rattr 0x${device.deviceNetworkId} 2 0x0300 0x0000 {}"
        ]
    }
    state.lastHue = hexHue
    return cmd
}

def setSaturation(value) {
    def rate = 1000
    def cmd = []
    def isOn = device.currentValue("switch") == "on"
    def hexSat = zigbee.convertToHexString(Math.round(value * 254 / 100).toInteger(),2)
    def hexHue = state.lastHue
    if (isOn){
        cmd = [
                "st cmd 0x${device.deviceNetworkId} 2 0x0300 0x06 {${hexHue} ${hexSat} ${intTo16bitUnsignedHex(rate / 100)}}",
                "delay ${rate + 400}",
                "st rattr 0x${device.deviceNetworkId} 2 0x0300 0x0001 {}"
        ]
    } else {
        cmd = [
                "st cmd 0x${device.deviceNetworkId} 2 0x0300 0x06 {${hexHue} ${hexSat} 0x0100}", "delay 200",
                "st cmd 0x${device.deviceNetworkId} 2 0x0006 1 {}","delay 200",
                "st rattr 0x${device.deviceNetworkId} 2 0x0006 0 {}","delay 200",
                "st rattr 0x${device.deviceNetworkId} 2 0x0300 0x0001 {}"
        ]
    }
    state.lastSaturation = hexSat
    return cmd
}

//
// PING is used by Device-Watch in attempt to reach the Device
//
def ping() {
	log.debug "ping "
	zigbee.readAttribute(zigbee.IAS_ZONE_CLUSTER, zigbee.ATTRIBUTE_IAS_ZONE_STATUS)
}

def refresh() {
    if (logEnable) log.debug "refresh"
    return  [
            "st rattr 0x${device.deviceNetworkId} 1 0x0402 0x0000 {}","delay 200",  //temp OK
            "st rattr 0x${device.deviceNetworkId} 1 0x0403 0x0000 {}","delay 200",  // pressure
            "st rattr 0x${device.deviceNetworkId} 1 0x0405 0x0000 {}" ,"delay 200", //hum OK
            "st rattr 0x${device.deviceNetworkId} 2 0x0006 0 {}","delay 200",  //light state
            "st rattr 0x${device.deviceNetworkId} 2 0x0008 0 {}","delay 200",  //light level
            "st rattr 0x${device.deviceNetworkId} 2 0x0300 0x0000 {}","delay 200", //hue
            "st rattr 0x${device.deviceNetworkId} 2 0x0300 0x0001 {}" //sat
    ]

}

def configure() {
    log.warn "configure..."
    sendEvent(name: "checkInterval", value:6 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
    def cmds = [
            //bindings
            "zdo bind 0x${device.deviceNetworkId} 1 1 0x0402 {${device.zigbeeId}} {}", "delay 200",	//temp
            "zdo bind 0x${device.deviceNetworkId} 1 1 0x0403 {${device.zigbeeId}} {}", "delay 200",	//pressure
            "zdo bind 0x${device.deviceNetworkId} 1 1 0x0405 {${device.zigbeeId}} {}", "delay 200",	//hum
            "zdo bind 0x${device.deviceNetworkId} 2 1 0x0006 {${device.zigbeeId}} {}", "delay 200",
            "zdo bind 0x${device.deviceNetworkId} 2 1 0x0008 {${device.zigbeeId}} {}", "delay 200",
            "zdo bind 0x${device.deviceNetworkId} 2 1 0x0300 {${device.zigbeeId}} {}", "delay 200",
            //mfr specific
            "zdo bind 0x${device.deviceNetworkId} 4 1 0xFD00 {${device.zigbeeId}} {1201}", "delay 200",  //appears to be custom alarm
            "zdo bind 0x${device.deviceNetworkId} 4 1 0xFD01 {${device.zigbeeId}} {1201}", "delay 200", //hush???
            "zdo bind 0x${device.deviceNetworkId} 4 1 0xFD02 {${device.zigbeeId}} {1201}", "delay 200",
            //reporting
            "st cr 0x${device.deviceNetworkId} 1 0x0402 0x0000 0x29 1 43200 {50}","delay 200",	//temp
            "st cr 0x${device.deviceNetworkId} 1 0x0405 0x0000 0x21 1 43200 {50}","delay 200",	//hum
            "st cr 0x${device.deviceNetworkId} 1 0x0403 0x0000 0x29 1 43200 {1}","delay 200",	//pressure
            //mfr specific reporting
            "st cr 0x${device.deviceNetworkId} 0x04 0xFD00 0x0000 0x30 5 120 {} {1201}","delay 200",	//alarm
            "st cr 0x${device.deviceNetworkId} 0x04 0xFD00 0x0001 0x30 5 120 {} {1201}","delay 200",	//alarm
            //need to verify ep, st attrib id's 1, and 0
            "st cr 0x${device.deviceNetworkId} 0x01 0xFD01 0x0000 0x30 5 120 {} {1201}","delay 200",	//hush???
            "st cr 0x${device.deviceNetworkId} 0x01 0xFD01 0x0001 0x0A 5 120 {} {1201}","delay 200",	//hush???
            "st cr 0x${device.deviceNetworkId} 0x01 0xFD02 0x0000 0x29 5 120 {} {1201}","delay 200",	//no idea...

    ] + zigbee.enrollResponse(1200) + refresh()
    return cmds
}

def configuredemo() {
	[
		"zdo bind 0x${device.deviceNetworkId} 1 1 0x000 {${device.zigbeeId}} {}", "delay 1000",
        "zdo bind 0x${device.deviceNetworkId} 1 1 0x001 {${device.zigbeeId}} {}", "delay 1000",
		"zdo bind 0x${device.deviceNetworkId} 1 1 0x201 {${device.zigbeeId}} {}", "delay 1000",
		"zdo bind 0x${device.deviceNetworkId} 1 1 0x202 {${device.zigbeeId}} {}", "delay 1000",
		"zcl global send-me-a-report 1 0x20 0x20 3600 86400 {01}", "delay 1000", // Battery report
		"send 0x${device.deviceNetworkId} 1 1"
	]
    zigbee.configureReporting(0x0201, 0x0029, 0x19, 60, 0, null) // Thermostat operating state report to send whenever it changes, or at a minimum of every minute.
}

def configuredemoagain() {
	log.debug "configure"
	sendEvent(name: "checkInterval", value:6 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
	Integer minReportTime = 0
	Integer maxReportTime = 180
	Integer reportableChange = null
	return refresh() + zigbee.enrollResponse() + zigbee.configureReporting(zigbee.POWER_CONFIGURATION_CLUSTER, 0x0021, DataType.UINT8, 30, 21600, 0x10) +
				zigbee.configureReporting(zigbee.IAS_ZONE_CLUSTER, zigbee.ATTRIBUTE_IAS_ZONE_STATUS, DataType.BITMAP16, minReportTime, maxReportTime, reportableChange)
}

def intTo16bitUnsignedHex(value) {
    def hexStr = zigbee.convertToHexString(value.toInteger(),4)
    return new String(hexStr.substring(2, 4) + hexStr.substring(0, 2))
}

def intTo8bitUnsignedHex(value) {
    return zigbee.convertToHexString(value.toInteger(), 2)
}