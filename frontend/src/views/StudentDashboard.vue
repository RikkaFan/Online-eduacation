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

    <div class="glass-card" style="margin-bottom: 24px;">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
        <h3 style="margin: 0; color: #1c1c1e; font-weight: 600;">📈 课程学情追踪</h3>
        <el-select v-model="selectedCourse" placeholder="请选择课程" @change="renderChart" style="width: 200px;">
          <el-option v-for="c in availableCourses" :key="c" :label="c" :value="c" />
        </el-select>
      </div>
      <div ref="chartRef" style="height: 350px; width: 100%;"></div>
    </div>

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
