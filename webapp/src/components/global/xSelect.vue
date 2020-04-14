<template>
  <div class="xselect">
    <!-- <div class="label">{{label}}</div> -->
    <div>
      <select :size="size" :value="value" @change="propagate($event.target.value)">
        <option :class="{'item-selected': value == obj.key}" v-for="obj in items" :key="obj.key" :value="obj.key">{{obj.value}}</option>
      </select>
      <!-- {{value}} -->
    </div>
  </div>
</template>



<script>
export default {
  name: "xSelect",
  data() {
    return {
      valueBackup: "",
    };
  },
  props: {
    size: {
      type: String,
      default: "10"
    },
    value: {
      type: String
    },
    items: {
      type: Array
    }
  },
  methods: {
    propagate(eventValue) {
      // console.log(eventValue);
      this.$emit("input", eventValue);
    }
  },
  // watch: {
    // value(val) {
      // this.$emit("input", val); 
      // if(this.valueBackup !== val)
        // this.editor.setValue(val,1);
    // }
  // },
  mounted() {

  }
};
</script>



<style scoped>
.item-selected {

  color: red;
}

.label {
  margin-bottom: 0.5em;
}

select {
  min-width: 8em;
}
</style>