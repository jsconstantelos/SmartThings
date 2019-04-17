/****************************************************************************
 * DRIVER NAME:  Smartenit ZBMLC15
 * DESCRIPTION:	 Device handler for Smartenit Metering Single Load Controller (#4034A)
 * 				 https://smartenit.com/product/zbmlc15/
 * $Rev:         $: 3
 * $Author:      $: Dhawal Doshi
 * $Date:	 	 $: 07/13/2018
 ****************************************************************************
 * This software is owned by Compacta and/or its supplier and is protected
 * under applicable copyright laws. All rights are reserved. We grant You,
 * and any third parties, a license to use this software solely and
 * exclusively on Compacta products. You, and any third parties must reproduce
 * the copyright and warranty notice and any other legend of ownership on each
 * copy or partial copy of the software.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS". COMPACTA MAKES NO WARRANTIES, WHETHER
 * EXPRESS, IMPLIED OR STATUTORY, INCLUDING, BUT NOT LIMITED TO, IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE,
 * ACCURACY OR LACK OF NEGLIGENCE. COMPACTA SHALL NOT, UNDERN ANY CIRCUMSTANCES,
 * BE LIABLE FOR ANY DAMAGES, INCLUDING, BUT NOT LIMITED TO, SPECIAL,
 * INCIDENTAL OR CONSEQUENTIAL DAMAGES FOR ANY REASON WHATSOEVER.
 *
 * Copyright Compacta International, Ltd 2016. All rights reserved
 ****************************************************************************/

 import groovy.transform.Field

 @Field final OnoffCluster = 0x0006
 
 @Field final MeteringEP = 0x0A
 @Field final MeteringCluster = 0x0702
 @Field final MetSummDataType = 0x25
 @Field final MetDemandDataType = 0x2A
 @Field final MeteringDemandAttrID = 0x0400
 @Field final MeteringSummAttrID = 0x0000
 @Field final MeteringDivisor = 1000

 @Field final SumMaxReportTimeSecs = 0
 @Field final SumMinReportTimeSecs = 0
 @Field final DemMaxReportTimeSecs = 0
 @Field final DemMinReportTimeSecs = 0
 @Field final MeteringReportableChange = 1	//watt-hour and watts

 @Field final ReportIntervalsecs = 300
 @Field final HealthCheckSecs = 600
 
metadata {
	// Automatically generated. Make future change here.
	definition (name: "My Smartenit ZBMLC15", namespace: "jsconstantelos", author: "Smartenit", ocfDeviceType: "oic.d.switch", genericHandler: "Zigbee") {
		capability "Switch"
		capability "Power Meter"
		capability "Configuration"
		capability "Refresh"
		capability "Sensor"
		capability "Energy Meter"
		capability "Actuator"
        capability "Health Check"

		// indicates that device keeps track of heartbeat (in state.heartbeat)
		attribute "heartbeat", "string"
        attribute "voltage", "number"
        attribute "current", "number"

		command "reset"

		fingerprint profileId: "0104", inClusters: "0000,0005,0003,0004,0006,0702", model: "ZBMLC15", deviceJoinName: "Smartenit ZBMLC15"
}

	preferences {
		section {
			image(name: 'educationalcontent', multiple: true, images: [
				"http://cdn.device-gse.smartthings.com/Outlet/US/OutletUS1.jpg",
				"http://cdn.device-gse.smartthings.com/Outlet/US/OutletUS2.jpg"
				])
		}
	}

	// UI tile definitions
	tiles(scale: 2) {
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#00A0DC"
				attributeState "off", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff"
			}
            tileAttribute ("power", key: "SECONDARY_CONTROL") {
				attributeState "power", label:'${currentValue} W'
			}
		}
		standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:'Refresh', action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		valueTile("energy", "device.energy", width: 2, height: 2) {
		    state "val", label:'0${currentValue} kWh', action:"configuration.configure"
		}
		standardTile("reset", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:'Reset kWh', action:"reset", icon:"st.secondary.refresh"
		}
		main "switch"
		details(["switch","refresh","energy","reset"])
	}
}

def getFPoint(String FPointHex){
    return (Float)Long.parseLong(FPointHex, 16)
}

