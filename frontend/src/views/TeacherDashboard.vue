<template>
  <div class="teacher-dashboard">
    <div class="glass-card welcome-banner">
      <div class="hero-left">
        <h2 class="hero-title">欢迎回来，{{ userName }}！</h2>
        <p class="hero-subtitle">从这里快速掌握课程、考试与题库运行状态。</p>
      </div>
    </div>

    <div class="metrics-grid">
      <div class="glass-card metric-card">
        <el-icon class="metric-icon"><Reading /></el-icon>
        <div class="metric-label">课程总数</div>
        <div class="metric-value">{{ adminStats.totalCourses }}</div>
      </div>
      <div class="glass-card metric-card">
        <el-icon class="metric-icon"><EditPen /></el-icon>
        <div class="metric-label">考试场次</div>
        <div class="metric-value">{{ adminStats.totalExams }}</div>
      </div>
      <div class="glass-card metric-card">
        <el-icon class="metric-icon"><FolderOpened /></el-icon>
        <div class="metric-label">题库总数</div>
        <div class="metric-value">{{ adminStats.totalQuestions }}</div>
      </div>
      <div class="glass-card metric-card">
        <el-icon class="metric-icon"><UserFilled /></el-icon>
        <div class="metric-label">待阅卷数</div>
        <div class="metric-value">{{ pendingGradingCount }}</div>
      </div>
    </div>

    <div class="bottom-grid">
      <el-card class="glass-card section-card" shadow="never">
        <div class="section-header">
          <h3>快捷操作台</h3>
        </div>
        <div class="action-grid">
          <div class="glass-card action-card" @click="goCreateExam">
            <el-icon class="action-icon"><EditPen /></el-icon>
            <div class="action-title">发布新考试</div>
            <div class="action-sub">进入考试管理并唤起新增弹窗</div>
          </div>
          <div class="glass-card action-card" @click="goQuestion">
            <el-icon class="action-icon"><FolderOpened /></el-icon>
            <div class="action-title">录入新题目</div>
            <div class="action-sub">跳转题库管理快速录题</div>
          </div>
          <div class="glass-card action-card" @click="goScore">
            <el-icon class="action-icon"><DataLine /></el-icon>
            <div class="action-title">查看最新成绩</div>
            <div class="action-sub">查看考试结果与分数分布</div>
          </div>
        </div>
      </el-card>

      <el-card class="glass-card section-card" shadow="never">
        <div class="section-header">
          <h3>最新动态</h3>
        </div>
        <div class="activity-scroll">
          <div v-if="loadingActivity" class="placeholder">正在加载动态...</div>
          <ul v-else class="activity-list">
            <li v-for="item in latestActivities" :key="item.key" class="activity-item">
              <div class="activity-main">{{ item.title }}</div>
              <div class="activity-sub">{{ item.sub }}</div>
            </li>
          </ul>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { storeToRefs } from 'pinia';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { DataLine, EditPen, FolderOpened, Reading, UserFilled } from '@element-plus/icons-vue';
import { getAdminStats } from '@/api/stats';
import { getCourses } from '@/api/course';
import { getAllExamsByAllCourses } from '@/api/examTaking';
import { getPendingGrading } from '@/api/score';
import { useAuthStore } from '@/store/auth';

const router = useRouter();
const authStore = useAuthStore();
const { user } = storeToRefs(authStore);
const adminStats = ref({
  totalCourses: 0,
  totalExams: 0,
  totalQuestions: 0,
  totalUsers: 0,
});

const loadingActivity = ref(false);
const latestActivities = ref([]);
const pendingGradingCount = ref(0);
const userName = computed(() => user.value?.username || '教师');

onMounted(async () => {
  try {
    const data = await getAdminStats();
    adminStats.value = { ...adminStats.value, ...(data || {}) };
  } catch (e) {
    ElMessage.error(e.message || '加载统计数据失败');
  }
  try {
    const pendingList = await getPendingGrading();
    pendingGradingCount.value = Array.isArray(pendingList) ? pendingList.length : 0;
  } catch {
    pendingGradingCount.value = 0;
  }

  loadingActivity.value = true;
  try {
    const [courses, exams] = await Promise.all([
      getCourses().catch(() => []),
      getAllExamsByAllCourses().catch(() => []),
    ]);
    const latestCourseList = (courses || []).slice(-3).reverse().map(c => ({
      key: `c-${c.id}`,
      title: `课程更新：${c.courseName || c.name || `课程#${c.id}`}`,
      sub: `课程ID ${c.id} · 教师 ${c.teacherId || '-'}`,
    }));
    const latestExamList = (exams || []).slice(-3).reverse().map(e => ({
      key: `e-${e.id}`,
      title: `考试动态：${e.title || `考试#${e.id}`}`,
      sub: `课程 ${e.course?.courseName || e.course?.name || '-'} · 开始于 ${formatDateTime(e.startTime)}`,
    }));
    const merged = [...latestExamList, ...latestCourseList];
    latestActivities.value = merged.length ? merged.slice(0, 6) : [{
      key: 'empty',
      title: '暂无最新动态',
      sub: '发布课程和考试后，这里会展示最近更新。',
    }];
  } catch (e) {
    latestActivities.value = [{
      key: 'fallback',
      title: '动态加载失败',
      sub: '请稍后刷新页面重试。',
    }];
  } finally {
    loadingActivity.value = false;
  }
});

function formatDateTime(value) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleString();
}

