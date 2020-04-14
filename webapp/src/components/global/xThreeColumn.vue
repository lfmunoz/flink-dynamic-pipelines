<template>
  <div class="threeColumn">

    <div class="flex">
      <!-- COLUMN LEFT -->
      <div class="column-left" >
        <slot name="left"></slot>
      </div>
      <!-- COLUMN MIDDLE -->
      <div class="column-middle" >
        <slot name="middle"></slot>
      </div>
      <!-- COLUMN RIGHT-->
      <div class="column-right" >
        <slot name="right"></slot>
      </div>
    </div>
  </div>
</template>


<!-- ________________________________________________________________________________ --> 
<!-- SCRIPT -->
<!-- ________________________________________________________________________________ -->
<script>
export default {
  name: "xThreeColumn",
  data() {
    return {
    };
  },
  methods: {

  }
};
</script>

<!-- ________________________________________________________________________________ --> 
<!-- STYLE -->
<!-- ________________________________________________________________________________ -->
<style scoped>
.groupLabel {
  padding-top: 4px;
  font-weight: 900;
  padding-right: 8px;
  margin-left: 10px;

}

.large {
  font-size: 1.5rem;
  padding-right: 5px;
}

.group {
  display: flex;
  border-bottom: 1px solid black;
}

.flex {
  display: flex;
}

.flex > * + * {
  margin-top: 0;
  margin-left: 1.5em;
}

.column-left{ 
  /* border: 1px solid red; */
   /* width: min-content; */
  width: 20%

}
.column-middle{
  /* border: 1px solid red; */
  width: 50%;

 }
.column-right{ 
  /* border: 1px solid red; */
 width: 30%;
}

</style>
