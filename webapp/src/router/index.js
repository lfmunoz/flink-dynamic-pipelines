import Vue from 'vue'
import VueRouter from 'vue-router'
import Home from '../views/Home.vue'

import Test from '@/test/Test.vue'
import Connection from '@/websocket/Connection.vue'

// ________________________________________________________________________________
// ROUTES
// ________________________________________________________________________________
const routes = [
  { path: '/', name: 'connection', component: Connection },
  { path: '/home', name: 'Home', component: Home },
  { path: '/test', name: 'Test', component: Test},

  {
    path: '/about',
    name: 'About',
    // route level code-splitting
    // this generates a separate chunk (about.[hash].js) for this route
    // which is lazy-loaded when the route is visited.
    component: () => import(/* webpackChunkName: "about" */ '../views/About.vue')
  }
]

// ________________________________________________________________________________
// ROUTER OBJECT
// ________________________________________________________________________________
Vue.use(VueRouter)
const router = new VueRouter({
  mode: 'history',
  base: process.env.BASE_URL,
  routes
})

// ________________________________________________________________________________
// EXPORT
// ________________________________________________________________________________
export default router
