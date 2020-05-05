<template>
  <div class="tree">
    <div class="top">
      <div :class="{folder: isFolder, active:   selectedId == item.id}" @click="toggle(item.id)" @dblclick="makeFolder">
        <span v-for="i in item.level" :key="i"><span v-if="i==1">├</span>─</span>
        <span v-show="isOpen && isFolder"><i class="fas fa-folder-open"></i>  </span>
        <span v-show="!isOpen && isFolder"><i class="fas fa-folder"></i>  </span>
        <span v-show="!isFolder"><i class="fas fa-file"></i>  </span>
        <!-- <i v-show="!isOpen" class="fas fa-folder-open"></i>   -->
        <div  v-if="!isFolder" class="name"  @click="$emit('select-item', item.id)">
          {{item.name}} 
        </div>
        <div v-if="isFolder" class="name" > {{ item.name }} </div>
        <!-- {{isOpen}} {{isFolder}} -->
        <!-- <span v-if="isFolder">[{{ isOpen ? '-' : '+' }}]</span> -->
         <!-- <span v-if="!isFolder" @click="$emit('add-item', item)">SELECT</span> -->
      </div>
      <div class="folder" v-show="isOpen" v-if="isFolder">
      <!-- <span>...</span> -->
        <x-tree
          class="item"
          v-for="(child, index) in item.children"
          :key="index"
          :item="child"
          :lookup="lookup"
          :selectedId="selectedId"
          @select-item="$emit('select-item', $event)"
          @select-folder="$emit('select-folder', $event)"
        />
      </div>
    </div>
  </div>
</template>


<!-- ________________________________________________________________________________ --> 
<!-- SCRIPT -->
<!-- ________________________________________________________________________________ -->
<script>
import { mapGetters, mapState } from "vuex";
import { buildNode, pathToArr, insertItem } from "@/task/TreeObj.js";
import Vue from 'vue'
export default {
  name: "xTree",
  props: {
    item: Object,
    lookup: Object,
    selectedId: String
  },
  data: function() {
    return {
      isOpen: true
    };
  },
  computed: {
    isFolder: function() {
      return this.item.isFolder
    },
  },
  methods: {
    toggle: function(id) {
      // console.log("toggle ", id)
      if (this.isFolder) {
      this.$emit('select-folder', id)
        this.isOpen = !this.isOpen;
      }
    },
    makeFolder: function() {
      if (!this.isFolder) {
        this.$emit("make-folder", this.item);
        this.isOpen = true;
      }
    },
  },
  mounted() { 
    if(this.item.isOpen != undefined) {
      this.isOpen = this.item.isOpen
    }

  }
};
</script>

<!-- ________________________________________________________________________________ --> 
<!-- STYLE -->
<!-- ________________________________________________________________________________ -->
<style scoped>

.id-text {

}


.name {
      min-width: 80%;
  display: inline-block;
  /* border: 1px solid blue; */
}

.name:hover {
  /* background-color: green; */
  color: black;
  /* font-weight: 900; */
  font-size: 1.1rem;
  /* border-bottom: 1px solid black; */
}

.tree {
  /* border: 1px solid blue; */
  text-align: left;
}

.active {
  color: red;
}

.top {

/* padding-left: 10px; */
/* padding-left: 10px; */
/* border-bottom: 1px solid black; */
  /* border: 1px solid blue; */
}
.folder {
  /* border: 1px solid green; */
  font-weight: bold;
}
.item {
  cursor: pointer;
}

</style>
