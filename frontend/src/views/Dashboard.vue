<template>
  <div class="dashboard-container">
    <el-container>
      <el-header class="dashboard-header">
        <span>在线教育测评系统</span>
        <el-button type="danger" @click="handleLogout">登出</el-button>
      </el-header>
      <el-main>
        <div v-if="!isStudent">
          <el-card class="welcome-card" shadow="never">
            <div class="welcome-row">
              <div class="welcome-text">
                <div class="title">欢迎使用在线教育测评系统</div>
                <div class="sub">当前角色：{{ roleLabel }}</div>
              </div>
            </div>
          </el-card>
          <el-row :gutter="12" class="stats-row">
            <el-col :span="6">
              <el-card shadow="never" class="stat-card">
                <el-statistic title="总课程数" :value="stats.courses" />
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card shadow="never" class="stat-card">
                <el-statistic title="题库总量" :value="stats.questions" />
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card shadow="never" class="stat-card">
                <el-statistic title="考试场次" :value="stats.exams" />
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card shadow="never" class="stat-card">
                <el-statistic title="参与学生" :value="stats.students" />
              </el-card>
            </el-col>
          </el-row>
        </div>
        <div v-if="isAdmin">
          <AdminView />
        </div>
        <div v-else-if="isTeacher">
          <TeacherView />
        </div>
        <div v-else-if="isStudent">
          <StudentView />
        </div>
        <div v-else>
          <p>未知的用户角色。</p>
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue';
import { storeToRefs } from 'pinia';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/store/auth';
import AdminView from './AdminView.vue';
import TeacherView from './TeacherView.vue';
import StudentView from './StudentView.vue';
import { getCourses } from '@/api/course';
import { getCategories, getQuestionsByCategory } from '@/api/question';
import { getAllExamsByAllCourses } from '@/api/examTaking';
import { getScoresByExam } from '@/api/score';

const authStore = useAuthStore();
const router = useRouter();
const { roles } = storeToRefs(authStore);

const isAdmin = computed(() => roles.value.includes('ROLE_ADMIN'));
const isTeacher = computed(() => roles.value.includes('ROLE_TEACHER'));
const isStudent = computed(() => roles.value.includes('ROLE_STUDENT'));

const roleLabel = computed(() => {
  if (isAdmin.value) return '管理员';
  if (isTeacher.value) return '教师';
  if (isStudent.value) return '学生';
  return '未知';
});

const stats = ref({ courses: 0, questions: 0, exams: 0, students: 0 });

onMounted(async () => {
  try {
    // 课程数
    const courses = await getCourses();
    stats.value.courses = Array.isArray(courses) ? courses.length : 0;
    // 题目总量：遍历分类并累加各分类题目数
    const categories = await getCategories();
    const qTasks = (categories || []).map(c => getQuestionsByCategory(c.id).then(list => Array.isArray(list) ? list.length : 0));
    const qCounts = await Promise.all(qTasks);
    stats.value.questions = qCounts.reduce((a, b) => a + b, 0);
    // 考试场次
    const exams = await getAllExamsByAllCourses();
    stats.value.exams = Array.isArray(exams) ? exams.length : 0;
    // 参与学生：汇总所有考试成绩中的去重学生数（教师/管理员角色可获取）
    if (isTeacher.value || isAdmin.value) {
      const resultTasks = (exams || []).map(e => getScoresByExam(e.id).catch(() => []));
      const allResultsChunks = await Promise.all(resultTasks);
      const allResults = allResultsChunks.flat();
      const uniq = new Set((allResults || []).map(r => r.student?.id).filter(Boolean));
      stats.value.students = uniq.size;
    } else {
      stats.value.students = 0;
    }
  } catch (e) {
    // 忽略统计失败，不影响主功能
    console.debug('统计数据加载失败', e);
  }
});

const handleLogout = () => {
  authStore.logout();
  router.push('/login');
};
</script>

<style scoped>
.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #409EFF;
  color: white;
  padding: 0 20px;
}
.welcome-card { margin: 12px 0; }
.welcome-row { display: flex; align-items: center; justify-content: space-between; }
.welcome-text .title { font-size: 18px; font-weight: 600; }
.welcome-text .sub { color: #666; margin-top: 4px; }
.stats-row { margin-bottom: 12px; }
.stat-card { text-align: center; }
</style>
