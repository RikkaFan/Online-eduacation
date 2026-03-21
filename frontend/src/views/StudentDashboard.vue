<template>
  <div class="dashboard-wrapper">
    <div class="glass-card welcome-banner">
      <div class="hero-left">
        <h2 class="hero-title">欢迎回来，{{ userName }}！</h2>
        <p class="hero-subtitle">今天想复习点什么？保持学习的节奏。</p>
      </div>
    </div>

    <div class="metrics-grid">
      <div class="glass-card metric-card">
        <el-icon class="metric-icon"><EditPen /></el-icon>
        <div class="metric-label">已考场次</div>
        <div class="metric-number">{{ stats.attendedExams }}</div>
      </div>
      <div class="glass-card metric-card">
        <el-icon class="metric-icon"><Warning /></el-icon>
        <div class="metric-label">错题总数</div>
        <div class="metric-number">{{ stats.totalMistakes }}</div>
      </div>
      <div class="glass-card metric-card">
        <el-icon class="metric-icon"><DataBoard /></el-icon>
        <div class="metric-label">平均得分</div>
        <div class="metric-number">{{ averageScoreText }}<small>分</small></div>
      </div>
      <div class="glass-card metric-card">
        <el-icon class="metric-icon"><Reading /></el-icon>
        <div class="metric-label">累计作答</div>
        <div class="metric-number">{{ totalAnswered }}</div>
      </div>
    </div>

    <div class="bottom-grid">
      <div class="glass-card chart-section">
        <div class="section-head">
          <h3>课程学情追踪</h3>
          <el-select v-model="selectedCourse" placeholder="请选择课程" @change="renderChart" style="width: 200px;">
            <el-option v-for="c in availableCourses" :key="c" :label="c" :value="c" />
          </el-select>
        </div>
        <div ref="chartRef" class="chart-panel"></div>
      </div>

      <div class="glass-card list-section">
        <h3 class="list-title">近期考试</h3>
        <div v-if="loadingExams" class="placeholder">正在加载考试安排...</div>
        <div v-else-if="upcomingExamsView.length > 0" class="exam-list-container custom-scrollbar">
          <div class="exam-head-row">
            <span class="exam-head-course">课程名称</span>
            <span class="exam-head-date">日期</span>
            <span class="exam-head-entry">入口</span>
          </div>
          <div class="exam-body-grid">
            <div
              v-for="exam in upcomingExamsView"
              :key="exam.id"
              class="exam-card-item"
            >
              <div class="exam-col title-col">
                <el-icon class="exam-cal-icon"><Calendar /></el-icon>
                <span class="exam-name">{{ exam.title || '未命名考试' }}</span>
              </div>
              <div class="exam-col time-col">{{ exam.displayTime }}</div>
              <div class="exam-col action-col">
                <el-button class="entry-btn" size="small" type="primary" plain round @click="enterExam(exam.id)">进入</el-button>
              </div>
            </div>
          </div>
        </div>
        <el-empty v-else description="近期没有待考安排" :image-size="80" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue';
import * as echarts from 'echarts';
import { storeToRefs } from 'pinia';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Calendar, DataBoard, EditPen, Reading, Warning } from '@element-plus/icons-vue';
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
const allScores = ref([]);
const availableCourses = ref([]);
const selectedCourse = ref('');
const chartRef = ref(null);
let myChart = null;

const averageScoreText = computed(() => Number(stats.value.averageScore || 0).toFixed(1));
const userName = computed(() => user.value?.username || '同学');
const totalAnswered = computed(() => allScores.value.length || stats.value.attendedExams || 0);
const upcomingExamsView = computed(() => upcomingExams.value.map(exam => ({
  ...exam,
  displayTime: formatDateTime(exam.startTime)
})));

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
  initChart();
  renderChart();
  window.addEventListener('resize', handleChartResize);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleChartResize);
  if (myChart) {
    myChart.dispose();
    myChart = null;
  }
});

