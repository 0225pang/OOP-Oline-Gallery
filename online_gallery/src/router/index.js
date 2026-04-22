import {createRouter, createWebHistory} from 'vue-router'
import {routes} from "vue-router/auto-routes";
import HomeView from '../views/HomeView.vue'

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        ...routes,
        {
            path: '/',
            name: 'home',
            component: HomeView,
        },
    ],
})

export default router
