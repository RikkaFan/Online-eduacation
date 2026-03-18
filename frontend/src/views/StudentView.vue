<template>
  <el-container class="apple-layout">
    <el-aside width="240px" class="apple-sidebar">
      <div class="logo-area">
        <h2>在线测评系统</h2>
      </div>
      <el-menu :default-active="$route.path" router class="apple-menu">
        <el-menu-item index="/dashboard">
          <el-icon><DataBoard /></el-icon>
          <span>控制台首页</span>
        </el-menu-item>
        <el-menu-item index="/student/exams">
          <el-icon><EditPen /></el-icon>
          <span>考试列表</span>
        </el-menu-item>
        <el-menu-item index="/student/scores">
          <el-icon><DataLine /></el-icon>
          <span>我的成绩</span>
        </el-menu-item>
        <el-menu-item index="/student/practice">
          <el-icon><Notebook /></el-icon>
          <span>错题本</span>
        </el-menu-item>
      </el-menu>
      <!-- nav-bottom removed per request -->
    </el-aside>

    <el-container>
      <el-main class="apple-main">
        <!-- 欢迎 Banner -->
        <el-card class="apple-card banner-card" shadow="never">
          <div class="banner-content">
            <div class="banner-title">你好，学生</div>
            <div class="banner-sub">欢迎来到在线教育测评系统</div>
          </div>
        </el-card>

        <!-- 统计卡片 -->
        <el-row :gutter="24" class="stats-row">
          <el-col :span="6">
            <el-card class="apple-card stat-card" shadow="never">
              <div class="stat-title">课程数</div>
              <div class="stat-number">{{ courseCount }}</div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="apple-card stat-card" shadow="never">
              <div class="stat-title">待办考试</div>
              <div class="stat-number">{{ upcomingExams.length }}</div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="apple-card stat-card" shadow="never">
              <div class="stat-title">全部考试</div>
              <div class="stat-number">{{ examsCount }}</div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="apple-card stat-card" shadow="never">
              <div class="stat-title">近期课程</div>
              <div class="stat-number">{{ recentCourses.length }}</div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 网格卡片：左近期课程 + 右待办考试 -->
        <el-row :gutter="24">
          <el-col :span="16">
            <el-card class="apple-card list-card" shadow="never">
              <div class="card-header">
                <div class="card-title">近期课程</div>
                <el-button link type="primary" @click="goCourses">查看全部</el-button>
              </div>
              <div v-if="recentCourses.length === 0" class="empty">暂无课程</div>
              <div v-else class="horizontal-list">
                <div v-for="c in recentCourses" :key="c.id" class="apple-card item-card">
                  <div class="item-title">{{ c.courseName }}</div>
                  <div class="item-sub">教师ID：{{ c.teacherId || '-' }}</div>
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card class="apple-card list-card" shadow="never">
              <div class="card-header">
                <div class="card-title">近期考试</div>
                <el-button link type="primary" @click="goExams">进入列表</el-button>
              </div>
              <div v-if="upcomingExams.length === 0" class="empty">暂无待办考试</div>
              <ul v-else class="exam-list">
                <li v-for="e in upcomingExams" :key="e.id" class="exam-item">
                  <div class="exam-title">{{ e.title }}</div>
                  <div class="exam-sub">截止：{{ formatDateTime(e.endTime) }}</div>
                  <el-button link type="primary" @click="enterExam(e.id)">进入考试</el-button>
                </li>
              </ul>
            </el-card>
          </el-col>
        </el-row>

        <!-- 下方内容：近期成绩 + 快速入口 -->
        <el-row :gutter="24">
          <el-col :span="16">
            <el-card class="apple-card list-card" shadow="never">
              <div class="card-header">
                <div class="card-title">我的近期成绩</div>
                <el-button link type="primary" @click="goScores">查看全部</el-button>
              </div>
              <div v-if="recentScores.length === 0" class="empty">暂无成绩记录</div>
              <ul v-else class="score-list">
                <li v-for="s in recentScores" :key="s.id" class="score-item">
                  <div class="score-left">
                    <div class="score-title">{{ s.exam?.title || '未命名考试' }}</div>
                    <div class="score-sub">得分：{{ s.score ?? '-' }}</div>
                  </div>
                  <div class="score-right">{{ formatDateTime(s.submittedAt || s.createdAt) }}</div>
                </li>
              </ul>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card class="apple-card list-card" shadow="never">
              <div class="card-title" style="margin-bottom: 12px;">快速入口</div>
              <div class="quick-actions">
                <el-button type="primary" plain @click="goExams">进入考试列表</el-button>
                <el-button type="primary" plain @click="goScores">查看我的成绩</el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-main>
        <!-- 新增：成绩查询 与 通知/日历（比例一致 16:8） -->
        <el-row :gutter="24">
          <el-col :span="16">
            <el-card class="apple-card list-card" shadow="never">
              <div class="card-header">
                <div class="card-title">成绩查询</div>
                <el-button link type="primary" @click="goScores">查看历史成绩</el-button>
              </div>
              <div class="empty">在此查看所有历史考试的分数与记录</div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card class="apple-card list-card" shadow="never">
              <div class="card-header">
                <div class="card-title">
                  通知 / 日历
                  <el-badge :value="notificationCount" class="item" style="margin-left:8px;" />
                </div>
              </div>
              <ul class="exam-list" v-if="notifications.length">
                <li v-for="n in notifications" :key="n.id" class="exam-item">
                  <div class="exam-title">{{ n.title }}</div>
                  <div class="exam-sub">{{ n.timeText }}</div>
                </li>
              </ul>
              <div v-else class="empty">暂无待办或新成绩通知</div>
            </el-card>
          </el-col>
        </el-row>

    </el-container>
  </el-container>
