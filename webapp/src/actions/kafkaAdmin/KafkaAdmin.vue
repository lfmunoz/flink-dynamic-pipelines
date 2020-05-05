<!-- ________________________________________________________________________________ --> 
<!-- TEMPLATE-->
<!-- ________________________________________________________________________________ -->
<template>
  <aside>
    <h1>Kafka Admin</h1>
    <div class="status">
        <x-label label="Lasted Updated" :value="lastUpdated" />
    <x-input label="Bootstrap Server" v-model="bootstrapServer" />
        <x-input label="Zookeeper URL" v-model="zooKeeper" />

      <button @click="topics">TOPICS</button>
      <button @click="consumers">CONSUMERS</button>
      <button @click="clear">CLEAR</button>
      <x-ace-editor height="400" v-model="stdin" />
    </div>
  </aside>
</template>

<!-- ________________________________________________________________________________ --> 
<!-- SCRIPT -->
<!-- ________________________________________________________________________________ -->
<script>
// import zCommand from "@/test/components/zCommand.vue"
// import zKafkaGenerator from "@/test/components/zKafkaGenerator.vue"
// import xKeyValue from "@/test/components/xKeyValue.vue"
import {
  buildTopicsKafkaAdminPkt,
  buildConsumersKafkaAdminPkt
} from "@/actions/kafkaAdmin/KafkaAdminUtils.js";

import { Code } from "@/websocket/ClientUtils.js";

//--------------------------------------------------------------------------------------
// Default
//--------------------------------------------------------------------------------------
export default {
  name: "test",
  components: {
    // zCommand,
    // zKafkaGenerator,
    // xKeyValue
  },
  //--------------------------------------------------------------------------------------
  // DATA
  //--------------------------------------------------------------------------------------
  data: function() {
    return {
      bootstrapServer: "localhost:9092",
      zooKeeper: "localhost:2180",
      lastUpdated: Date.now(),
      stdin: ""
    };
  },
  //--------------------------------------------------------------------------------------
  // METHODS
  //--------------------------------------------------------------------------------------
  methods: {

    clear() {
      this.stdin = ""
    },




    async topics() {
      this.clear()
      const aWsPacket = buildTopicsKafkaAdminPkt();
      const obs$ = await this.$store.dispatch(
        "websocket/sendAndGetObservable",
        aWsPacket
      );
      obs$.subscribe(resp => {
        if (resp.code === Code.ACK) {
          const payload = JSON.parse(resp.payload);
          this.stdin = `${this.stdin}\n${payload.body}`
        }
      });
    },

    async consumers() {
      this.clear()
      const aWsPacket = buildConsumersKafkaAdminPkt();
      const obs$ = await this.$store.dispatch(
        "websocket/sendAndGetObservable",
        aWsPacket
      );
      obs$.subscribe(resp => {
        if (resp.code === Code.ACK) {
          const payload = JSON.parse(resp.payload);
          this.stdin = `${this.stdin}\n${payload.body}`
        }
      });
    },

  },
  //--------------------------------------------------------------------------------------
  // COMPUTED
  //--------------------------------------------------------------------------------------
  computed: {},
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
aside {
  border: 5px solid black;
   padding: 10px;
}
</style>