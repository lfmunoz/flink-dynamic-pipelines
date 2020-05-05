<!-- ________________________________________________________________________________ --> 
<!-- TEMPLATE-->
<!-- ________________________________________________________________________________ -->
<template>
  <aside>
    <h1>Flink Mapper</h1>
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

      <!-- <div class="sample"> -->
      <!-- <x-ace-editor height="500" v-model="stdin" /> -->
      <!-- </div> -->
      <x-input label="NEW KEY" v-model="newKey" />
      <button @click="newMapping">CREATE</button>
      <div v-for="(value, key) in fullConfig" :key="key" class="lambda">
        <x-toggle-view :label="key">
          <button @click="removeMapping(key)">DELETE</button>
          <x-ace-editor height="500" v-model="fullConfig[key]" />
        </x-toggle-view>
      </div>
    </div>

    <x-toggle-view label="SEND AND VIEW CONFIGURATION" class="config-view">
      <button @click="sendMapper">SEND TO FLINK</button>
      <json-viewer :value="fullConfig" theme="json-theme" />
    </x-toggle-view>
  </aside>
</template>

<!-- ________________________________________________________________________________ --> 
<!-- SCRIPT -->
<!-- ________________________________________________________________________________ -->
<script>
import { Code } from "@/websocket/ClientUtils.js";
import {
  buildKafkaSendMapper,
  buildKafkaReadConfig,
  buildKafkaWriteConfig
} from "@/actions/mapper/MapperUtils.js";

const defaultLambda = `
  override open fun main(m: MonitorMessage) : MapperResult  {
    return MapperResult(values= mutableMapOf("two" to "2"))
  } 
`;

//--------------------------------------------------------------------------------------
// Default
//--------------------------------------------------------------------------------------
export default {
  name: "mapper",
  components: {
    // zLambda
    // zObjectView
    // zJsonConfig
  },
  //--------------------------------------------------------------------------------------
  // DATA
  //--------------------------------------------------------------------------------------
  data: function() {
    return {
      config: {
        offset: 0,
        lastMessage: "",
        kafkaConfig: {
          bootstrapServer: "localhost:9092",
          topic: "mapper-topic",
          groupId: "default-groupId",
          compression: "none", // none, lz4
          offset: "none" // latest, earliest, none(use zookeper)
        }
      },
      fullConfig: {},
      newKey: `randomKey${Date.now()}`,
      lastUpdated: Date.now(),
      isDirty: false
    };
  },
  //--------------------------------------------------------------------------------------
  // METHODS
  //--------------------------------------------------------------------------------------
  methods: {
    newMapping() {
      this.$set(this.fullConfig, this.newKey, defaultLambda);
      this.newKey = `randomKey${Date.now()}`;
    },
    removeMapping(key) {
      this.$delete(this.fullConfig, key);
    },

    setConfig(config) {
      this.config.offset = String(config.offset);
      this.config.lastMessage = config.lastMessage;
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
      console.log(result)
      this.lastUpdated = Date.now();
    },
    async writeConfig() {
      const aWsPacket = buildKafkaWriteConfig(this.config);
      const result = await this.sendAndReceive(aWsPacket);
      // this.setConfig(result);
      console.log(result);
      this.lastUpdated = Date.now();
    },

    async sendMapper() {
      const aWsPacket = buildKafkaSendMapper(this.fullConfig);
      const result = await this.sendAndReceive(aWsPacket);
      // this.setConfig(result);
      console.log(result);
      this.lastUpdated = Date.now();
    },

    // ________________________________________________________________________________
    // HELPER METHODS
    // ________________________________________________________________________________
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
            // const body = JSON.parse(payload.body);
            resolve(this.returnStringOrObject(payload.body));
            // } else if (resp.code === Code.FACK) {
            // } else if (resp.code === Code.LACK) {
          } else if (resp.code === Code.ERROR) {
            reject("NOK");
          }
        });
      });
    },

    returnStringOrObject(body) {
      try {
        return JSON.parse(body);
      } catch(e) {
        return body
      }
    }

  },



  //--------------------------------------------------------------------------------------
  // WATCH
  //--------------------------------------------------------------------------------------
  watch: {
    config: {
      handler() {
        // console.log("kafka producer config change");
        // console.log(val);
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
    stdout() {
      return JSON.stringify(this.fullConfig, null, 2).replace("\n//", "\n");
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
.lambda {
  margin-top: 5px;
  border: 2px solid black;
}

.config-view {
  margin-top: 5px;
  border: 2px solid magenta;
}

aside {
  /* border: 4px solid orange; */
  margin: 10px;
}

.config {
  border: 2px solid green;
   padding: 10px;
}


.dirty {
  border-left: 5px solid red;
}
</style>