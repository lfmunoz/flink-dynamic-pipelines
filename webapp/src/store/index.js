import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)


import ConnectionStore from '@/websocket/ConnectionStore.js'

import Constants from '@/Constants.js'
import {NotifyType} from '@/Constants.js'
import * as logger from 'loglevel';


const LOG_HEADER = 'StoreIndex'
const LOG = logger.getLogger(LOG_HEADER)
LOG.setLevel(Constants.logLevel)
LOG.info(`[${LOG_HEADER}] - Loading`)


//________________________________________________________________________________
// HELPER METHODs
//________________________________________________________________________________
var notifyTimer = null

function clearNotify(commit, state) {
  if(notifyTimer != null) {
      clearTimeout(notifyTimer)
      notifyTimer = null
  }
  notifyTimer = setTimeout(() => { 
      commit('notify', { "message": state.notifyMessage , "type": state.notifyType, "display": false})
  }, 2300)
}

//________________________________________________________________________________
// STATE
//________________________________________________________________________________
const state = {
  //Notify
  isNotify: false,
  notifyMessage: "Welcome",
  notifyType: NotifyType.BLANK,
}

//________________________________________________________________________________
// MUTATIONS
//________________________________________________________________________________
const mutations = {

  notify(state, obj) {
      let {message, type, display} = obj
      state.notifyMessage = message
      state.notifyType = type
      state.isNotify = display
  }

} 

//________________________________________________________________________________
// ACTIONS
//________________________________________________________________________________
const actions = {

  async notifyGood({ commit }, message) {
      LOG.debug(`[${LOG_HEADER}] - notifyGood():`, message)
      commit('notify', { "message": message, "type": NotifyType.GOOD, "display": true})
      clearNotify(commit, state)
  },

  async notifyBad({ commit }, message) {
      LOG.debug(`[${LOG_HEADER}] - notifyBad():`, message)
      commit('notify', { "message": message, "type": NotifyType.BAD, "display": true})
      clearNotify(commit, state)
  },


} 

//________________________________________________________________________________
// EXPORT
//________________________________________________________________________________
export default new Vuex.Store({
  state: state,
  mutations: mutations,
  actions: actions,
  modules: {
    websocket: ConnectionStore,
  }
})
