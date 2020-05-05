<!-- ________________________________________________________________________________ --> 
<!-- TEMPLATE-->
<!-- ________________________________________________________________________________ -->
<template>
  <div class="connection">
    <div class="item">
      <h1>Connection State: {{statusAsString}}</h1>
      <x-input label="Host" v-model="hostname"></x-input>
      <x-input label="Port" v-model="port"></x-input>
      <x-input label="Endpoint" v-model="endpoint"></x-input>

      <h3>URL: {{url}}</h3>

      <h3>Authenticated: {{isAuthenticated}}</h3>

      <button v-if="!isAuthenticated" @click="connect()">Connect</button>
      <button v-if="isAuthenticated" @click="disconnect()">Disconnect</button>
    </div>
    <!-- <div class="item"> <button>test</button> </div> -->
  </div>
</template>

<!-- ________________________________________________________________________________ --> 
<!-- SCRIPT -->
<!-- ________________________________________________________________________________ -->
<script>
import { takeState } from "vuex-dot";
import { mapGetters } from "vuex";
import { connect } from "@/websocket/ClientUtils.js";

// import zConnection from "@/websocket/components/zConnection.vue";

//--------------------------------------------------------------------------------------
// Default
//--------------------------------------------------------------------------------------
export default {
  components: {
    // zConnection
  },
  name: "connection",
  //--------------------------------------------------------------------------------------
  // DATA
  //--------------------------------------------------------------------------------------
  data: function() {
    return {
      main_bg: "#fff",
      main_color: "#000",
      nav_bar_bg: "#08090e",
      nav_bar_box: "#cc6b5a",

      navigation: "home",
      xx: "wtf",
      username: "defaultUser",
      password: "defaultPw"
    };
  },
  //--------------------------------------------------------------------------------------
  // METHODS
  //--------------------------------------------------------------------------------------
  methods: {
    async connect() {
      try {
        connect(this.$store.dispatch, this.$store.state);
        this.$store.dispatch("notifyGood", "Login was successful", {root:true});
      } catch(e) {
        this.$store.dispatch("notifyBad", e, {root:true});
      }
    },
    async disconnect() {
      console.log("Disconnect is not implemented")
      this.$store.dispatch("notifyBad", "Disconnect is not implemented", {root:true});
    },

    test() {
      console.log("test clicked");
      var html = document.getElementsByTagName("html")[0];
      html.setAttribute("style", "--nav-bar-bg: green");
    }
  },
  //--------------------------------------------------------------------------------------
  // COMPUTED
  //--------------------------------------------------------------------------------------
  // https://github.com/yarsky-tgz/vuex-dot
  computed: {
    globalConfig() {
      return this.$store.getters["globalConfig"];
    },
    hostname: takeState("websocket.hostname")
      .commit("websocket/hostname")
      .map(),
    port: takeState("websocket.port")
      .commit("websocket/port")
      .map(),
    endpoint: takeState("websocket.endpoint")
      .commit("websocket/endpoint")
      .map(),
    ...mapGetters("websocket", ["statusAsString", "url", "isAuthenticated"])
  },
  //--------------------------------------------------------------------------------------
  // MOUNTED
  //--------------------------------------------------------------------------------------
  mounted() {}
};
</script>

<!-- ________________________________________________________________________________ --> 
<!-- STYLE -->
<!-- ________________________________________________________________________________ -->
<style scoped>
.connection {
  display: flex;
  align-items: center;
  justify-content: center;
  /* justify-content: space-between; */
  /* border: 1px solid black; */
  min-height: 50vh;
  flex-direction: column;
  margin-top: 1vh;
}

.item {
  background-color: #e2e5dc;
  padding-top:15px;
  padding-bottom:15px;
  padding-left:35px;
  padding-right:35px;
  /* border: 1px solid red; */
}
</style>