<template>
  <div class="xlabel">
    <span class="label">{{label}}</span>
    <span class="value">{{value}}</span>
  </div>
</template>


<!-- ________________________________________________________________________________ --> 
<!-- SCRIPT -->
<!-- ________________________________________________________________________________ -->
<script>
export default {
  name: "xlabel",
  props: ['value', "label"],
  data() { return { } },
  methods: { }
};
</script>

<!-- ________________________________________________________________________________ --> 
<!-- STYLE -->
<!-- ________________________________________________________________________________ -->
<style scoped>
/** X-LABEL COMPONENT **/
div.xlabel {
    text-align: left;
    margin-top: 0.5em;
}
div.xlabel > .label {
    font-weight: 900;
    display: inline-block;
    min-width: 80px;
    text-align: right;
    padding-right: 1rem;
}

div.xlabel > .value {
    text-align: left;
    display: inline-block;
    min-width: 80px;
}

div.xlabel .label:after { 
    content: ":"; 
}


</style>
