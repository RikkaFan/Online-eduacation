<template>
  <div class="student-dashboard" style="display: flex; flex-direction: column; gap: 24px; padding: 24px;">
    <div class="page-label">Student Dashboard</div>
    <div class="glass-card welcome-banner" style="padding: 24px; display: flex; justify-content: space-between; align-items: center;">
      <div class="hero-left">
        <h2 class="dash-title">欢迎回来，{{ userName }}</h2>
        <p class="hero-subtitle">持续学习，稳步提升，每一次作答都在靠近更好的自己。</p>
        <div class="hero-meta-list">
          <div class="hero-meta-item"><span>学号</span><strong>{{ userIdText }}</strong></div>
          <div class="hero-meta-item"><span>专业</span><strong>{{ userMajorText }}</strong></div>
          <div v-if="reviewMode" class="hero-meta-item"><span>当前交卷时间</span><strong>{{ nowText }}</strong></div>
        </div>
      </div>
      <div class="hero-art"></div>
    </div>

    <div class="metrics-grid" style="display: grid; grid-template-columns: repeat(4, 1fr); gap: 24px;">
      <div class="glass-card metric-item" style="padding: 24px; text-align: center; display: flex; flex-direction: column; justify-content: center; gap: 8px;">
        <div class="metric-top"><el-icon class="metric-icon"><EditPen /></el-icon><span>已考场次</span></div>
        <div class="metric-value">{{ stats.attendedExams }}</div>
      </div>
      <div class="glass-card metric-item" style="padding: 24px; text-align: center; display: flex; flex-direction: column; justify-content: center; gap: 8px;">
        <div class="metric-top"><el-icon class="metric-icon"><Warning /></el-icon><span>错题总数</span></div>
        <div class="metric-value">{{ stats.totalMistakes }}</div>
      </div>
      <div class="glass-card metric-item" style="padding: 24px; text-align: center; display: flex; flex-direction: column; justify-content: center; gap: 8px;">
        <div class="metric-top"><el-icon class="metric-icon"><DataBoard /></el-icon><span>平均得分</span></div>
        <div class="metric-value">{{ averageScoreText }}<small> 分</small></div>
      </div>
      <div class="glass-card metric-item" style="padding: 24px; text-align: center; display: flex; flex-direction: column; justify-content: center; gap: 8px;">
        <div class="metric-top"><el-icon class="metric-icon"><Reading /></el-icon><span>课程进度</span></div>
        <div class="metric-value">{{ availableCourses.length || 0 }}</div>
      </div>
    </div>

    <div class="bottom-grid" style="display: grid; grid-template-columns: 7fr 3fr; gap: 24px;">
      <div class="glass-card chart-section" style="padding: 24px; height: 400px; display: flex; flex-direction: column;">
        <div class="section-head">
          <h3>课程学情追踪</h3>
          <el-select v-model="selectedCourse" placeholder="请选择课程" @change="renderChart" style="width: 200px;">
            <el-option v-for="c in availableCourses" :key="c" :label="c" :value="c" />
          </el-select>
        </div>
        <div ref="chartRef" class="chart-panel"></div>
      </div>

      <div class="glass-card list-section" style="padding: 24px; height: 400px; display: flex; flex-direction: column;">
        <h3 style="font-size: 18px; font-weight: 600; color: #1c1c1e; margin-bottom: 20px;">近期考试</h3>
        <div v-if="loadingExams" class="placeholder">正在加载考试安排...</div>
        <div v-else-if="upcomingExamsView.length > 0" class="exam-list-container custom-scrollbar" style="flex: 1; overflow-y: auto; overflow-x: hidden;">
          <div class="exam-head-row">
            <span>课程名称</span>
            <span>日期</span>
            <span>操作</span>
          </div>
          <div style="display: grid; grid-template-columns: 1fr; gap: 12px;">
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
                <el-button size="small" type="primary" plain round @click="enterExam(exam.id)">进入</el-button>
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
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Calendar, DataBoard, EditPen, Reading, Warning } from '@element-plus/icons-vue';
import { useAuthStore } from '@/store/auth';
import { getStudentStats } from '@/api/stats';
import { getAllExamsByAllCourses } from '@/api/examTaking';
import { getMyScores } from '@/api/score';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const { user } = storeToRefs(authStore);
const reviewMode = computed(() => route.query.review === '1');
const nowText = ref('');
const nowTimer = ref(null);

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
const userIdText = computed(() => user.value?.id ? String(user.value.id) : '-');
const userMajorText = computed(() => user.value?.major || user.value?.department || '-');
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
  refreshNowText();
  nowTimer.value = setInterval(refreshNowText, 1000);
  window.addEventListener('resize', handleChartResize);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleChartResize);
  if (nowTimer.value) {
    clearInterval(nowTimer.value);
    nowTimer.value = null;
  }
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

