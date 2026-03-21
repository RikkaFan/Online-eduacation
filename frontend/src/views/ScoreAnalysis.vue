<template>
  <div class="score-analysis">
    <div class="glass-card action-bar">
      <div>
        <h2>成绩分析</h2>
        <div class="bar-sub">查看成绩分布、统计指标并导出分析数据。</div>
      </div>
      <div class="action-tools">
        <el-select v-model="selectedExamId" placeholder="选择考试" filterable clearable @change="onExamChange" style="width: 320px">
          <el-option v-for="e in exams" :key="e.id" :label="examLabel(e)" :value="e.id" />
        </el-select>
        <el-button type="success" :icon="Download" :disabled="!selectedExamId" @click="onExportExcel">
          导出为 Excel
        </el-button>
      </div>
    </div>

    <div v-if="selectedExamId" class="stat-cards">
      <el-card class="glass-card stat-card"><div class="stat-title">最高分</div><div class="stat-value">{{ stat.max }}</div></el-card>
      <el-card class="glass-card stat-card"><div class="stat-title">最低分</div><div class="stat-value">{{ stat.min }}</div></el-card>
      <el-card class="glass-card stat-card"><div class="stat-title">平均分</div><div class="stat-value">{{ stat.avg }}</div></el-card>
    </div>

    <el-card v-if="selectedExamId" class="glass-card distribution-card" shadow="never">
      <div class="distribution-title">本次考试成绩分布</div>
      <div ref="distributionChartRef" class="distribution-chart"></div>
    </el-card>

    <el-card class="glass-card" shadow="never">
      <el-table :data="scores" v-loading="loadingScores" empty-text="请选择考试后查看成绩" stripe>
        <el-table-column label="学生" min-width="160">
          <template #default="{ row }">{{ row.student?.username || '-' }}</template>
        </el-table-column>
        <el-table-column label="考试名称" min-width="180">
          <template #default="{ row }">{{ row.exam?.title || '-' }}</template>
        </el-table-column>
        <el-table-column prop="score" label="得分" width="120" align="center" />
        <el-table-column label="交卷时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.submittedAt) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, nextTick, watch, onBeforeUnmount } from 'vue';
import * as echarts from 'echarts';
import { ElMessage } from 'element-plus';
import { Download } from '@element-plus/icons-vue';
import { getAllExamsByAllCourses } from '@/api/examTaking';
import { exportScoreToExcel, getScoresByExam } from '@/api/score';

const exams = ref([]);
const selectedExamId = ref(null);
const scores = ref([]);
const loadingExams = ref(false);
const loadingScores = ref(false);
const distributionChartRef = ref(null);
let distributionChartInstance = null;

onMounted(async () => {
  loadingExams.value = true;
  try {
    exams.value = await getAllExamsByAllCourses();
  } catch (e) {
    ElMessage.error(e.message || '加载考试列表失败');
  } finally {
    loadingExams.value = false;
  }
  window.addEventListener('resize', handleChartResize);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleChartResize);
  if (distributionChartInstance) {
    distributionChartInstance.dispose();
    distributionChartInstance = null;
  }
});

watch(() => selectedExamId.value, async (val) => {
  if (!val && distributionChartInstance) {
    distributionChartInstance.clear();
  }
});

watch(() => scores.value, async () => {
  await nextTick();
  initDistributionChart();
  renderDistributionChart();
});

function examLabel(e) {
  const name = e.title || e.name || `考试#${e.id}`;
  const course = e.course?.courseName || e.courseName || '';
  return course ? `${name}（${course}）` : name;
}

async function onExamChange() {
  if (!selectedExamId.value) {
    scores.value = [];
    return;
  }
  loadingScores.value = true;
  try {
    scores.value = await getScoresByExam(selectedExamId.value);
  } catch (e) {
    ElMessage.error(e.message || '加载成绩失败');
    scores.value = [];
  } finally {
    loadingScores.value = false;
  }
}

async function onExportExcel() {
  if (!selectedExamId.value) {
    ElMessage.warning('请先选择考试');
    return;
  }
  try {
    const { blob, fileName } = await exportScoreToExcel(selectedExamId.value);
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    link.style.display = 'none';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
    ElMessage.success('导出成功');
  } catch (e) {
    ElMessage.error(e.message || '导出失败');
  }
}

const stat = computed(() => {
  const arr = (scores.value || []).map(s => Number(s.score ?? 0));
  if (arr.length === 0) return { max: 0, min: 0, avg: 0 };
  const max = Math.max(...arr);
  const min = Math.min(...arr);
  const avg = (arr.reduce((a, b) => a + b, 0) / arr.length).toFixed(2);
  return { max, min, avg };
});

const scoreDistribution = computed(() => {
  const buckets = [0, 0, 0, 0, 0];
  (scores.value || []).forEach(item => {
    const score = Number(item?.score ?? 0);
    if (score < 60) buckets[0] += 1;
    else if (score < 70) buckets[1] += 1;
    else if (score < 80) buckets[2] += 1;
    else if (score < 90) buckets[3] += 1;
    else buckets[4] += 1;
  });
  return buckets;
});

function initDistributionChart() {
  if (!distributionChartRef.value) return;
  if (!distributionChartInstance) {
    distributionChartInstance = echarts.init(distributionChartRef.value);
  }
}

function renderDistributionChart() {
  if (!distributionChartInstance) return;
  const labels = ['<60 不及格', '60-69 及格', '70-79 中等', '80-89 良好', '90-100 优秀'];
  const colors = ['#EF4444', '#F59E0B', '#3B82F6', '#8B5CF6', '#10B981'];
  const values = scoreDistribution.value;
  distributionChartInstance.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const point = Array.isArray(params) ? params[0] : params;
        return `${point?.axisValue || '-'}<br/>人数：${point?.data ?? 0}`;
      },
    },
    grid: {
      left: 24,
      right: 20,
      top: 28,
      bottom: 22,
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: labels,
      axisLabel: { color: '#64748B' },
      axisLine: { lineStyle: { color: '#CBD5E1' } },
    },
    yAxis: {
      type: 'value',
      name: '人数',
      minInterval: 1,
      axisLabel: { color: '#64748B' },
      splitLine: { lineStyle: { color: 'rgba(203, 213, 225, 0.55)' } },
    },
    series: [{
      type: 'bar',
      data: values.map((val, idx) => ({ value: val, itemStyle: { color: colors[idx] } })),
      barWidth: '48%',
      borderRadius: [8, 8, 0, 0],
    }],
  });
}

function handleChartResize() {
  distributionChartInstance?.resize();
}

function formatDateTime(v) {
  if (!v) return '-';
  const d = typeof v === 'string' ? new Date(v) : v;
  if (Number.isNaN(d.getTime())) return '-';
  return d.toLocaleString();
}
</script>

<style scoped>
.score-analysis {
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
.action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 22px;
}
.bar-sub {
  margin-top: 8px;
  color: #64748b;
  font-size: 13px;
}
.action-tools {
  display: flex;
  align-items: center;
  gap: 12px;
}
.stat-cards { display: flex; gap: 16px; }
.stat-card { width: 220px; text-align: center; padding: 18px; }
.stat-title { font-size: 14px; color: #666; }
.stat-value { font-size: 24px; font-weight: 600; color: #111; }
.distribution-card { padding: 18px; }
.distribution-title {
  margin-bottom: 10px;
  color: #0F172A;
  font-size: 18px;
  font-weight: 600;
}
.distribution-chart {
  width: 100%;
  height: 300px;
}
</style>
