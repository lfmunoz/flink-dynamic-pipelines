
<!-- ________________________________________________________________________________ --> 
<!-- TEMPLATE-->
<!-- ________________________________________________________________________________ -->
<template>
  <div class="tab">
    <!-- <hr /> -->

    <!-- maxHeight: {{maxHeight}} -->

    <!-- <hr /> -->
    <!-- <h1>xTab</h1> -->
    <!-- count: {{count}} -->
    <!-- {{active}} -->
    <nav>
      <div class="item" v-for="idx in count" :key="idx" @click="selectTab(idx)">
        <a :class="{'active': value[idx-1]}">{{titles[idx-1]}}</a>
      </div>
    </nav>

    <div v-for="idx in count" :key="idx">
      <div v-show="value[idx-1]">
      <slot :name="idx"></slot>
      </div>
    </div>
  </div>
</template>


<!-- ________________________________________________________________________________ --> 
<!-- SCRIPT -->
<!-- ________________________________________________________________________________ -->
<script>

//--------------------------------------------------------------------------------------
// Default
//--------------------------------------------------------------------------------------
export default {
  name: "xTab",
  props: {
    titles: {
      type: Array,
      required: true
    },
    value: {
      type: Array,
      default: function() {
        return []
      }
    }
  },
  //--------------------------------------------------------------------------------------
  // DATA
  //--------------------------------------------------------------------------------------
  data() {
    return { }
  },
  //--------------------------------------------------------------------------------------
  // METHODS
  //--------------------------------------------------------------------------------------
  methods: {
    selectTab(aNumber) {
      // console.log("selected tab " + aNumber);
      const newArr = this.newBlankArray()
      newArr[aNumber-1] = true
      // Vue.set(newArr, aNumber - 1, true)
      // this.value = newArr
      this.$emit("input", newArr)
    },

    newBlankArray() {
      const newArr = []
      for (let i = 0; i < this.count; i++) {
        newArr.push(false)
      }
      return newArr
    }
  },
  //--------------------------------------------------------------------------------------
  // Computed
  //--------------------------------------------------------------------------------------
  computed: {
    count() {
      return this.titles.length;
    }
  },
  //--------------------------------------------------------------------------------------
  // Mounted
  //--------------------------------------------------------------------------------------
  mounted() {

  }
};
</script>



<!-- ________________________________________________________________________________ --> 
<!-- STYLE -->
<!-- ________________________________________________________________________________ -->
<style scoped>

.item {
  display: inline-block;
  /* padding: 0.5em; */
  /* border: 1px solid black; */
}

.active {
  color: red;
}

nav > .item:after {
  content: "|";
}

nav > .item:last-child:after {
  content: "";
}

nav {
  margin-bottom: 10px;
}

.item > a {
  padding: 0.2em;
  font-weight: bold;
  /* color: #2c3e50; */
}

.item > a:link {
  color: blue;
  text-decoration: none;
}


.item > a:hover {
  text-decoration: underline;
}

.item > a:active {
  color: red;
}
</style>