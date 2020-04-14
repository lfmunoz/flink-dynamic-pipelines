/****************************************************************************************
 *  Store Module - Connection
 ****************************************************************************************/
import * as logger from 'loglevel'
import WsClient from '@/websocket/WsClient.js'
import { ConnectionStates } from "@/websocket/ClientUtils.js"
import { ConnectionLookUp } from "@/websocket/ClientUtils.js"
import { buildAuthenticationRequest } from "@/websocket/ClientUtils.js"
import { okResult, nokResult} from '@/Constants.js'
import Constants from '@/Constants.js'
import Vue from 'vue'
import { Code, connect } from "@/websocket/ClientUtils.js"

// import AuthDTO from "@/websocket/AuthDTO.js";


const MSG_HISTORY = 5

const LOG_LEVEL = Constants.logLevel
const LOG_HEADER = 'ConnectionStore'
let LOG = logger.getLogger(LOG_HEADER)
LOG.setLevel(LOG_LEVEL)
LOG.info(`[${LOG_HEADER}] - Loading`)

const aWsClient = new WsClient()
var statusSubscription = null


function registerStatus(commit) {
  if (statusSubscription !== null) return
  statusSubscription = aWsClient.status()
    .subscribe(status => {
      commit('connState', status)
    })
}


function responseWrapper(startTime, response) {
  return {
    t: Date.now() - startTime,
    msg: response
  }
}


function logSendAndReceiveMessages(state, commit, obs$, aWsPacket) {
  let startTime = Date.now()
  commit('START_MSG', {t: startTime, msg: aWsPacket})
  const historyObj = state.msg_history[0]
  obs$.subscribe(resp => {
    if (resp.code === Code.ACK) {
      commit('ACK_MSG', { obj: responseWrapper(startTime, resp), historyObj: historyObj })
    } else if (resp.code === Code.FACK) {
      commit('FACK_MSG', { obj: responseWrapper(startTime, resp), historyObj: historyObj })
    } else if (resp.code === Code.LACK) {
      commit('LACK_MSG', { obj: responseWrapper(startTime, resp), historyObj: historyObj })
    } else if (resp.code === Code.ERROR) {
      commit('ERROR_MSG', { obj: responseWrapper(startTime, resp), historyObj: historyObj })
    }
  })
}

function buildHistoryArray(size) {
  var arr = [];
  for (var i = 0; i < size; ++i) {
    arr[i] = createHistoryObj(i)
  }
  return arr;
}

function createHistoryObj(idx) {
  let startTime = Date.now()
  return {
    id: idx,
    start: {t: startTime, msg: ''},
    fack:  responseWrapper(startTime, ''),
    error:  responseWrapper(startTime, ''),
    ack: [],
    lack:  responseWrapper(startTime, ''),
  }
}



//________________________________________________________________________________
// STATE
//________________________________________________________________________________
const state = {
  connState: ConnectionStates.IDLE,
  connStateString: ConnectionLookUp[ConnectionStates.IDLE],
  hostname: "localhost",
  port: "1991",
  endpoint: "socket",

  username: "defaultUser",
  password: "defaultPw",


  msg_START: null,
  msg_FACK: null,
  msg_LACK: null,
  msg_ERROR: null,
  msg_ACK: null,

  msg_history_idx: 0,
  msg_history: buildHistoryArray(MSG_HISTORY),





  wsPackets: {}
}

//________________________________________________________________________________
// GETTERS
//________________________________________________________________________________
const getters = {

  wsReply: (state ) => (id) => {
    return state.wsPackets[id]
  },

  status(state) {
    return state.connState
  },

  statusAsString(state) {
    return ConnectionLookUp[state.connState]
  },

  url(state) {
    return `ws://${state.hostname}:${state.port}/${state.endpoint}`;
  },

  isAuthenticated(state) {
    if (state.connState === ConnectionStates.ONLINE) {
      return true
    }
    return false
  }

}

