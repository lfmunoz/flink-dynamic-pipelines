
export default class AuthDTO{

    constructor(username, password) {
        this.username = username
        this.password =  password
    }

    toJson() {
        return JSON.stringify(this)
    }

    static fromJson(jsonString) {
        return JSON.parse(jsonString)
    }

}