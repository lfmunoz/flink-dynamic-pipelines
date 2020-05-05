/**
 *  TestUtils.js
 */

import WsPacket from "@/websocket/WsPacket.js";
import { generateRandomInt32, Action, Code} from "@/websocket/ClientUtils.js"

// ________________________________________________________________________________
// KAFKA ACTION
// ________________________________________________________________________________
export function createWsPacketForKafka(obj) {
    const aWsPacket = new WsPacket({
        payload: JSON.stringify(obj),
        action: Action.KAFKA_CONSUMER,
        id: generateRandomInt32(),
        code: Code.REQ
    })
    return aWsPacket
}

export const KafkaProducerType = {
    SAMPLE: "SAMPLE",
    CONFIG_READ: "CONFIG_READ",
    CONFIG_WRITE: "CONFIG_WRITE"
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

export function buildKafkaSample(debounce) {
    const dto = { 
        'type': KafkaProducerType.SAMPLE, 
        'body': debounce
    }
    return createWsPacketForKafka(dto)
}