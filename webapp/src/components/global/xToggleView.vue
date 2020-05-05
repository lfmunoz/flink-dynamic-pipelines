<template>
  <div class="toggle-view">
    <span>{{label}}</span>
    <button @click="toggle">{{buttonText}}</button>
    <div v-if="isShowing"> <slot /> </div>
  </div>
</template>


<!-- ________________________________________________________________________________ --> 
<!-- SCRIPT -->
<!-- ________________________________________________________________________________ -->
<script>
export default {
  name: "xToggleView",
  props: ["value", "label"],
  data() {
    return {
      isShowing: true,
      buttonText: "HIDE"
    };
  },
  methods: {
    toggle() {
      if (this.isShowing) {
        this.buttonText = "SHOW";
        this.isShowing = false;
        this.$emit("input", this.isShowing);
      } else {
        this.buttonText = "HIDE";
        this.isShowing = true;
        this.$emit("input", this.isShowing);
      }
    }
  },

};
</script>

<!-- ________________________________________________________________________________ --> 
<!-- STYLE -->
<!-- ________________________________________________________________________________ -->
<style scoped>
span {
  margin-right: 15px;
  font-weight: 900;
}
</style>