async function loadMyHistoryScores() {
  try {
    const list = await getMyScores();
    allScores.value = (Array.isArray(list) ? list : []).map((item, index) => {
      const submitTime = resolveSubmitTime(item);
      return {
        ...item,
        submitTime,
        examTitle: item?.exam?.title || `考试${index + 1}`,
        courseName: item?.courseName || item?.exam?.course?.courseName || item?.exam?.courseName || '未分类课程',
      };
    });
    availableCourses.value = [...new Set(allScores.value.map(item => item.courseName))];
    const latest = allScores.value.slice().sort((a, b) => b.submitTime - a.submitTime)[0];
    selectedCourse.value = latest?.courseName || availableCourses.value[0] || '';
  } catch (e) {
    ElMessage.error(e.message || '加载历史成绩失败');
    allScores.value = [];
    availableCourses.value = [];
    selectedCourse.value = '';
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

function initChart() {
  if (!chartRef.value) return;
  if (myChart) {
    myChart.dispose();
  }
  myChart = echarts.init(chartRef.value);
}

function renderChart() {
  if (!myChart) return;
  const filtered = allScores.value
    .filter(item => item.courseName === selectedCourse.value)
    .slice()
    .sort((a, b) => a.submitTime - b.submitTime);
  const xData = filtered.map(item => item.examTitle);
  const yData = filtered.map(item => Number(item.score ?? 0));
  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.85)',
      borderColor: '#E5E5EA',
      textStyle: { color: '#1C1C1E', fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI"' },
      extraCssText: 'backdrop-filter: blur(12px); box-shadow: 0 8px 24px rgba(0,0,0,0.08); border-radius: 12px; padding: 12px;'
    },
    grid: { top: 30, right: 20, bottom: 30, left: 40, containLabel: true },
    xAxis: {
      type: 'category',
      data: xData, // 过滤后的考试名称
      axisLine: { lineStyle: { color: '#D1D1D6', width: 1 } },
      axisTick: { show: false },
      axisLabel: { color: '#8E8E93', fontFamily: '-apple-system', margin: 12 }
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 100,
      splitLine: { lineStyle: { color: '#F2F2F7', type: 'dashed' } },
      axisLabel: { color: '#8E8E93', fontFamily: '-apple-system' }
    },
    series: [{
      data: yData, // 过滤后的成绩
      type: 'line',
      smooth: 0.4, // 平滑曲线
      showSymbol: false, // 隐藏默认圆点，只有 hover 时显示
      symbol: 'circle',
      symbolSize: 10,
      itemStyle: { color: '#007AFF', borderColor: '#FFF', borderWidth: 2 }, // iOS Blue
      lineStyle: {
        width: 4,
        color: '#007AFF',
        shadowColor: 'rgba(0, 122, 255, 0.3)', // 曲线的立体投影
        shadowBlur: 12,
        shadowOffsetY: 6
      },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(0, 122, 255, 0.25)' }, // 顶部淡蓝色
          { offset: 1, color: 'rgba(0, 122, 255, 0.02)' }  // 底部几乎透明
        ])
      }
    }]
  };
  myChart.setOption(option);
}

function handleChartResize() {
  myChart?.resize();
}

function formatDateTime(value) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleString();
}