</template>


<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { DataBoard, Reading, EditPen, DataLine, Notebook } from '@element-plus/icons-vue';
import { getCourses } from '@/api/course';
import { getAllExamsByAllCourses } from '@/api/examTaking';
import { useAuthStore } from '@/store/auth';
import { getMyScores } from '@/api/score';

const router = useRouter();
const authStore = useAuthStore();

const recentCourses = ref([]);
const upcomingExams = ref([]);
const examsAll = ref([]);
const recentScores = ref([]);

onMounted(async () => {
  try {
    const courses = await getCourses();
    recentCourses.value = (courses || []).slice(-5).reverse();
    const exams = await getAllExamsByAllCourses();
    examsAll.value = exams || [];
    const now = Date.now();
    upcomingExams.value = (exams || []).filter(e => {
      const start = e.startTime ? new Date(e.startTime).getTime() : 0;
      const end = e.endTime ? new Date(e.endTime).getTime() : 0;
      return (!start || now >= start) && (!end || now < end);
    }).slice(0, 5);
  } catch (e) {
    ElMessage.error(e.message || '加载学生仪表盘数据失败');
  }
  try {
    const scores = await getMyScores();
    recentScores.value = (scores || []).slice(-5).reverse();
  } catch (e) {}
});

const courseCount = computed(() => recentCourses.value.length);
const examsCount = computed(() => examsAll.value.length);
const notifications = computed(() => {
  const items = [];
  const now = Date.now();
  const soon = 48 * 60 * 60 * 1000; // 48小时
  (examsAll.value || []).forEach(e => {
    const end = e.endTime ? new Date(e.endTime).getTime() : 0;
    if (end && end - now > 0 && end - now <= soon) {
      items.push({ id: `exam-${e.id}`, title: `考试将截止：${e.title}`, timeText: new Date(e.endTime).toLocaleString() });
    }
  });
  (recentScores.value || []).forEach(s => {
    items.push({ id: `score-${s.id}`, title: `新成绩：${s.exam?.title || '考试'}`, timeText: (s.submittedAt || s.createdAt) ? new Date(s.submittedAt || s.createdAt).toLocaleString() : '' });
  });
  return items.slice(0, 6);
});
const notificationCount = computed(() => notifications.value.length);

