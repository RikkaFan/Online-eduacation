<template>
  <div class="student-score-history">
    <div class="page-header"><h2>我的成绩</h2></div>
    <el-card shadow="never">
      <el-table :data="scores" v-loading="loading" empty-text="暂无成绩记录">
        <el-table-column label="考试名称" min-width="180">
          <template #default="{ row }">{{ row.exam?.title || '-' }}</template>
        </el-table-column>
        <el-table-column prop="score" label="得分" width="120" align="center" />
        <el-table-column label="交卷时间" min-width="180">
          <template #default> - </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { getMyScores } from '@/api/score';

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
</script>

<style scoped>
.student-score-history { padding: 20px; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
</style>