/*
* Parse incoming device messages to generate events
*/
def parse(String description) {
//	log.debug "parse... description: ${description}"    
	def event = zigbee.getEvent(description)
	if ((event) && (event.name == "switch")) {
        sendEvent(event)
    }
    else  {
        def mapDescription = zigbee.parseDescriptionAsMap(description)
//		log.debug "parse... mapDescription: ${mapDescription}"
        if(mapDescription.clusterInt == MeteringCluster) {
            if(mapDescription.attrId == "0400") {
            	log.debug "Received Power value: ${mapDescription.value}"
                sendEvent(name:"power", value: getFPoint(mapDescription.value))
            }
            else if(mapDescription.attrId == "0000") {
//            	log.debug "Received Energy value: ${mapDescription.value}"
                sendEvent(name:"energy", value: getFPoint(mapDescription.value)/MeteringDivisor)
            }
            else if(mapDescription.attrId == "e000") {
//            	log.debug "Received Current value: ${mapDescription.value}"
                sendEvent(name:"current", value: getFPoint(mapDescription.value), "displayed": false)
            }
            else if(mapDescription.attrId == "e001") {
//            	log.debug "Received Voltage value: ${mapDescription.value}"
                sendEvent(name:"voltage", value: getFPoint(mapDescription.value), "displayed": false)
            }
            
            if (mapDescription.additionalAttrs) {
//            	log.debug "Additional attrs found"
                if(mapDescription.additionalAttrs[0].attrId == "0400") {
            		log.debug "Received attr Power value: ${mapDescription.additionalAttrs[0].value}"
                	sendEvent(name:"power", value: getFPoint(mapDescription.additionalAttrs[0].value))
            	}
            	else if(mapDescription.additionalAttrs[0].attrId == "0000") {
//            		log.debug "Received attr Energy value: ${mapDescription.additionalAttrs[0].value}"
                	sendEvent(name:"energy", value: getFPoint(mapDescription.additionalAttrs[0].value)/MeteringDivisor)
           	 	}
                else if(mapDescription.additionalAttrs[0].attrId == "e000") {
//            		log.debug "Received attr Current value: ${mapDescription.additionalAttrs[0].value}"
                    sendEvent(name:"current", value: getFPoint(mapDescription.additionalAttrs[0].value), "displayed": false)
           	 	}
                else if(mapDescription.additionalAttrs[0].attrId == "e001") {
//            		log.debug "Received attr Voltage value: ${mapDescription.additionalAttrs[0].value}"
                    sendEvent(name:"voltage", value: getFPoint(mapDescription.additionalAttrs[0].value), "displayed": false)
           	 	}
            }
        }
        else if(mapDescription.clusterInt == OnoffCluster) {
            def attrName = "switch"
            def attrValue = null
			if(mapDescription.command == "0B") {
                if(mapDescription.data[0] == "00") {
                    attrValue = "off"
                }else if(mapDescription.data[0] == "01") {
                    attrValue = "on"
                }else{
                	return
                }
            }else {
                if(mapDescription.value == "00") {
                    attrValue = "off"
                }else if(mapDescription.value == "01") {
                    attrValue = "on"
                }else{
                	return
                }
            }
            return sendEvent(name: attrName, value: attrValue)
        }
        return createEvent([:])
    }
}

def off() {
	log.debug "Turning Off..."
    sendEvent(name:"power", value: 0.0)
	zigbee.off()
}

def on() {
	log.debug "Turning On..."
	zigbee.on()
}

def reset() {
	log.debug "Resetting kWh value"
    [
    	zigbee.command(0x0702, 0xe0, "00"), "delay 1000",
    	zigbee.readAttribute(0x0702, 0x0000)
	]
}

def refresh() {
	log.debug "Refreshing..."
	if (state.configured != 1) {
    	return configure()
	}
    
    return (
    	zigbee.readAttribute(MeteringCluster, MeteringSummAttrID, [destEndpoint:MeteringEP]) + 
    	zigbee.readAttribute(MeteringCluster, MeteringDemandAttrID, [destEndpoint:MeteringEP]) + 
    	zigbee.onOffRefresh() 
    )
}

def configure() {
	log.debug "Configuring..."
    state.configured = 1
    
    sendEvent(name: "checkInterval", value: HealthCheckSecs, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
    
	def meterconfigCmds = ["zdo bind 0x${device.deviceNetworkId} MeteringEP 0x01 MeteringCluster {${device.zigbeeId}} {}"]
    def onoffconfigCmds = ["zdo bind 0x${device.deviceNetworkId} MeteringEP 0x01 OnoffCluster {${device.zigbeeId}} {}"]
    return  (
    	meterconfigCmds + 
        onoffconfigCmds +
    	zigbee.configureReporting(MeteringCluster, MeteringSummAttrID, MetSummDataType, SumMinReportTimeSecs, SumMaxReportTimeSecs, null, [destEndpoint:MeteringEP]) + 
    	zigbee.configureReporting(MeteringCluster, MeteringDemandAttrID, MetDemandDataType, DemMinReportTimeSecs, DemMaxReportTimeSecs, null, [destEndpoint:MeteringEP]) +
        zigbee.configureReporting(OnoffCluster, 0x0000, 0x10, 0, 0, 0x01, [destEndpoint:MeteringEP])
   	)
//    return  (
//    	meterconfigCmds + 
//      onoffconfigCmds +
//    	zigbee.configureReporting(MeteringCluster, MeteringSummAttrID, MetSummDataType, SumMinReportTimeSecs, SumMaxReportTimeSecs, MeteringReportableChange, [destEndpoint:MeteringEP]) + 
//    	zigbee.configureReporting(MeteringCluster, MeteringDemandAttrID, MetDemandDataType, DemMinReportTimeSecs, DemMaxReportTimeSecs, MeteringReportableChange, [destEndpoint:MeteringEP]) +
//      zigbee.configureReporting(OnoffCluster, 0x0000, 0x10, 0, 0, 0x01, [destEndpoint:MeteringEP])
//  	)
}

def updated() {
    log.debug "Updated..."
//    configure()
    refresh()
}

def ping() {
	log.debug "Health Check called..."
    [
    	zigbee.readAttribute(MeteringCluster, MeteringSummAttrID, [destEndpoint:MeteringEP]), "delay 1000",
    	zigbee.readAttribute(MeteringCluster, MeteringDemandAttrID, [destEndpoint:MeteringEP]), "delay 1000",
    	zigbee.onOffRefresh()
    ]
}