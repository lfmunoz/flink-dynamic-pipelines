<!-- ________________________________________________________________________________ --> 
<!-- TEMPLATE-->
<!-- ________________________________________________________________________________ -->
<template>
  <div class="kafkaGenerator">
    <h1>Kafka Generator</h1>

    <div class="config">
      <button @click="start">START</button>
      <button @click="stop">STOP</button>
      <x-input label="Kafka Topic" value="5" />
      <x-input label="Kafka Bootstrap Server" value="5" />

      <x-input label="Messages Per Second" value="5" />
      
      <!-- <z-json-config /> -->
    </div>

    <div class="sample-view">
      <button @click="sample">SAMPLE</button>
      <button @click="cancel">CANCEL</button>
      <x-input label="Sample Interval" value="5" />
    </div>

    <div class="aggregate">
      <z-object-view :values="aggregateSample" />
    </div>

    <!-- {{statusObj}} -->

    <div class="debug">
      <button @click="debug">DEBUG</button>

      <div class="samples">{{samples}}</div>
    </div>
  </div>
</template>

<!-- ________________________________________________________________________________ --> 
<!-- SCRIPT -->
<!-- ________________________________________________________________________________ -->
<script>
import {
  buildDefaultKafkaAction,
  KafkaActionType,
  createWsPacketForKafka
} from "@/test/TestUtils.js";

import { Code } from "@/websocket/ClientUtils.js";
import zObjectView from "@/test/components/zObjectView.vue";
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
    zObjectView,
    // zJsonConfig
  },
  //--------------------------------------------------------------------------------------
  // DATA
  //--------------------------------------------------------------------------------------
  data: function() {
    return {
      stdin: "",
      statusUpdated: Date.now(),
      // statusEnabled: statusDisabled,
      statusObj: {},
      samples: []
    };
  },
  //--------------------------------------------------------------------------------------
  // METHODS
  //--------------------------------------------------------------------------------------
  methods: {
    debug() {
      console.log(this.samples);
    },
    updateStatus(status) {
      this.statusUpdate = Date.now();
      this.statusObj = status;
    },
    updateSamples(sample) {
      if (this.samples.length >= 10) {
        this.samples.shift();
        this.samples.push(sample);
      } else {
        this.samples.push(sample);
      }
    },
    async start() {
      const kafkaAction = buildDefaultKafkaAction();
      kafkaAction.type = KafkaActionType.START;
      const aWsPacket = createWsPacketForKafka(kafkaAction);
      const kafkaStatus = await this.sendAndReceiveKafkaActionDTO(aWsPacket);
      this.updateStatus(kafkaStatus);
    },

    async stop() {
      const kafkaAction = buildDefaultKafkaAction();
      kafkaAction.type = KafkaActionType.STOP;
      const aWsPacket = createWsPacketForKafka(kafkaAction);
      const kafkaStatus = await this.sendAndReceiveKafkaActionDTO(aWsPacket);
      this.updateStatus(kafkaStatus);
    },

    async sample() {
      const kafkaAction = buildDefaultKafkaAction();
      kafkaAction.type = KafkaActionType.SAMPLE;
      const aWsPacket = createWsPacketForKafka(kafkaAction);
      const obs$ = await this.$store.dispatch(
        "websocket/sendAndGetObservable",
        aWsPacket
      );
      obs$.subscribe(resp => {
        // console.log("subscribe resp");
        // console.log(resp);
        if (resp.code === Code.ACK) {
          const payload = JSON.parse(resp.payload);
          this.updateSamples(payload);
        }
        // const payload = JSON.parse(resp.payload)
        // const body = JSON.parse(payload.body)
        // resolve(body)
        // } else if (resp.code === Code.FACK) {
        // } else if (resp.code === Code.LACK) {
        // } else if (resp.code === Code.ERROR) {
        // }
      });
    },

    async cancel() {
      const kafkaAction = buildDefaultKafkaAction();
      kafkaAction.type = KafkaActionType.CANCEL;
      const aWsPacket = createWsPacketForKafka(kafkaAction);
      const kafkaStatus = await this.sendAndReceiveKafkaActionDTO(aWsPacket);
      this.updateStatus(kafkaStatus);
    },

    async status() {
      const kafkaAction = buildDefaultKafkaAction();
      kafkaAction.type = KafkaActionType.STATUS;
      const aWsPacket = createWsPacketForKafka(kafkaAction);
      const kafkaStatus = await this.sendAndReceiveKafkaActionDTO(aWsPacket);
      this.updateStatus(kafkaStatus);
    },

    // ________________________________________________________________________________
    // HELPER METHODS
    // ________________________________________________________________________________
    statusOnInterval() {
      getStatusOnInterval(this.status);
    },

    async sendAndReceiveKafkaActionDTO(aWsPacket) {
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
            resolve(body);
            // } else if (resp.code === Code.FACK) {
            // } else if (resp.code === Code.LACK) {
          } else if (resp.code === Code.ERROR) {
            reject("NOK");
          }
        });
      });
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
<style>
.kafkaGenerator {
  border: 4px solid orange;
}

.config {
  border: 4px solid lightblue;
  margin: 10px;
}

.sample-view {
  border: 4px solid lightseagreen;
  margin: 10px;
}

.debug {
  padding-top: 170px;
}


.test {
  border: 1px solid black;
}

.samples {
  border: 1px solid blue;
}
</style>