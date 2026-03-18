<template>
  <div class="student-dashboard">
    <el-card class="glass-card welcome-board" shadow="never">
      <div class="welcome-left">
        <div class="welcome-title">欢迎回来，准备好今天的学习了吗？</div>
        <div class="welcome-sub">保持节奏，稳步提升，每一次练习都算数。</div>
      </div>
      <div class="welcome-right">
        <div class="metric-item">
          <div class="metric-label">已考场次</div>
          <div class="metric-value">{{ stats.attendedExams }}</div>
        </div>
        <div class="metric-item">
          <div class="metric-label">错题总数</div>
          <div class="metric-value">{{ stats.totalMistakes }}</div>
        </div>
        <div class="metric-item">
          <div class="metric-label">平均分</div>
          <div class="metric-value">{{ averageScoreText }}</div>
        </div>
      </div>
    </el-card>

    <el-row :gutter="20">
      <el-col :span="16">
        <el-card class="glass-card section-card" shadow="never">
          <div class="section-header">
            <h3>近期待考</h3>
            <el-button link type="primary" @click="goExams">查看全部</el-button>
          </div>
          <div v-if="loadingExams" class="placeholder">正在加载考试安排...</div>
          <div v-else-if="upcomingExams.length === 0" class="placeholder">近期暂无考试安排</div>
          <div v-else class="exam-grid">
            <div v-for="exam in upcomingExams" :key="exam.id" class="glass-card exam-card">
              <div class="exam-title">{{ exam.title || '未命名考试' }}</div>
              <div class="exam-meta">课程：{{ exam.course?.courseName || exam.course?.name || '-' }}</div>
              <div class="exam-meta">时间：{{ formatDateTime(exam.startTime) }}</div>
              <el-button type="primary" round @click="enterExam(exam.id)">前往考试</el-button>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="glass-card section-card" shadow="never">
          <div class="section-header">
            <h3>快捷入口</h3>
          </div>
          <div class="quick-links">
            <div class="glass-card quick-card" @click="goPractice">
              <div class="quick-title">错题本</div>
              <div class="quick-sub">查看错题与解析</div>
            </div>
            <div class="glass-card quick-card" @click="goCourses">
              <div class="quick-title">全部课程</div>
              <div class="quick-sub">进入课程学习区</div>
            </div>
            <div class="glass-card quick-card" @click="goScores">
              <div class="quick-title">成绩查询</div>
              <div class="quick-sub">回看历史成绩</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { storeToRefs } from 'pinia';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useAuthStore } from '@/store/auth';
import { getStudentStats } from '@/api/stats';
import { getAllExamsByAllCourses } from '@/api/examTaking';

const router = useRouter();
const authStore = useAuthStore();
const { user } = storeToRefs(authStore);

const stats = ref({
  attendedExams: 0,
  totalMistakes: 0,
  averageScore: 0,
});
const loadingExams = ref(false);
const upcomingExams = ref([]);

const averageScoreText = computed(() => Number(stats.value.averageScore || 0).toFixed(1));

onMounted(async () => {
  const studentId = user.value?.id;
  if (studentId) {
    try {
      const data = await getStudentStats(studentId);
      stats.value = { ...stats.value, ...(data || {}) };
    } catch (e) {
      ElMessage.error(e.message || '加载学生统计失败');
    }
  }

  loadingExams.value = true;
  try {
    const all = await getAllExamsByAllCourses();
    const now = Date.now();
    const list = (all || []).filter(exam => {
      const endAt = exam.endTime ? new Date(exam.endTime).getTime() : Number.MAX_SAFE_INTEGER;
      return now <= endAt;
    }).sort((a, b) => {
      const ta = a.startTime ? new Date(a.startTime).getTime() : Number.MAX_SAFE_INTEGER;
      const tb = b.startTime ? new Date(b.startTime).getTime() : Number.MAX_SAFE_INTEGER;
      return ta - tb;
    });
    upcomingExams.value = list.slice(0, 6);
  } catch (e) {
    ElMessage.error(e.message || '加载待考列表失败');
  } finally {
    loadingExams.value = false;
  }
});

function formatDateTime(value) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleString();
}

function enterExam(id) {
  router.push(`/student/exam/${id}`);
}

function goExams() {
  router.push('/student/exams');
}

function goPractice() {
  router.push('/student/practice');
}

function goCourses() {
  router.push('/student/courses');
}

function goScores() {
  router.push('/student/scores');
}
</script>

<style scoped>
.student-dashboard {
  display: grid;
  gap: 16px;
}

.welcome-board {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  padding: 22px;
}

.welcome-left {
  flex: 1;
}

.welcome-title {
  font-size: 28px;
  font-weight: 700;
  color: #0F172A;
}

.welcome-sub {
  margin-top: 8px;
  color: #64748B;
}

.welcome-right {
  display: grid;
  grid-template-columns: repeat(3, minmax(90px, 1fr));
  gap: 12px;
}

.metric-item {
  text-align: center;
  padding: 12px 10px;
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
  padding: 18px;
  height: 100%;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.section-header h3 {
  margin: 0;
  color: #0F172A;
  font-size: 18px;
  font-weight: 600;
}

.placeholder {
  color: #64748B;
  padding: 8px 2px;
}

.exam-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.exam-card {
  padding: 14px;
  display: grid;
  gap: 8px;
}

.exam-title {
  color: #0F172A;
  font-weight: 600;
  line-height: 1.4;
}

.exam-meta {
  color: #64748B;
  font-size: 13px;
}

.quick-links {
  display: grid;
  gap: 10px;
}

.quick-card {
  padding: 14px;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.quick-card:hover {
  transform: translateY(-2px);
}

.quick-title {
  color: #0F172A;
  font-weight: 600;
}

.quick-sub {
  color: #64748B;
  font-size: 13px;
  margin-top: 4px;
}
</style>