function refreshNowText() {
  nowText.value = new Date().toLocaleString();
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
.page-label {
  font-size: 42px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.6px;
  line-height: 1;
}
.dash-title {
  font-size: 30px;
  font-weight: 700;
  color: #1c1c1e;
  margin: 0;
}
.welcome-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
  position: relative;
  overflow: hidden;
}
.welcome-banner::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(to right, rgba(203, 225, 255, 0.18), rgba(255, 255, 255, 0)),
    repeating-linear-gradient(90deg, rgba(148, 163, 184, 0.1) 0 1px, transparent 1px 48px);
  pointer-events: none;
}
.hero-left {
  flex: 1;
  position: relative;
  z-index: 1;
}
.hero-subtitle {
  margin-top: 8px;
  color: #475569;
}
.hero-meta-list {
  margin-top: 18px;
  display: grid;
  gap: 8px;
}
.hero-meta-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(255, 255, 255, 0.42);
  border-radius: 12px;
  padding: 9px 12px;
  color: #64748b;
  font-size: 13px;
}
.hero-meta-item strong {
  color: #0f172a;
  font-weight: 600;
}
.hero-art {
  width: 220px;
  height: 140px;
  border-radius: 16px;
  background:
    radial-gradient(at 20% 20%, rgba(59, 130, 246, 0.2) 0px, transparent 55%),
    radial-gradient(at 75% 75%, rgba(14, 165, 233, 0.2) 0px, transparent 50%),
    linear-gradient(160deg, rgba(255, 255, 255, 0.45), rgba(255, 255, 255, 0.22));
}
.metric-top {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #64748b;
  font-size: 13px;
}
.metric-value {
  color: #0f172a;
  font-size: 30px;
  font-weight: 700;
  margin-top: 6px;
}
.metric-value small {
  font-size: 15px;
  font-weight: 600;
}
.metric-icon {
  color: #0a84ff;
}
.metric-item {
  box-shadow: 0 6px 14px rgba(15, 23, 42, 0.03);
}
.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.section-head h3 {
  margin: 0;
  color: #1c1c1e;
  font-weight: 700;
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
  overflow-y: auto;
  overflow-x: hidden;
  transition: all 0.3s ease;
}
.exam-head-row {
  display: grid;
  grid-template-columns: 1fr 1fr auto;
  align-items: center;
  color: #8e8e93;
  font-size: 12px;
  font-weight: 600;
  padding: 0 14px 8px;
}
.exam-card-item {
  padding: 14px 16px;
  border-radius: 12px;
  display: grid;
  grid-template-columns: 1fr 1fr auto;
  align-items: center;
  border: none;
  box-shadow: 0 4px 12px rgba(0,0,0,0.02);
  transition: all 0.3s ease;
  background: rgba(255,255,255,0.4);
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
}
.action-col {
  justify-content: flex-end;
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
  background: rgba(255, 255, 255, 0.58) !important;
}
:deep(.el-button) {
  border-radius: 12px !important;
}
:deep(.el-tag) {
  border-radius: 12px !important;
}
@media (max-width: 1200px) {
  .welcome-banner {
    flex-direction: column;
    align-items: flex-start;
  }
  .hero-art {
    width: 100%;
    height: 120px;
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
</style>
