import { createRouter, createWebHistory } from 'vue-router';
import { storeToRefs } from 'pinia';
import { useAuthStore } from '@/store/auth';
import Login from '@/views/Login.vue';
import Dashboard from '@/views/Dashboard.vue';
import CourseManagement from '@/views/CourseManagement.vue';
import QuestionManagement from '@/views/QuestionManagement.vue';
import ExamManagement from '@/views/ExamManagement.vue';
import StudentExamList from '@/views/StudentExamList.vue';
import ExamTaking from '@/views/ExamTaking.vue';
import ScoreAnalysis from '@/views/ScoreAnalysis.vue';
import StudentScoreHistory from '@/views/StudentScoreHistory.vue';
import StudentView from '@/views/StudentView.vue';
import StudentDashboard from '@/views/StudentDashboard.vue';
import StudentCourseList from '@/views/StudentCourseList.vue';

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
    path: '/student/exams',
    name: 'StudentExamList',
    component: StudentExamList,
    meta: { requiresAuth: true, roles: ['ROLE_STUDENT'] },
  },
  {
    path: '/student/exam/:id',
    name: 'ExamTaking',
    component: ExamTaking,
    meta: { requiresAuth: true, roles: ['ROLE_STUDENT'] },
  },
  {
    path: '/student',
    component: StudentView,
    meta: { requiresAuth: true, roles: ['ROLE_STUDENT'] },
    children: [
      { path: '', redirect: 'dashboard' },
      { path: 'dashboard', name: 'StudentDashboard', component: StudentDashboard, meta: { requiresAuth: true, roles: ['ROLE_STUDENT'] } },
      { path: 'courses', name: 'StudentCourseList', component: StudentCourseList, meta: { requiresAuth: true, roles: ['ROLE_STUDENT'] } },
      { path: 'exams', name: 'StudentExamsAlias', component: StudentExamList, meta: { requiresAuth: true, roles: ['ROLE_STUDENT'] } },
      { path: 'scores', name: 'StudentScoresAlias', component: StudentScoreHistory, meta: { requiresAuth: true, roles: ['ROLE_STUDENT'] } },
    ],
  },
  {
    path: '/teacher/scores',
    name: 'ScoreAnalysis',
    component: ScoreAnalysis,
    meta: { requiresAuth: true, roles: ['ROLE_TEACHER', 'ROLE_ADMIN'] },
  },
  {
    path: '/student/scores',
    name: 'StudentScoreHistory',
    component: StudentScoreHistory,
    meta: { requiresAuth: true, roles: ['ROLE_STUDENT'] },
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
    const rawRoles = roles.value || [];
    const normalized = rawRoles.map(r => (r && r.startsWith('ROLE_')) ? r : `ROLE_${r}`);
    const hasAccess = normalized.some(r => allowedRoles.includes(r));
    if (!hasAccess) {
      // 学生端受限时优先送到学生仪表盘，其它角色回到主仪表盘
      if (normalized.includes('ROLE_STUDENT')) {
        return { path: '/student/dashboard' };
      }
      return { path: '/dashboard' };
    }
  }

  // Otherwise, allow navigation
  return true;
});

export default router;
