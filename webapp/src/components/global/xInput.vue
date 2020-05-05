<template>
  <div class="xinput">
    <span class="label">{{label}}</span>
    <input class="value" :value="value" @keyup="propagate($event.target.value)" />
  </div>
</template>


<!-- ________________________________________________________________________________ --> 
<!-- SCRIPT -->
<!-- ________________________________________________________________________________ -->
<script>
export default {
  name: "xInput",
  data() { return { } },
  props: ['value', 'label'],
  methods: {
    propagate(eventValue) {
      // console.log(eventValue)
      this.$emit('input', eventValue)
    }
  }
};
</script>

<!-- ________________________________________________________________________________ --> 
<!-- STYLE -->
<!-- ________________________________________________________________________________ -->
<style scoped>

/* .label { border: 1px solid orange; } */
/* .xinput { border: 1px solid blue; }  */
/* .value { border: 1px solid blue; } */

/** X-INPUT COMPONENTS **/
div.xinput {
    text-align: left;
    margin-top: 0.5em;
}
div.xinput > .label  {
    font-weight: 900;
    display: inline-block;
    min-width: 80px;
    text-align: right;
    padding-right: 1rem;
    
}
div.xinput > .value {
    /* min-width: 80px; */
   width: 60%;

}

div.xinput span:after { 
    content: ":"; 
}



</style>
