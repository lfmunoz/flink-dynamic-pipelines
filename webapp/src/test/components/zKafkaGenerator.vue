<!-- ________________________________________________________________________________ --> 
<!-- TEMPLATE-->
<!-- ________________________________________________________________________________ -->
<template>
  <div class="kafkaGenerator">
    <h1>Kafka Generator</h1>

    <div class="status">
      {{statusUpdated}}
      <x-label label="Is Producing" :value="statusObj.isProducing" />
      <x-label label="Is Sampling" :value="statusObj.isSampling" />
      <x-label label="Messages Received" :value="statusObj.messagesReceived" />
      <x-label label="Messages Sent" :value="statusObj.messagesSent" />
      <x-label label="Messages" :value="statusObj.message" />
    </div>


    <button @click="start">START</button>
    <button @click="stop">STOP</button>
    <button @click="sample">SAMPLE</button>
    <button @click="cancel">CANCEL</button>
    <button @click="status">STATUS</button>
    <!-- {{statusObj}} -->

    <button @click="debug">DEBUG</button>

    <div class="samples">
      {{samples}}
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

// const statusEnabled = "Status Enabled"
// const statusDisabled = "Status Disabled"

//--------------------------------------------------------------------------------------
// Default
//--------------------------------------------------------------------------------------
export default {
  name: "zKafkaGenerator",
  components: {},
  //--------------------------------------------------------------------------------------
  // DATA
  //--------------------------------------------------------------------------------------
  data: function() {
    return {
      stdin: "",
      statusUpdated: Date.now(),
      // statusEnabled: statusDisabled,
      statusObj: {},
      samples: [

      ]
    };
  },
  //--------------------------------------------------------------------------------------
  // METHODS
  //--------------------------------------------------------------------------------------
  methods: {
    debug() {
      console.log(this.samples)

    },
    updateStatus(status) {
      this.statusUpdate = Date.now();
      this.statusObj = status;
    },
    updateSamples(sample) {
      if(this.samples.length >= 10) {
        this.samples.shift()
        this.samples.push(sample)
      } else {
        this.samples.push(sample)
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
        console.log("subscribe resp");
        console.log(resp);
            const payload = JSON.parse(resp.payload);
        this.updateSamples(payload)
        // if (resp.code === Code.ACK) {
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
<style>
.test {
  border: 1px solid black;
}

.status {
  border: 1px solid green;
}

.samples {
  border: 1px solid blue;

}
</style>