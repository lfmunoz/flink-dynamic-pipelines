<template>
  <div class="xselect">
    <!-- <div class="label">{{label}}</div> -->
    <span class="label">{{label}}</span>
    <select :size="size" :value="value" @change="propagate($event.target.value)">
      <option
        :class="{'item-selected': value == obj.key}"
        v-for="obj in items"
        :key="obj.key"
        :value="obj.key"
      >{{obj.value}}</option>
    </select>
    <!-- {{value}} -->
  </div>
</template>



<script>
export default {
  name: "xSelect",
  data() {
    return {
      valueBackup: ""
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
    },
    label: {
      type: String
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
  mounted() {}
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

.xselect {
    text-align: left;
    margin-top: 0.5em;
}

.label {
    font-weight: 900;
    display: inline-block;
    min-width: 80px;
    text-align: right;
    padding-right: 1rem;
}
</style>