// ________________________________________________________________________________
// KAFKA MAPPER ACTION
// ________________________________________________________________________________
import WsPacket from "@/websocket/WsPacket.js";
import { generateRandomInt32, Action, Code} from "@/websocket/ClientUtils.js"

export function buildKafkaMapperPkt(dto) {
    return new WsPacket({
        payload: JSON.stringify(dto),
        action: Action.KAFKA_MAPPER,
        id: generateRandomInt32(),
        code: Code.REQ
    })
}

export const KafkaMapperType = {
    SEND_MAPPER: "SEND_MAPPER",
    CONFIG_READ: "CONFIG_READ",
    CONFIG_WRITE: "CONFIG_WRITE"
}

export function buildKafkaSendMapper(mapper) {
    const dto = { 
        'type': KafkaMapperType.SEND_MAPPER, 
        'body': JSON.stringify(mapper)
    }
    return buildKafkaMapperPkt(dto)
}

export function buildKafkaReadConfig() {
    const dto = { 
        'type': KafkaMapperType.CONFIG_READ, 
        'body': '' 
    }
    return buildKafkaMapperPkt(dto)
}

export function buildKafkaWriteConfig(body) {
    const dto = { 
        'type': KafkaMapperType.CONFIG_WRITE, 
        'body': JSON.stringify(body)
    }
    return buildKafkaMapperPkt(dto)
}