function goCourses() {
  router.push('/student/courses');
}
function goExams() {
  router.push('/student/exams');
}
function goScores() {
  router.push('/student/scores');
}
function enterExam(id) {
  router.push(`/student/exam/${id}`);
}
function formatDateTime(v) {
  if (!v) return '-';
  const d = typeof v === 'string' ? new Date(v) : v;
  if (Number.isNaN(d.getTime())) return v;
  return d.toLocaleString();
}
function goProfile() {}
function logout() {
  authStore.logout();
  router.push('/login');
}
function handleLogout() {
  logout();
}
</script>

<style scoped>
/* Layout 基础 */
.apple-layout {
  height: 100vh;
  background-color: #F5F5F7;
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Text", "Helvetica Neue", Arial, sans-serif;
}
.apple-sidebar {
  background-color: transparent;
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.logo-area {
  padding: 8px 0;
  margin-bottom: 8px;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.logo-area h2 {
  font-size: 18px;
  font-weight: 600;
  color: #1D1D1F;
  margin: 0;
}
.apple-menu {
  border-right: none !important;
  background: transparent;
}
::v-deep(.apple-menu .el-menu-item) {
  border-radius: 12px;
  margin-bottom: 4px;
  color: #515154;
  height: 48px;
  line-height: 48px;
}
::v-deep(.apple-menu .el-menu-item.is-active) {
  background-color: #E8F0FE;
  color: #1967D2;
  font-weight: 600;
}
::v-deep(.apple-menu .el-menu-item:hover) {
  background-color: rgba(0,0,0,0.04);
}
.apple-main {
  padding: 24px;
}
.user-profile {
  display: flex;
  align-items: center;
  cursor: pointer;
  color: #1D1D1F;
  font-weight: 500;
}
.nav-bottom { width: 100%; margin-bottom: 16px; }
:deep(.el-tooltip__trigger:focus-visible) { outline: none; }

/* Apple Card 通用类（也在全局定义，局部保留以确保生效） */
.apple-card {
  background: #FFFFFF;
  border-radius: 20px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.04);
  padding: 24px;
  margin-bottom: 24px;
  border: none;
}

/* Banner 与内容 */
.banner-card {
  margin-bottom: 12px;
  background: #fff;
}
.banner-content {
  padding: 40px 30px;
  background: linear-gradient(135deg, #fdfbfb 0%, #ebedee 100%);
}
.banner-title {
  font-size: 28px;
  font-weight: 700;
  color: #1D1D1F;
}
.banner-sub {
  margin-top: 4px;
  color: #86868B;
}
.stats-row { margin-bottom: 16px; }
.stat-card { padding: 20px; }
.stat-title { color: #86868B; font-size: 14px; }
.stat-number { color: #1D1D1F; font-size: 32px; font-weight: 700; }
.list-card {
  border: none;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.card-title {
  font-weight: 600;
  color: #1D1D1F;
}
.empty {
  color: #86868B;
  padding: 12px 0;
  text-align: center;
}
.horizontal-list {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  padding-bottom: 8px;
}
.item-card {
  min-width: 220px;
  padding: 16px;
}
.item-title { font-weight: 600; color: #1D1D1F; margin-bottom: 6px; }
.item-sub { color: #86868B; }
.exam-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.exam-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.exam-title { font-weight: 600; color: #1D1D1F; }
.exam-sub { color: #86868B; margin-right: 8px; }
.score-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.score-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.score-title { font-weight: 600; color: #1D1D1F; }
.score-sub { color: #86868B; }
.quick-actions { display: flex; gap: 10px; }
</style>
