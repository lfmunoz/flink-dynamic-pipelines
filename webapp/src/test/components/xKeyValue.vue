<!-- ________________________________________________________________________________ --> 
<!-- TEMPLATE-->
<!-- ________________________________________________________________________________ -->
<template>
  <div class="xkeyValue">
    <div class="group">
      <button class="add-button" @click="add">ADD</button>
      length: {{length}}
    </div>
    <div v-for="(v, k) in values" :key="k">
      {{v.uniqueId}}
        <x-input label="Name" :value="v.name" />
      <button @click="v.selected = !v.selected">Selected:{{v.selected}}</button>
      <div v-if="v.selected">
        <x-ace-editor height="100" :view="v.script" />
      </div>
    </div>
  </div>
</template>


<!-- ________________________________________________________________________________ --> 
<!-- SCRIPT -->
<!-- ________________________________________________________________________________ -->
<script>
// import Vue from "vue";

function buildObject() {
  return {
    uniqueId: 0,
    name: "Default",
    script: "// TODO ",
    selected: false
  };
}

export default {
  name: "xKeyValue",
  components: {},
  /*
  props: {
    value: {
      type: Object,
      default() {
        return {
          // outDefaultKey: "ourDefaultValue"
        };
      }
    }
  },
  */
  //--------------------------------------------------------------------------------------
  // DATA
  //--------------------------------------------------------------------------------------
  data() {
    return {
      values: []
    };
  },
  //--------------------------------------------------------------------------------------
  // METHODS
  //--------------------------------------------------------------------------------------
  methods: {
    add() {
      //   const newKey = "x" + this.length.toString();
      //   Vue.set(this.value, newKey, "defaultVal");
      const obj = buildObject();
      obj.uniqueId = this.length + 1;
      this.values.push(obj);
    }
  },
  watch: {},
  //--------------------------------------------------------------------------------------
  // COMPUTED
  //--------------------------------------------------------------------------------------
  computed: {
    length() {
      return this.values.length;
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
.group {
  /* text-align: right; */
  /* margin-left: 309px; */
  /* border: 1px solid green; */
  /* width:100%; */
  text-align: left;
}

.add-button {
  background-color: black;
  color: yellowgreen;
  margin-left: 20px;
}

/* .xinput { border: 1px solid blue; } */
/* .xkeyValue { border: 1px solid red; } */
</style>