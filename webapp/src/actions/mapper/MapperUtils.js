/**
 *  TestUtils.js
 */

import WsPacket from "@/websocket/WsPacket.js";
import { generateRandomInt32, Action, Code} from "@/websocket/ClientUtils.js"

export function buildKafkaProducerPkt(dto) {
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
        action: Action.KAFKA_PRODUCER,
        id: generateRandomInt32(),
        code: Code.REQ
    })
    return aWsPacket
}

export const KafkaProducerType = {
    START: "START",
    CONFIG_READ: "CONFIG_READ",
    CONFIG_WRITE: "CONFIG_WRITE"
}

export function buildKafkaStart() {
    const dto = { 
        'type': KafkaProducerType.START, 
        'body': '' 
    }
    return createWsPacketForKafka(dto)
}

export function buildKafkaReadConfig() {
    const dto = { 
        'type': KafkaProducerType.CONFIG_READ, 
        'body': '' 
    }
    return createWsPacketForKafka(dto)
}

export function buildKafkaWriteConfig(body) {
    const dto = { 
        'type': KafkaProducerType.CONFIG_WRITE, 
        'body': JSON.stringify(body)
    }
    return createWsPacketForKafka(dto)
}