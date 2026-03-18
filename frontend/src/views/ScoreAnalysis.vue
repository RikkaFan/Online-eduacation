<template>
  <div class="score-analysis">
    <div class="action-bar">
      <h2>成绩分析</h2>
      <el-select v-model="selectedExamId" placeholder="选择考试" filterable clearable @change="onExamChange" style="width: 320px">
        <el-option v-for="e in exams" :key="e.id" :label="examLabel(e)" :value="e.id" />
      </el-select>
    </div>

    <div v-if="selectedExamId" class="stat-cards">
      <el-card class="stat-card"><div class="stat-title">最高分</div><div class="stat-value">{{ stat.max }}</div></el-card>
      <el-card class="stat-card"><div class="stat-title">最低分</div><div class="stat-value">{{ stat.min }}</div></el-card>
      <el-card class="stat-card"><div class="stat-title">平均分</div><div class="stat-value">{{ stat.avg }}</div></el-card>
    </div>

    <el-card shadow="never">
      <el-table :data="scores" v-loading="loadingScores" empty-text="请选择考试后查看成绩" stripe>
        <el-table-column label="学生" min-width="160">
          <template #default="{ row }">{{ row.student?.username || '-' }}</template>
        </el-table-column>
        <el-table-column label="考试名称" min-width="180">
          <template #default="{ row }">{{ row.exam?.title || '-' }}</template>
        </el-table-column>
        <el-table-column prop="score" label="得分" width="120" align="center" />
        <el-table-column label="交卷时间" min-width="180">
          <template #default="{ row }">-</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { ElMessage } from 'element-plus';
import { getAllExamsByAllCourses } from '@/api/examTaking';
import { getScoresByExam } from '@/api/score';

const exams = ref([]);
const selectedExamId = ref(null);
const scores = ref([]);
const loadingExams = ref(false);
const loadingScores = ref(false);

onMounted(async () => {
  loadingExams.value = true;
  try {
    exams.value = await getAllExamsByAllCourses();
  } catch (e) {
    ElMessage.error(e.message || '加载考试列表失败');
  } finally {
    loadingExams.value = false;
  }
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

const stat = computed(() => {
  const arr = (scores.value || []).map(s => Number(s.score ?? 0));
  if (arr.length === 0) return { max: 0, min: 0, avg: 0 };
  const max = Math.max(...arr);
  const min = Math.min(...arr);
  const avg = (arr.reduce((a, b) => a + b, 0) / arr.length).toFixed(2);
  return { max, min, avg };
});
</script>

<style scoped>
.score-analysis { padding: 20px; }
.action-bar {
  display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px;
}
.stat-cards { display: flex; gap: 12px; margin-bottom: 12px; }
.stat-card { width: 200px; text-align: center; }
.stat-title { font-size: 14px; color: #666; }
.stat-value { font-size: 24px; font-weight: 600; color: #111; }
</style>
