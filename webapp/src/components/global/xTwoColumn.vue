<template>
  <div class="twoColumn">
    <div class="group" v-if="!hide">
      <button style="margin-left: 4px" @click="toggleHide">Hide </button>
      <div class="groupLabel">Column Size</div>
      <div>
        <input @change="changeValue(1)" type="radio" id="50" name="layout" value="50"  />
        <label for="50">&frac13;</label>
      </div>
      <div>
        <input @change="changeValue(2)" type="radio" id="66" name="layout" value="66" checked />
        <label for="66">&frac12;</label>
      </div>
      <div>
        <input @change="changeValue(3)" type="radio" id="75" name="layout" value="75" />
        <label for="75">&frac23;</label>
      </div>
      <div>
        <input @change="changeValue(4)" type="radio" id="100" name="layout" value="100" />
        <label for="100">1</label>
      </div>

      <div class="groupLabel" style="margin-left: 40px">Three Columns</div>
      <div>
        <input @change="setThreeColumn(true)" type="radio" id="true" name="threeCol" value="true" />
        <label for="true">True</label>
      </div>
      <div>
        <input @change="setThreeColumn(false)" type="radio" id="false" name="threeCol" value="false" checked />
        <label for="false">False</label>
      </div>

      <button style="margin-left: 40px">Save</button>
    </div>
    <div class="group" v-else>
      <button style="margin-left: 4px" @click="toggleHide">Adjust Layout</button>
    </div>


    <div class="flex">
      <!-- COLUMN RIGHT -->
      <div class="column-main" :style="{flex: flexForLeft}">
        <slot name="left"></slot>
      </div>
      <!-- COLUMN LEFT -->
      <div class="column-sidebar" :style="{display: display }">
        <slot name="right"></slot>
      </div>
      <div class="column-sidebar" :style="{display: display }" v-if="threeColumn">
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
  name: "xTwoColumn",
  props: {

  },
  data() {
    return {
      flexForLeft: 2,
      display: "block",
      threeColumn: false, 
      hide: false,
    };
  },
  methods: {
    changeValue(newValue) {
      if(newValue === 4)  {
        this.display = "none"
        return
      }
      this.display = "block"
      if(newValue === 1) this.flexForLeft = 1
      if(newValue === 2) this.flexForLeft = 2
      if(newValue === 3) this.flexForLeft = 3

    },
    setThreeColumn(newValue) {
      console.log("set three column " + newValue)
      this.threeColumn = newValue
    },
    toggleHide() {
      this.hide = !this.hide
    }
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

.column-main {
  flex: 1;
  /* border: 2px solid blue; */
}

.column-sidebar {
  flex: 1;
  /* border: 2px solid green; */
  display: flex;
  flex-direction: column;
}
</style>