function enterExam(id) {
  router.push(`/student/exam-ready/${id}`);
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
.dashboard-wrapper {
  --dashboard-scale: clamp(0.9, calc((100vw - 260px) / 1420), 1);
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 40px 24px 24px;
  box-sizing: border-box;
  width: calc(100% / var(--dashboard-scale));
  transform: scale(var(--dashboard-scale));
  transform-origin: top left;
  background: transparent;
}
@supports (zoom: 1) {
  .dashboard-wrapper {
    width: 100%;
    transform: none;
    zoom: var(--dashboard-scale);
  }
}
:deep(.glass-card) {
  border-radius: 16px !important;
  border: 1px solid rgba(212, 224, 244, 0.96) !important;
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
.metric-icon {
  color: #0a84ff;
  font-size: 28px;
}
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}
.metric-card {
  padding: 18px 14px;
  min-height: 108px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 8px;
  text-align: center;
  border-radius: 20px !important;
}
.metric-label {
  font-size: 14px;
  color: #8E8E93;
  font-weight: 500;
}
.metric-number {
  font-size: 32px;
  font-weight: 700;
  color: #1c1c1e;
  line-height: 1;
}
.metric-number small {
  font-size: 14px;
  font-weight: 600;
  margin-left: 4px;
}
.bottom-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 24px;
}
.chart-section,
.list-section {
  padding: 24px;
  height: 270px;
  display: flex;
  flex-direction: column;
  border-radius: 20px !important;
}
.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.section-head h3 {
  margin: 0;
  color: #1c1c1e;
  font-weight: 700;
}
.list-title {
  margin: 0 0 20px;
  font-size: 18px;
  font-weight: 600;
  color: #1c1c1e;
}
.chart-panel {
  flex: 1;
  min-height: 0;
}
.placeholder {
  color: #64748b;
  padding: 8px 2px;
}
.exam-list-container {
  -webkit-overflow-scrolling: touch;
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 2px;
  transition: all 0.3s ease;
}
.exam-head-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(140px, 140px) minmax(88px, 88px);
  align-items: center;
  color: #8e8e93;
  font-size: 12px;
  font-weight: 600;
  padding: 0 16px 10px;
}
.exam-head-course {
  text-align: left;
}
.exam-head-date,
.exam-head-entry {
  justify-self: start;
  padding-left: 6px;
}
.exam-body-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 14px;
}
.exam-card-item {
  padding: 16px 18px;
  border-radius: 14px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(140px, 140px) minmax(88px, 88px);
  align-items: center;
  border: 1px solid rgba(212, 224, 244, 0.92);
  box-shadow: 0 4px 12px rgba(0,0,0,0.02);
  transition: all 0.3s ease;
  background: rgba(255,255,255,0.86);
}
.exam-col {
  display: flex;
  align-items: center;
}
.exam-cal-icon {
  font-size: 18px;
  color: #007AFF;
  margin-right: 8px;
}
.exam-name {
  font-size: 14px;
  font-weight: 600;
  color: #1c1c1e;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.time-col {
  font-size: 12px;
  color: #8E8E93;
  line-height: 1.45;
  white-space: nowrap;
  justify-content: flex-start;
  text-align: left;
  padding-left: 6px;
}
.action-col {
  justify-content: flex-start;
  padding-left: 6px;
}
.entry-btn {
  min-width: 58px;
}
.exam-list-container::-webkit-scrollbar {
  width: 6px;
  background: transparent;
}
.exam-list-container::-webkit-scrollbar-track {
  background: transparent;
}
.exam-list-container::-webkit-scrollbar-thumb {
  background-color: transparent;
  border-radius: 6px;
  transition: all 0.3s ease;
}
.exam-list-container:hover::-webkit-scrollbar-thumb {
  background-color: rgba(200, 200, 200, 0.5) !important;
}
:deep(.exam-card-item:hover) {
  transform: translateY(-1px);
  background: rgba(255, 255, 255, 0.96) !important;
  border-color: rgba(187, 207, 240, 0.95);
}
:deep(.el-button) {
  border-radius: 12px !important;
}
:deep(.el-tag) {
  border-radius: 12px !important;
}
@media (max-width: 1200px) {
  .dashboard-wrapper {
    --dashboard-scale: 1;
    padding: 28px 20px 20px;
    gap: 20px;
    width: 100%;
    transform: none;
  }
  @supports (zoom: 1) {
    .dashboard-wrapper {
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
  .exam-head-row,
  .exam-card-item {
    grid-template-columns: 1fr;
    gap: 6px;
  }
  .action-col {
    justify-content: flex-start;
  }
}
@media (max-width: 760px) {
  .dashboard-wrapper {
    padding: 20px 16px;
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
