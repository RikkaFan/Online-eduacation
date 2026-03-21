<template>
  <div class="student-dashboard">
    <h2 class="dash-title">
      <el-icon class="title-icon"><DataBoard /></el-icon>
      欢迎回来，今天想学点什么？
    </h2>

    <div class="metric-grid">
      <el-card class="glass-card metric-card" shadow="never">
        <div class="metric-icon"><el-icon><EditPen /></el-icon></div>
        <div class="metric-label">已考场次</div>
        <div class="metric-value">{{ stats.attendedExams }}</div>
      </el-card>
      <el-card class="glass-card metric-card" shadow="never">
        <div class="metric-icon"><el-icon><Trophy /></el-icon></div>
        <div class="metric-label">平均得分</div>
        <div class="metric-value">{{ averageScoreText }} 分</div>
      </el-card>
      <el-card class="glass-card metric-card" shadow="never">
        <div class="metric-icon"><el-icon><Warning /></el-icon></div>
        <div class="metric-label">错题总数</div>
        <div class="metric-value">{{ stats.totalMistakes }}</div>
      </el-card>
      <el-card class="glass-card metric-card" shadow="never">
        <div class="metric-icon"><el-icon><Reading /></el-icon></div>
        <div class="metric-label">参与课程</div>
        <div class="metric-value">{{ availableCourses.length || 0 }}</div>
      </el-card>
    </div>

    <el-card class="glass-card hero-banner" shadow="never">
      <div class="hero-text">
        <template v-if="upcomingExams.length > 0">
          <el-icon class="hero-icon"><Promotion /></el-icon>
          你的下一场考试《{{ upcomingExams[0]?.title }}》即将开始，请做好准备！
        </template>
        <template v-else>
          <el-icon class="hero-icon"><CircleCheck /></el-icon>
          近期暂无考试，去复习一下错题本吧！
        </template>
      </div>
      <template v-if="upcomingExams.length > 0">
        <el-button type="primary" size="large" @click="enterExam(upcomingExams[0].id)">进入考场</el-button>
      </template>
      <template v-else>
        <el-button type="primary" size="large" plain @click="goPractice">去刷错题</el-button>
      </template>
    </el-card>

    <div class="bottom-layout">
      <div class="glass-card chart-section">
        <div class="chart-head">
          <h3><el-icon class="section-icon"><TrendCharts /></el-icon>课程学情追踪</h3>
          <el-select v-model="selectedCourse" placeholder="请选择课程" @change="renderChart" style="width: 200px;">
            <el-option v-for="c in availableCourses" :key="c" :label="c" :value="c" />
          </el-select>
        </div>
        <div ref="chartRef" class="chart-panel"></div>
      </div>

      <div class="glass-card list-section">
        <div class="section-header">
          <h3>近期考试</h3>
          <el-button link type="primary" @click="goExams">查看全部</el-button>
        </div>
        <div v-if="loadingExams" class="placeholder">正在加载考试安排...</div>
        <el-empty v-else-if="upcomingExams.length === 0" description="近期没有待考安排" :image-size="80" />
        <div v-else class="exam-timeline">
          <div v-for="exam in upcomingExams" :key="exam.id" class="timeline-item">
            <div class="timeline-left">
              <el-icon class="calendar-icon"><Calendar /></el-icon>
              <span class="timeline-time">{{ formatDateTime(exam.startTime) }}</span>
            </div>
            <div class="timeline-title">{{ exam.title || '未命名考试' }}</div>
            <el-button size="small" type="primary" plain @click="enterExam(exam.id)">前往</el-button>
          </div>
        </div>
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
import { Calendar, CircleCheck, DataBoard, EditPen, Promotion, Reading, TrendCharts, Trophy, Warning } from '@element-plus/icons-vue';
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
.student-dashboard {
  display: flex;
  flex-direction: column;
  gap: 24px;
}
.dash-title {
  font-size: 24px;
  font-weight: 600;
  color: #1c1c1e;
  margin: 0 0 20px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.title-icon {
  color: #0a84ff;
}
.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}
.metric-card {
  padding: 24px;
  text-align: center;
}
.metric-icon {
  font-size: 24px;
  margin-bottom: 8px;
  color: #334155;
}
.metric-label {
  color: #64748b;
  font-size: 13px;
}
.metric-value {
  color: #0f172a;
  font-size: 26px;
  font-weight: 700;
  margin-top: 6px;
}
.hero-banner {
  background: linear-gradient(135deg, #f0f7ff 0%, #ffffff 100%);
  border: 1px solid #cce4ff;
  padding: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
}
.hero-text {
  color: #1f2937;
  font-size: 15px;
  line-height: 1.6;
  font-weight: 500;
  display: flex;
  align-items: center;
}
.hero-icon {
  margin-right: 6px;
  color: #0a84ff;
}
.bottom-layout {
  display: grid;
  grid-template-columns: 7fr 3fr;
  gap: 20px;
  margin-top: 24px;
}
.chart-section {
  padding: 24px;
}
.chart-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.chart-head h3 {
  margin: 0;
  color: #1c1c1e;
  font-weight: 600;
  display: flex;
  align-items: center;
}
.section-icon {
  margin-right: 6px;
  color: #0a84ff;
}
.chart-panel {
  height: 350px;
  width: 100%;
  padding: 10px 20px;
  box-sizing: border-box;
}
.list-section {
  padding: 24px;
}
.placeholder {
  color: #64748b;
  padding: 8px 2px;
}
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.section-header h3 {
  margin: 0;
  color: #0f172a;
  font-size: 18px;
  font-weight: 600;
}
.exam-timeline {
  display: grid;
  gap: 10px;
}
.timeline-item {
  display: grid;
  grid-template-columns: 1.2fr 1fr auto;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  margin-bottom: 8px;
  background: rgba(255, 255, 255, 0.4);
  border-radius: 12px;
  transition: all 0.3s ease;
}
.timeline-item:hover {
  background: rgba(255, 255, 255, 0.62);
}
.timeline-left {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #64748b;
  min-width: 0;
}
.calendar-icon {
  font-size: 16px;
  color: #64748b;
}
.timeline-time {
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.timeline-title {
  color: #0f172a;
  font-weight: 600;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
:deep(.el-button) {
  border-radius: 12px !important;
}
:deep(.el-tag) {
  border-radius: 12px !important;
}
@media (max-width: 1200px) {
  .metric-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .bottom-layout {
    grid-template-columns: 1fr;
  }
  .timeline-item {
    grid-template-columns: 1fr;
    gap: 8px;
    align-items: flex-start;
  }
}
</style>
