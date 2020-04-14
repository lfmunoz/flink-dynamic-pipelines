<template>
  <div id="app">
    <app-nav/>
    <div id="nav">
      <router-link to="/">Home</router-link>|
      <router-link to="/about">About</router-link>
    </div>
    <router-view />
  </div>
</template>

<style>
/* ________________________________________________________________________________ */
/* GLOBAL CSS */
/* ________________________________________________________________________________ */
:root {
  /** :root  or html **/
  font-size: 1em; /*browser’s default of 16 px*/
  box-sizing: border-box; /*width+border+margin */
  --main-bg: #c9d2d5;
  --main-color: #000;

  --nav-bar-bg: #08090e;
  --nav-bar-box: #3a3d4e;
  --nav-text-color: lightblue;

  --net-bg-color: #f8faf2;

  --notify-good: #d2f2c4;
  --notify-bad: #7f0800;
  --notify-neutral: #d2f2c4;

  --color1: #0bff89;
  --color2: #ff8b8b;
  --color3: #0bbcd6;
  --color4: #b18ae0;
  --color5: #eed974;
  --color6: #005397;
  --color7: #e1cac4;
  --color8: #e3efe3;
}

#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
  color: #2c3e50;
}

#nav {
  padding: 30px;
}

#nav a {
  font-weight: bold;
  color: #2c3e50;
}

#nav a.router-link-exact-active {
  color: #42b983;
}
</style>
