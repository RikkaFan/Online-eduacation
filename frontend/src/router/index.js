import { createRouter, createWebHistory } from 'vue-router';
import { storeToRefs } from 'pinia';
import { useAuthStore } from '@/store/auth';
import Login from '@/views/Login.vue';
import Dashboard from '@/views/Dashboard.vue';
import CourseManagement from '@/views/CourseManagement.vue';
import QuestionManagement from '@/views/QuestionManagement.vue';
import ExamManagement from '@/views/ExamManagement.vue';

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
    path: '/courses',
    name: 'CourseManagement',
    component: CourseManagement,
    meta: { requiresAuth: true, roles: ['ROLE_TEACHER', 'ROLE_ADMIN'] },
  },
  {
    path: '/questions',
    name: 'QuestionManagement',
    component: QuestionManagement,
    meta: { requiresAuth: true, roles: ['ROLE_TEACHER', 'ROLE_ADMIN'] },
  },
  {
    path: '/exams',
    name: 'ExamManagement',
    component: ExamManagement,
    meta: { requiresAuth: true, roles: ['ROLE_TEACHER', 'ROLE_ADMIN'] },
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
  const { isAuthenticated, roles } = storeToRefs(authStore);
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth);
  const allowedRoles = to.matched.flatMap(record => (record.meta && record.meta.roles) ? record.meta.roles : []);

  if (requiresAuth && !isAuthenticated.value) {
    return { path: '/login', query: { redirect: to.fullPath } };
  }

  if (allowedRoles.length > 0) {
    const userRoles = roles.value || [];
    const hasAccess = userRoles.some(r => allowedRoles.includes(r));
    if (!hasAccess) {
      return { path: '/dashboard' };
    }
  }

  // Otherwise, allow navigation
  return true;
});

export default router;
