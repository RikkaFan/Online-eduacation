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
import StudentExamReady from '@/views/StudentExamReady.vue';
import ScoreAnalysis from '@/views/ScoreAnalysis.vue';
import StudentScoreHistory from '@/views/StudentScoreHistory.vue';
import StudentView from '@/views/StudentView.vue';
import StudentDashboard from '@/views/StudentDashboard.vue';
import StudentCourseList from '@/views/StudentCourseList.vue';
import StudentPractice from '@/views/StudentPractice.vue';
import TeacherView from '@/views/TeacherView.vue';
import TeacherDashboard from '@/views/TeacherDashboard.vue';
import AdminUserManagement from '@/views/AdminUserManagement.vue';
import AdminView from '@/views/AdminView.vue';

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
    redirect: '/teacher/courses',
  },
  {
    path: '/questions',
    redirect: '/teacher/questions',
  },
  {
    path: '/exams',
    redirect: '/teacher/exams',
  },
  {
    path: '/student/exams',
    name: 'StudentExamList',
    component: StudentExamList,
    meta: { requiresAuth: true, roles: ['ROLE_STUDENT'] },
  },
  {
    path: '/student/exam-taking/:id',
    alias: '/student/exam/:id',
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
      { path: 'exam-ready/:id', name: 'StudentExamReady', component: StudentExamReady, meta: { requiresAuth: true, roles: ['ROLE_STUDENT'] } },
      { path: 'exam-review/:id', name: 'StudentExamReview', component: () => import('../views/StudentExamReview.vue'), meta: { requiresAuth: true, roles: ['ROLE_STUDENT'] } },
      { path: 'scores', name: 'StudentScoresAlias', component: StudentScoreHistory, meta: { requiresAuth: true, roles: ['ROLE_STUDENT'] } },
      { path: 'practice', name: 'StudentPractice', component: StudentPractice, meta: { requiresAuth: true, roles: ['ROLE_STUDENT'] } },
      { path: 'favorites', component: () => import('../views/StudentFavorites.vue'), meta: { requiresAuth: true, roles: ['ROLE_STUDENT'] } },
      { path: 'smart-practice', component: () => import('../views/StudentSmartPractice.vue'), meta: { requiresAuth: true, roles: ['ROLE_STUDENT'] } },
    ],
  },
  {
    path: '/teacher',
    component: TeacherView,
    meta: { requiresAuth: true, roles: ['ROLE_TEACHER'] },
    children: [
      { path: '', redirect: 'dashboard' },
      { path: 'dashboard', name: 'TeacherDashboard', component: TeacherDashboard, meta: { requiresAuth: true, roles: ['ROLE_TEACHER'] } },
      { path: 'courses', name: 'CourseManagement', component: CourseManagement, meta: { requiresAuth: true, roles: ['ROLE_TEACHER'] } },
      { path: 'questions', name: 'QuestionManagement', component: QuestionManagement, meta: { requiresAuth: true, roles: ['ROLE_TEACHER'] } },
      { path: 'exams', name: 'ExamManagement', component: ExamManagement, meta: { requiresAuth: true, roles: ['ROLE_TEACHER'] } },
      { path: 'scores', name: 'ScoreAnalysis', component: ScoreAnalysis, meta: { requiresAuth: true, roles: ['ROLE_TEACHER'] } },
      { path: 'grading', component: () => import('../views/TeacherGrading.vue'), meta: { requiresAuth: true, roles: ['ROLE_TEACHER'] } },
    ],
  },
  {
    path: '/admin',
    component: AdminView,
    meta: { requiresAuth: true, roles: ['ROLE_ADMIN'] },
    children: [
      { path: '', redirect: 'dashboard' },
      { path: 'dashboard', name: 'AdminDashboard', component: Dashboard, meta: { requiresAuth: true, roles: ['ROLE_ADMIN'] } },
      { path: 'users', name: 'AdminUsers', component: AdminUserManagement, meta: { requiresAuth: true, roles: ['ROLE_ADMIN'] } },
      { path: 'logs', component: () => import('../views/AdminLogManagement.vue'), meta: { requiresAuth: true, roles: ['ROLE_ADMIN'] } },
    ],
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
  const allowedRoles = Array.isArray(to.meta?.roles) ? to.meta.roles : [];
  const rawRoles = roles.value || [];
  const normalized = rawRoles.map(r => (r && r.startsWith('ROLE_')) ? r : `ROLE_${r}`);

  if (requiresAuth && !isAuthenticated.value) {
    return { path: '/login', query: { redirect: to.fullPath } };
  }

  if (to.path === '/dashboard' && normalized.length > 0) {
    if (normalized.includes('ROLE_STUDENT')) {
      return { path: '/student/dashboard' };
    }
    if (normalized.includes('ROLE_TEACHER')) {
      return { path: '/teacher/dashboard' };
    }
    if (normalized.includes('ROLE_ADMIN')) {
      return { path: '/admin/dashboard' };
    }
  }

  if (allowedRoles.length > 0) {
    const hasAccess = normalized.some(r => allowedRoles.includes(r));
    if (!hasAccess) {
      // 学生端受限时优先送到学生仪表盘，其它角色回到主仪表盘
      if (normalized.includes('ROLE_STUDENT')) {
        return { path: '/student/dashboard' };
      }
      if (normalized.includes('ROLE_TEACHER')) {
        return { path: '/teacher/dashboard' };
      }
      if (normalized.includes('ROLE_ADMIN')) {
        return { path: '/admin/dashboard' };
      }
      return { path: '/dashboard' };
    }
  }

  // Otherwise, allow navigation
  return true;
});

export default router;
