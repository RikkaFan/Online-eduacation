<template>
  <div class="teacher-dashboard">
    <el-card class="glass-card board-card" shadow="never">
      <div class="board-left">
        <div class="board-title">教学总览</div>
        <div class="board-sub">从这里快速掌握课程、考试与题库运行状态。</div>
      </div>
      <div class="board-metrics">
        <div class="metric-item">
          <div class="metric-label">课程总数</div>
          <div class="metric-value">{{ adminStats.totalCourses }}</div>
        </div>
        <div class="metric-item">
          <div class="metric-label">考试场次</div>
          <div class="metric-value">{{ adminStats.totalExams }}</div>
        </div>
        <div class="metric-item">
          <div class="metric-label">题库总数</div>
          <div class="metric-value">{{ adminStats.totalQuestions }}</div>
        </div>
        <div class="metric-item">
          <div class="metric-label">系统用户</div>
          <div class="metric-value">{{ adminStats.totalUsers }}</div>
        </div>
      </div>
    </el-card>

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
      <div v-if="loadingActivity" class="placeholder">正在加载动态...</div>
      <ul v-else class="activity-list">
        <li v-for="item in latestActivities" :key="item.key" class="activity-item">
          <div class="activity-main">{{ item.title }}</div>
          <div class="activity-sub">{{ item.sub }}</div>
        </li>
      </ul>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { DataLine, EditPen, FolderOpened } from '@element-plus/icons-vue';
import { getAdminStats } from '@/api/stats';
import { getCourses } from '@/api/course';
import { getAllExamsByAllCourses } from '@/api/examTaking';

const router = useRouter();
const adminStats = ref({
  totalCourses: 0,
  totalExams: 0,
  totalQuestions: 0,
  totalUsers: 0,
});

const loadingActivity = ref(false);
const latestActivities = ref([]);

onMounted(async () => {
  try {
    const data = await getAdminStats();
    adminStats.value = { ...adminStats.value, ...(data || {}) };
  } catch (e) {
    ElMessage.error(e.message || '加载统计数据失败');
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
  display: grid;
  gap: 20px;
  padding: 8px 4px 0;
}
.glass-card {
  border-radius: 20px !important;
  border: 1px solid rgba(212, 224, 244, 0.95) !important;
  background: rgba(255, 255, 255, 0.84) !important;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06), 0 2px 6px rgba(15, 23, 42, 0.03) !important;
}

.board-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
  padding: 24px;
}

.board-left {
  flex: 1;
}

.board-title {
  font-size: 28px;
  font-weight: 700;
  color: #0F172A;
}

.board-sub {
  margin-top: 8px;
  color: #64748B;
}

.board-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(120px, 1fr));
  gap: 10px;
  min-width: 360px;
}

.metric-item {
  padding: 12px;
  text-align: center;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(226, 232, 240, 0.8);
}

.metric-label {
  color: #64748B;
  font-size: 13px;
}

.metric-value {
  color: #0F172A;
  font-size: 24px;
  font-weight: 700;
  margin-top: 4px;
}

.section-card {
  padding: 20px;
}

.section-header {
  margin-bottom: 12px;
}

.section-header h3 {
  margin: 0;
  color: #0F172A;
  font-size: 18px;
  font-weight: 600;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.action-card {
  padding: 18px;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.action-card:hover {
  transform: translateY(-2px);
}

.action-icon {
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
  gap: 10px;
}

.activity-item {
  padding: 12px;
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
</style>
