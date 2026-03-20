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

    <el-card class="glass-card section-card trend-card" shadow="never">
      <div class="section-header">
        <h3>我的学情追踪大盘</h3>
      </div>
      <div ref="trendChartRef" class="trend-chart"></div>
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
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue';
import * as echarts from 'echarts';
import { storeToRefs } from 'pinia';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useAuthStore } from '@/store/auth';
import { getStudentStats } from '@/api/stats';
import { getAllExamsByAllCourses } from '@/api/examTaking';
import { getMyScores } from '@/api/score';

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
const myHistoryScores = ref([]);
const trendChartRef = ref(null);
let trendChartInstance = null;

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
  await loadMyHistoryScores();
  await nextTick();
  initTrendChart();
  renderTrendChart();
  window.addEventListener('resize', handleChartResize);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleChartResize);
  if (trendChartInstance) {
    trendChartInstance.dispose();
    trendChartInstance = null;
  }
});

async function loadMyHistoryScores() {
  try {
    const list = await getMyScores();
    const sorted = (Array.isArray(list) ? list : []).slice().sort((a, b) => {
      const ta = resolveSubmitTime(a);
      const tb = resolveSubmitTime(b);
      return ta - tb;
    });
    myHistoryScores.value = sorted;
  } catch (e) {
    ElMessage.error(e.message || '加载历史成绩失败');
    myHistoryScores.value = [];
  }
}

function resolveSubmitTime(item) {
  const raw = item?.submitTime
    || item?.submitAt
    || item?.createTime
    || item?.createAt
    || item?.exam?.endTime
    || item?.exam?.startTime;
  if (!raw) return 0;
  const time = new Date(raw).getTime();
  return Number.isNaN(time) ? 0 : time;
}

function initTrendChart() {
  if (!trendChartRef.value) return;
  if (trendChartInstance) {
    trendChartInstance.dispose();
  }
  trendChartInstance = echarts.init(trendChartRef.value);
}

function renderTrendChart() {
  if (!trendChartInstance) return;
  const xData = myHistoryScores.value.map((item, index) => item?.exam?.title || `考试${index + 1}`);
  const yData = myHistoryScores.value.map(item => Number(item?.score ?? 0));
  trendChartInstance.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        const point = Array.isArray(params) ? params[0] : params;
        return `${point?.axisValue || '-'}<br/>成绩：${point?.data ?? 0} 分`;
      },
    },
    grid: {
      left: 36,
      right: 20,
      top: 28,
      bottom: 28,
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: xData,
      axisLabel: { color: '#64748B' },
      axisLine: { lineStyle: { color: '#CBD5E1' } },
    },
    yAxis: {
      type: 'value',
      name: '分数',
      min: 0,
      axisLabel: { color: '#64748B' },
      splitLine: { lineStyle: { color: 'rgba(203, 213, 225, 0.55)' } },
    },
    series: [
      {
        type: 'line',
        smooth: true,
        data: yData,
        symbol: 'circle',
        symbolSize: 7,
        lineStyle: {
          width: 3,
          color: '#3B82F6',
        },
        itemStyle: {
          color: '#3B82F6',
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(59, 130, 246, 0.36)' },
            { offset: 1, color: 'rgba(59, 130, 246, 0)' },
          ]),
        },
      },
    ],
  });
}

function handleChartResize() {
  trendChartInstance?.resize();
}

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

.trend-card {
  height: auto;
}

.trend-chart {
  height: 350px;
  width: 100%;
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
