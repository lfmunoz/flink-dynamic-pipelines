// ________________________________________________________________________________
// IMPORTS
// ________________________________________________________________________________
import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'



// ________________________________________________________________________________
// GLOBAL COMPONENTS
// ________________________________________________________________________________
import xInput from "@/components/global/xInput.vue";
import xLabel from "@/components/global/xLabel.vue";
import xTab from "@/components/global/xTab.vue";
import xThreeColumn from "@/components/global/xThreeColumn.vue";
import xTwoColumn from "@/components/global/xTwoColumn.vue";
import xSelect from "@/components/global/xSelect.vue";
import xAceEditor from "@/components/global/xAceEditor.js";
Vue.component('x-tab', xTab)
Vue.component('x-three-column', xThreeColumn)
Vue.component('x-input', xInput)
Vue.component('x-label', xLabel)
Vue.component('x-two-column', xTwoColumn)
Vue.component('x-select', xSelect)
Vue.component('x-ace-editor', xAceEditor)

// ________________________________________________________________________________
// SYSTEM COMPONENTS
// ________________________________________________________________________________
import AppNav from "@/components/system/AppNav.vue";
import zConnection from "@/websocket/components/zConnection.vue";
Vue.component('app-nav', AppNav)
Vue.component('z-connection', zConnection)

// ________________________________________________________________________________
// MAIN VUE INSTANCE
// ________________________________________________________________________________
Vue.config.productionTip = false

new Vue({
  router,
  store,
  render: h => h(App)
}).$mount('#app')
