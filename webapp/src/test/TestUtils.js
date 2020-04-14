/**
 *  TestUtils.js
 */

import WsPacket from "@/websocket/WsPacket.js";
import TestDTO from "@/test/TestDTO.js";
import { generateRandomInt32 } from "@/websocket/ClientUtils.js"
import { Action } from "@/websocket/ClientUtils.js"
import { Code } from "@/websocket/ClientUtils.js"

export function buildTestActionWsPacket(count) {
    const dto = new TestDTO({
        id: 0,
        created: Date.now(),
        count: count,
        message: "request 0"
    })
    return new WsPacket({
        payload: dto.toJson(),
        action: Action.TEST,
        id: generateRandomInt32(),
        code: Code.REQ
    })
}


// ________________________________________________________________________________
// KAFKA ACTION
// ________________________________________________________________________________
export function createWsPacketForKafka(obj) {
    const aWsPacket = new WsPacket({
        payload: JSON.stringify(obj),
        action: Action.KAFKA,
        id: generateRandomInt32(),
        code: Code.REQ
    })
    return aWsPacket
}

export const KafkaActionType = {
    START: "START",
    STOP: "STOP",
    SAMPLE: "SAMPLE",
    CANCEL: "CANCEL",
    STATUS: "STATUS",
    ERROR: "ERROR"
}

export function buildDefaultKafkaAction() {
    return {
        'type': KafkaActionType.STATUS,
        'body': '',
    }
}


