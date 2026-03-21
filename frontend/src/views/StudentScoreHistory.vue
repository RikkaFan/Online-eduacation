<template>
  <div class="student-score-history">
    <div class="page-header"><h2>我的成绩</h2></div>
    <el-card shadow="never">
      <el-table :data="scores" v-loading="loading" empty-text="暂无成绩记录" stripe>
        <el-table-column label="考试名称" min-width="180">
          <template #default="{ row }">{{ row.exam?.title || '-' }}</template>
        </el-table-column>
        <el-table-column prop="score" label="得分" width="120" align="center" />
        <el-table-column label="交卷时间" min-width="180">
          <template #default> - </template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain @click="goReview(row)">🔍 查看试卷</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getMyScores } from '@/api/score';

const router = useRouter();
const scores = ref([]);
const loading = ref(false);

onMounted(async () => {
  loading.value = true;
  try {
    scores.value = await getMyScores();
  } catch (e) {
    ElMessage.error(e.message || '加载我的成绩失败');
  } finally {
    loading.value = false;
  }
});

function goReview(row) {
  const examId = row?.examId || row?.exam?.id;
  if (!examId) return;
  router.push('/student/exam-review/' + examId);
}
</script>

<style scoped>
.student-score-history { padding: 20px; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
</style>