function goCreateExam() {
  router.push({ path: '/teacher/exams', query: { create: '1' } });
}

function goQuestion() {
  router.push('/teacher/questions');
}

function goScore() {
  router.push('/teacher/scores');
}
</script>

<style scoped>
.teacher-dashboard {
  --dashboard-scale: clamp(0.9, calc((100vw - 260px) / 1420), 1);
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 52px 24px 12px;
  box-sizing: border-box;
  width: calc(100% / var(--dashboard-scale));
  transform: scale(var(--dashboard-scale));
  transform-origin: top left;
  background: transparent;
}
@supports (zoom: 1) {
  .teacher-dashboard {
    width: 100%;
    transform: none;
    zoom: var(--dashboard-scale);
  }
}
.glass-card {
  border-radius: 20px !important;
  border: 1px solid rgba(212, 224, 244, 0.95) !important;
  background: rgba(255, 255, 255, 0.84) !important;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06), 0 2px 6px rgba(15, 23, 42, 0.03) !important;
}

.welcome-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
  position: relative;
  overflow: hidden;
  padding: 0 36px 0 108px;
  height: 164px;
  min-height: 164px;
  border-radius: 20px !important;
  isolation: isolate;
}
.welcome-banner::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, rgba(248, 252, 255, 0.98) 0%, rgba(243, 249, 255, 0.94) 50%, rgba(243, 249, 255, 0.2) 72%),
    repeating-linear-gradient(90deg, rgba(148, 163, 184, 0.12) 0 1px, transparent 1px 52px);
  z-index: 0;
  pointer-events: none;
}
.welcome-banner::after {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  width: 50%;
  background-image: url('../assets/library-banner.svg');
  background-size: cover;
  background-position: center center;
  mask-image: linear-gradient(to right, transparent, black 40%);
  -webkit-mask-image: linear-gradient(to right, transparent, black 40%);
  z-index: 0;
  pointer-events: none;
  opacity: 0.9;
}
.hero-left {
  flex: 1;
  max-width: min(56%, 660px);
  min-height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding-top: 12px;
  position: relative;
  z-index: 2;
  min-width: 0;
}
.hero-title {
  font-size: 28px;
  font-weight: 700;
  color: #1c1c1e;
  margin: 0;
}
.hero-subtitle {
  margin-top: 14px;
  font-size: 14px;
  font-weight: 400;
  color: #8E8E93;
}
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}
.metric-card {
  min-height: 108px;
  padding: 18px 14px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 8px;
  text-align: center;
  border-radius: 20px !important;
}
.metric-icon {
  color: #0a84ff;
  font-size: 28px;
}
.metric-label {
  color: #8e8e93;
  font-size: 14px;
  font-weight: 500;
}
.metric-value {
  color: #0f172a;
  font-size: 32px;
  font-weight: 700;
  line-height: 1;
}
.bottom-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 24px;
}

.section-card {
  height: 270px;
  min-height: 0;
}
.section-card :deep(.el-card__body) {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 24px;
  box-sizing: border-box;
}

.section-header {
  margin-bottom: 12px;
}

.section-header h3 {
  margin: 0;
  color: #1c1c1e;
  font-weight: 700;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  align-content: start;
  margin-top: auto;
  margin-bottom: auto;
}

.action-card {
  padding: 18px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.action-card:hover {
  transform: translateY(-2px);
}

.action-icon {
  width: 100%;
  display: flex;
  justify-content: center;
  font-size: 26px;
  color: #4F46E5;
}

.action-title {
  margin-top: 8px;
  color: #0F172A;
  font-weight: 600;
}

.action-sub {
  margin-top: 4px;
  color: #64748B;
  font-size: 13px;
}

.placeholder {
  color: #64748B;
}

.activity-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 12px;
}
.activity-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding-top: 10px;
  padding-right: 4px;
}
.activity-scroll::-webkit-scrollbar {
  width: 6px;
  background: transparent;
}
.activity-scroll::-webkit-scrollbar-track {
  background: transparent;
}
.activity-scroll::-webkit-scrollbar-thumb {
  background-color: transparent;
  border-radius: 6px;
  transition: all 0.3s ease;
}
.section-card:hover .activity-scroll::-webkit-scrollbar-thumb {
  background-color: rgba(200, 200, 200, 0.55);
}

.activity-item {
  padding: 14px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(226, 232, 240, 0.8);
}

.activity-main {
  color: #0F172A;
  font-weight: 600;
}

.activity-sub {
  margin-top: 4px;
  color: #64748B;
  font-size: 13px;
}
@media (max-width: 1200px) {
  .teacher-dashboard {
    --dashboard-scale: 1;
    padding: 36px 20px 12px;
    gap: 20px;
    width: 100%;
    transform: none;
  }
  @supports (zoom: 1) {
    .teacher-dashboard {
      zoom: 1;
    }
  }
  .welcome-banner {
    padding: 0 24px;
    height: 136px;
    min-height: 136px;
    align-items: center;
  }
  .welcome-banner::after {
    width: 100%;
    height: 44%;
    top: auto;
    left: 0;
    bottom: 0;
    background-position: center bottom;
  }
  .hero-left {
    max-width: 100%;
  }
  .metrics-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .bottom-grid {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 760px) {
  .teacher-dashboard {
    padding: 28px 16px 10px;
    gap: 16px;
  }
  .metrics-grid {
    grid-template-columns: 1fr;
  }
  .hero-title {
    font-size: 28px;
  }
}
</style>
