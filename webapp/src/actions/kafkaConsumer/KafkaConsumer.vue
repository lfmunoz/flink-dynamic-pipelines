<!-- ________________________________________________________________________________ --> 
<!-- TEMPLATE-->
<!-- ________________________________________________________________________________ -->
<template>
  <aside>
    <h1>Kafka Output</h1>

    <div>
      <div class="config" :class="{ 'dirty' : isDirty}">
        <button @click="readConfig">readConfig</button>
        <button @click="writeConfig">writeConfig</button>
        <x-label label="Lasted Updated" :value="lastUpdated" />

        <x-input label="BootStrap Server" v-model="config.kafkaConfig.bootstrapServer" />
        <x-input label="Topic" v-model="config.kafkaConfig.topic" />
        <x-input label="GroupId" v-model="config.kafkaConfig.groupId" />
        <x-input label="Compression" v-model="config.kafkaConfig.compression" />
        <x-input label="offset" v-model="config.kafkaConfig.offset" />
      </div>
      <div class="sample">
        <x-label label="Is Sampling" :value="config.isSampling" />
        <x-label label="Messages Received" :value="config.messagesReceived" />
        <button @click="start">START</button>
        <json-viewer expanded :expand-depth="3" :value="stdin" theme="json-theme" />
      </div>
    </div>

  </aside>
</template>

<!-- ________________________________________________________________________________ --> 
<!-- SCRIPT -->
<!-- ________________________________________________________________________________ -->
<script>
import { Code } from "@/websocket/ClientUtils.js";

import {
  buildKafkaReadConfig,
  buildKafkaWriteConfig,
  buildKafkaSample
} from "@/actions/kafkaConsumer/KafkaConsumerUtils.js";

// import zObjectView from "@/test/components/zObjectView.vue";
// import zJsonConfig from "@/test/components/zJsonConfig.vue";

// const statusEnabled = "Status Enabled"
// const statusDisabled = "Status Disabled"

var statusInterval = null;

function getStatusOnInterval(callback) {
  if (statusInterval != null) {
    clearTimeout(statusInterval);
    statusInterval = null;
  }
  statusInterval = setTimeout(() => {
    callback();
  }, 2000);
}

//--------------------------------------------------------------------------------------
// Default
//--------------------------------------------------------------------------------------
export default {
  name: "zKafkaGenerator",
  components: {
    // zObjectView
    // zJsonConfig
  },
  //--------------------------------------------------------------------------------------
  // DATA
  //--------------------------------------------------------------------------------------
  data: function() {
    return {
      config: {
        isSampling: String(false),
        messagesReceived: 0,
        kafkaConfig: {
          bootstrapServer: "localhost:9092",
          topic: "output-topic",
          groupId: "default-groupId",
          compression: "none", // none, lz4
          offset: "none" // latest, earliest, none(use zookeper)
        }
      },
      stdin: {},
      lastUpdated: Date.now(),
      isDirty: false
    };
  },
  //--------------------------------------------------------------------------------------
  // METHODS
  //--------------------------------------------------------------------------------------
  methods: {
    debug() {
      console.log(this.samples);
    },
    setConfig(config) {
      this.config.isSampling = String(config.isSampling);
      this.config.messagesReceived = config.messagesReceived;
      this.config.kafkaConfig.bootstrapServer =
        config.kafkaConfig.bootstrapServer;
      this.config.kafkaConfig.topic = config.kafkaConfig.topic;
      this.config.kafkaConfig.groupId = config.kafkaConfig.groupId;
      this.config.kafkaConfig.components = config.kafkaConfig.components;
      this.config.kafkaConfig.offset = config.kafkaConfig.offset;
      this.$nextTick(() => {
        this.isDirty = false;
      });
    },

    async readConfig() {
      const aWsPacket = buildKafkaReadConfig();
      const result = await this.sendAndReceive(aWsPacket);
      this.setConfig(result);
      // console.log(result)
      this.lastUpdated = Date.now();
    },
    async writeConfig() {
      const aWsPacket = buildKafkaWriteConfig(this.config);
      const result = await this.sendAndReceive(aWsPacket);
      console.log(result)
      this.setConfig(result);
      this.lastUpdated = Date.now();
    },

    async start() {
      const aWsPacket = buildKafkaSample(1000);
      const obs$ = await this.$store.dispatch(
        "websocket/sendAndGetObservable",
        aWsPacket
      );
      obs$.subscribe(resp => {
        console.log("subscribe resp");
        console.log(resp);
        if (resp.code === Code.ACK) {
          const payload = JSON.parse(resp.payload);
          const body = JSON.parse(payload.body);
           if(Object.keys(body).length > 0 ) {
            this.stdin = body
          }
        }
      });
    },
    // ________________________________________________________________________________
    // HELPER METHODS
    // ________________________________________________________________________________
    statusOnInterval() {
      getStatusOnInterval(this.status);
    },

    async sendAndReceive(aWsPacket) {
      const obs$ = await this.$store.dispatch(
        "websocket/sendAndGetObservable",
        aWsPacket
      );
      return new Promise((resolve, reject) => {
        obs$.subscribe(resp => {
          // console.log("subscribe resp:")
          // console.log(resp)
          if (resp.code === Code.ACK) {
            const payload = JSON.parse(resp.payload);
            const body = JSON.parse(payload.body);
            resolve(body)
            // } else if (resp.code === Code.FACK) {
            // } else if (resp.code === Code.LACK) {
          } else if (resp.code === Code.ERROR) {
            reject("NOK");
          }
        });
      });
    },

   
  },
  //--------------------------------------------------------------------------------------
  // WATCH
  //--------------------------------------------------------------------------------------
  watch: {
    config: {
      handler(val) {
        console.log("kafka producer config change");
        console.log(val);
        this.isDirty = true;
        // do stuff
      },
      deep: true
    }
  },
  //--------------------------------------------------------------------------------------
  // COMPUTED
  //--------------------------------------------------------------------------------------
  computed: {
    aggregateSample() {
      if (this.samples.length > 0) {
        return this.samples[0].values;
      } else {
        return {};
      }
    }
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
aside {
  /* border: 4px solid purple; */
    padding: 10px;
}
.config {
  border: 2px solid green;
  padding: 10px;
}
.sample{
  border: 2px solid lightcoral;
  padding: 10px;
}

.dirty {
  border-left: 5px solid red;
}
</style>