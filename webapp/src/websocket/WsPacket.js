
export default class WsPacket {

    constructor({action, payload, id, code}) {
        this.action = action
        this.payload = payload
        this.id =  id 
        this.code = code
    }

    toJson() {
        return JSON.stringify(this)
    }

    static fromJson(jsonString) {
        return JSON.parse(jsonString)
    }

}