//________________________________________________________________________________
// MUTATIONS
//________________________________________________________________________________
const mutations = {
  FACK(state, obj) {
    state.msg_FACK = obj
  },
  LACK(state, obj) {
    state.msg_LACK = obj
  },
  ERROR(state, obj) {
    state.msg_ERROR = obj
  },
  ACK(state, obj) {
    state.msg_ACK.push(obj)
  },
  CLEAR(state, start) {
    state.msg_START = start
    state.msg_FACK = null
    state.msg_LACK = null
    state.msg_ERROR = null
    state.msg_ACK = []
  },


  START_MSG(state, obj) {
    // console.log("history idx= ", idx)
    // console.log(obj)
    state.msg_history.pop()
    state.msg_history.unshift(createHistoryObj(obj.msg.id))
    Vue.set(state.msg_history[0], 'id', obj.msg.id)
    Vue.set(state.msg_history[0], 'start', obj)
    Vue.set(state.msg_history[0], 'fack', null)
    Vue.set(state.msg_history[0], 'lack', null)
    Vue.set(state.msg_history[0], 'ack', [])
    Vue.set(state.msg_history[0], 'error', null)
    // state.msg_history_idx = idx + 1
  },

  FACK_MSG(state, { obj, historyObj }) {
    Vue.set(historyObj, 'fack', obj)
  },
  ACK_MSG(state, { obj, historyObj }) {
    historyObj['ack'].push(obj)
  },
  LACK_MSG(state, { obj, historyObj }) {
    Vue.set(historyObj, 'lack', obj)
  },
  ERROR_MSG(state, { obj, historyObj }) {
    Vue.set(historyObj, 'error', obj)
  },




  // LACK(state, aWsPacket) {
  // Vue.set(state.wsPackets[aWsPacket.id], 1, aWsPacket)
  // },
  // FACK(state, aWsPacket) {
  // const replyList = []
  // replyList.push(aWsPacket)
  // replyList.push(null)
  // Vue.set(state.wsPackets, aWsPacket.id, replyList)
  // },

  connState(state, value) {
    state.connState = value
  },

  hostname(state, hostname) {
    state.hostname = hostname
  },

  port(state, port) {
    state.port = port
  },

  endpoint(state, endpoint) {
    state.endpoint = endpoint
  },

}

//________________________________________________________________________________
// ACTIONS
//________________________________________________________________________________
const actions = {

  async connect({ commit, getters }) {
    // LOG.debug(`[${LOG_HEADER}] - connect() ${getters.statusAsString}`)
    if (getters.isAuthenticated === true) throw "Already authenticated"
    registerStatus(commit)
    try {
      await aWsClient.connect(getters.url)
      return okResult("OK")
    } catch (error) {
      return nokResult(error.message)
    }
  },

  async authenticate({ state, commit }) {
    LOG.debug(`[${LOG_HEADER}] - authenticate()`);
    // LOG.debug(`[${LOG_HEADER}] - authenticate() ${state.username} / ${state.password}`)
    commit('connState', ConnectionStates.AUTHENTICATING)
    let authWsPacket = buildAuthenticationRequest(state.username, state.password)
    try {
      let result = await aWsClient.sendAndGetPromise(authWsPacket)
      LOG.debug(`[${LOG_HEADER}] - authenticate result -  ${JSON.stringify(result)}`)
      commit('connState', ConnectionStates.ONLINE)
      return okResult("OK")
    } catch (error) {
      return nokResult(error.message)
    }
  },

  // Fire and forget
  async send(context, aWsPacket) {
    LOG.debug(`[${LOG_HEADER}] - send - ${JSON.stringify(aWsPacket)}`)
    aWsClient.send(aWsPacket)
  },

  async sendAndGetPromise(context, aWsPacket) {
    LOG.debug(`[${LOG_HEADER}] - sendAndGetPromise - ${JSON.stringify(aWsPacket)}`)
    return aWsClient.sendAndGetPromise(aWsPacket)
  },

  async sendAndGetObservable({ commit, state, dispatch, getters }, aWsPacket) {
    if (!getters.isAuthenticated) {
      await connect(dispatch, state)
    }
    LOG.debug(`[${LOG_HEADER}] - sendAndGetObservable - ${JSON.stringify(aWsPacket)}`)
    const obs$ = aWsClient.sendAndGetObservable(aWsPacket)
    logSendAndReceiveMessages(state, commit, obs$, aWsPacket)
    // obs$.subscribe( resp => {
    // console.log(resp)
    // })

    return obs$
  },

}

//________________________________________________________________________________
// export
//________________________________________________________________________________
export default {
  namespaced: true,
  state,
  mutations,
  actions,
  getters
}
