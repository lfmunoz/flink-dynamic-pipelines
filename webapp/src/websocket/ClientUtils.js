/**
 *  ClientUtils.js
 */

import WsPacket from "@/websocket/WsPacket.js";
import AuthDTO from "@/websocket/AuthDTO.js"

//________________________________________________________________________________
// ConnectionStates
//________________________________________________________________________________
export const ConnectionStates = {
    IDLE: 0,
    CONNECTING: 1,
    CONNECTED: 2,
    RETRYING: 3,
    ERROR: 4,
    CLOSED: 5,
    AUTHENTICATING: 6,
    INVALID_CREDENTIALS: 7,
    OFFLINE: 8,
    ONLINE: 9
};

export const ConnectionLookUp = [
    'IDLE',
    'CONNECTING',
    'CONNECTED',
    'RETRYING',
    'ERROR',
    'CLOSED',
    'AUTHENTICATING',
    'INVALID CREDENTIALS',
    'OFFLINE',
    'ONLINE'
];



//________________________________________________________________________________
// Action
//________________________________________________________________________________
export const Action = {
    TEST: "TEST",
    KAFKA: "KAFKA",
    DISCONNECT: "DISCONNECT",
    AUTH: "AUTH"
}

//________________________________________________________________________________
// Code
//________________________________________________________________________________
export const Code = {
    LREQ: "LREQ", // last request
    REQ: "REQ",
    FACK: "FACK", // last acknowledge
    ACK: "ACK",
    LACK: "LACK", // last acknowledge
    ERROR: "ERROR"
}

//________________________________________________________________________________
// Message
//________________________________________________________________________________
export const Messages = {
    CONNECT_FAILED: "Can't connect to server"
}


//________________________________________________________________________________
// Helper Methods
//________________________________________________________________________________

export function generateRandomInt32() {
    return Math.floor(Math.random() * (1 << 31))
}


export function buildAuthenticationRequest(username, password) {
    return new WsPacket({
        action: Action.AUTH,
        payload: `${username} ${password}`,
        id: generateRandomInt32(),
        code: Code.LREQ
    })
}

export function buildDisconnectRequest() {
    let body = ""
    return this.packet(body, generateRandomInt32(), Action.DISCONNECT, Code.LREQ)
}


export async function authenticate(store) {
    await store.dispatch("websocket/connect")
    const aAuthDTO = new AuthDTO("username", "password")
    const result = await store.dispatch("websocket/authenticate", aAuthDTO)
    return result.value
}

export async function connect(dispatch) {
    // try {
      await dispatch("websocket/connect", null, {root:true});
    //   const aAuthDTO = new AuthDTO(state.username, state.password);
      await dispatch("websocket/authenticate", null, {root:true});
    //   dispatch("notifyGood", "Login was successful", {root:true});
    //   await dispatch("crud/initAllEndpoints", null, {root:true});
    // } catch (error) {
    //   dispatch("notifyBad", error, {root:true});
    // }
    return true
  }
  