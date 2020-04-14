
export default class TestDTO{

    constructor({id, created, count, message}) {
        this.id = id
        this.created = created
        this.count =  count
        this.message = message
    }

    toJson() {
        return JSON.stringify(this)
    }

    static fromJson(jsonString) {
        return JSON.parse(jsonString)
    }

}