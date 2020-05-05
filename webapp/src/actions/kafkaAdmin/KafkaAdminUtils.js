/**
 *  TestUtils.js
 */

import WsPacket from "@/websocket/WsPacket.js";
import { generateRandomInt32, Action, Code} from "@/websocket/ClientUtils.js"

export function buildKafkaAdminPkt(dto) {
    return new WsPacket({
        payload: JSON.stringify(dto),
        action: Action.KAFKA_ADMIN,
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

export const KafkaAdminActionType = {
    TOPICS: "TOPICS",
    CONSUMERS: "CONSUMERS"
}

export function buildTopicsKafkaAdminPkt() {
    const dto = {
        'type': KafkaAdminActionType.TOPICS,
        'body': '',
    }

    return buildKafkaAdminPkt(dto)
}

export function buildConsumersKafkaAdminPkt() {
    const dto = {
        'type': KafkaAdminActionType.CONSUMERS,
        'body': '',
    }
    return buildKafkaAdminPkt(dto)
}