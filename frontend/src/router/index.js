import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '@/store/auth';
import Login from '@/views/Login.vue';
import Dashboard from '@/views/Dashboard.vue';

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login,
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: Dashboard,
    meta: { requiresAuth: true },
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/login',
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to, from) => {
  const authStore = useAuthStore();
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth);

  if (requiresAuth && !authStore.isAuthenticated) {
    // Redirect to the login page if authentication is required and the user is not authenticated
    return '/login';
  }

  // Otherwise, allow navigation
  return true;
});

export default router;
