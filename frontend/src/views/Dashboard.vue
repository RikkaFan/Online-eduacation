<template>
  <StudentView v-if="isStudent" />
  <div v-else class="dashboard-container">
    <el-container>
      <el-main>
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
              <el-statistic title="系统用户" :value="stats.users" />
            </el-card>
          </el-col>
        </el-row>
        <div v-if="isAdmin">
          <AdminView />
        </div>
        <div v-else-if="isTeacher">
          <TeacherView />
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
import { useAuthStore } from '@/store/auth';
import AdminView from './AdminView.vue';
import TeacherView from './TeacherView.vue';
import StudentView from './StudentView.vue';
import { getAdminStats } from '@/api/stats';

const authStore = useAuthStore();
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

const stats = ref({ courses: 0, questions: 0, exams: 0, users: 0 });

onMounted(async () => {
  if (isStudent.value) return;
  try {
    const data = await getAdminStats();
    stats.value = {
      courses: data?.totalCourses ?? 0,
      questions: data?.totalQuestions ?? 0,
      exams: data?.totalExams ?? 0,
      users: data?.totalUsers ?? 0
    };
  } catch (e) {
    // 忽略统计失败，不影响主功能
    console.debug('统计数据加载失败', e);
  }
});

// 顶部页头已移除，无需单独登出按钮；登出逻辑仍保留于布局或其他处
</script>

<style scoped>
.welcome-card { margin: 12px 0; }
.welcome-row { display: flex; align-items: center; justify-content: space-between; }
.welcome-text .title { font-size: 18px; font-weight: 600; }
.welcome-text .sub { color: #666; margin-top: 4px; }
.stats-row { margin-bottom: 12px; }
.stat-card { text-align: center; }
</style>
