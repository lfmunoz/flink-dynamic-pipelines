<template>
  <nav>
    <ul class="site-nav">
      <li>
        <router-link to="/about" tag="div" class="logo">Flink Pipeline</router-link>
      </li>
      <li>
        <router-link to="/home">HOME</router-link>
      </li>
      <li>
        <router-link to="/test">TEST</router-link>
      </li>
      <li>
        <router-link to="/about">ABOUT</router-link>
      </li>
      <li class="nav-right">
        <z-connection />
      </li>
    </ul>
  </nav>
</template>


<!-- ________________________________________________________________________________ --> 
<!-- SCRIPT -->
<!-- ________________________________________________________________________________ -->
<script>

//--------------------------------------------------------------------------------------
// Default
//--------------------------------------------------------------------------------------
export default {
  name: "appNav",
  components: { },
  //--------------------------------------------------------------------------------------
  // DATA
  //--------------------------------------------------------------------------------------
  data: function() {
    return {
      stdin: "",
    };
  },
  //--------------------------------------------------------------------------------------
  // METHODS
  //--------------------------------------------------------------------------------------
  methods: { },
  //--------------------------------------------------------------------------------------
  // COMPUTED
  //--------------------------------------------------------------------------------------
  computed: {},
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
/** NAVIGATION */

.logo {
  font-size: 2.1rem;
  font-weight: 900;
  color: var(--nav-text-color);
}

.site-nav {
  display: flex;
  padding: 0.5em;
  background-color: var(--nav-bar-bg);
  list-style-type: none;
  border-radius: 0.2em 0.2em 0 0;
  margin-bottom: 0px;
}
.site-nav > li {
  margin-top: 0;
}
.site-nav > li > a {
  display: block;
  padding: 0.5em 1em;
  background-color: var(--nav-bar-box);
  color: white;
  text-decoration: none;
}

.site-nav > li + li {
  margin-left: 1.5em;
}

.site-nav > .nav-right {
  margin-left: auto;
}

li > a.router-link-active {
  font-weight: 900;
  color: var(--nav-text-color);
  font-size: 1.1rem;
}

.site-nav > li > a.authenticated {
  background-color: green;
}
.site-nav > li > a.not-authenticated {
  background-color: red;
}
</style>
