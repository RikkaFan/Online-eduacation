<template>
  <div class="student-practice">
    <el-card class="apple-card summary-card" shadow="never">
      <div class="summary-title">错题本概览</div>
      <div class="summary-value">累计错题：{{ mistakes.length }} 道</div>
    </el-card>

    <el-empty v-if="!loading && mistakes.length === 0" description="暂无错题记录，继续保持！" />

    <div class="mistake-list" v-loading="loading">
      <el-card v-for="item in mistakes" :key="item.id" class="apple-card mistake-card" shadow="never">
        <div class="card-header">
          <el-tag size="small" effect="plain">{{ typeName(item.question?.type) }}</el-tag>
        </div>
        <div class="question-content">{{ item.question?.content || '-' }}</div>
        <div class="answer-row">
          <span class="label">你的答案：</span>
          <span class="value wrong">{{ item.selectedAnswer || '未作答' }}</span>
        </div>
        <div class="answer-row">
          <span class="label">正确答案：</span>
          <span class="value right">{{ item.question?.answer || '-' }}</span>
        </div>
        <div v-if="item.question?.analysis || item.question?.explanation" class="analysis">
          <span class="label">解析：</span>
          <span class="value">{{ item.question?.analysis || item.question?.explanation }}</span>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { storeToRefs } from 'pinia';
import { ElMessage } from 'element-plus';
import { useAuthStore } from '@/store/auth';
import { getStudentMistakes } from '@/api/examTaking';

const authStore = useAuthStore();
const { user } = storeToRefs(authStore);

const loading = ref(false);
const mistakes = ref([]);

function typeName(type) {
  if (type === 'SINGLE_CHOICE') return '单选题';
  if (type === 'MULTIPLE_CHOICE') return '多选题';
  if (type === 'TRUE_FALSE') return '判断题';
  return type || '未知题型';
}

onMounted(async () => {
  const studentId = user.value?.id;
  if (!studentId) {
    ElMessage.error('未获取到学生身份信息，请重新登录');
    return;
  }
  loading.value = true;
  try {
    const data = await getStudentMistakes(studentId);
    mistakes.value = Array.isArray(data) ? data : [];
  } catch (e) {
    ElMessage.error(e.message || '加载错题本失败');
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.student-practice {
  padding: 0;
}
.apple-card {
  border-radius: 16px;
  border: none;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
}
.summary-card {
  margin-bottom: 16px;
}
.summary-title {
  color: #6E6E73;
  font-size: 14px;
}
.summary-value {
  margin-top: 8px;
  font-size: 24px;
  font-weight: 600;
  color: #1D1D1F;
}
.mistake-list {
  display: grid;
  gap: 12px;
}
.mistake-card {
  padding: 4px 2px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.question-content {
  font-size: 15px;
  line-height: 1.7;
  color: #1D1D1F;
  margin-bottom: 10px;
}
.answer-row,
.analysis {
  margin-top: 8px;
  line-height: 1.6;
}
.label {
  color: #6E6E73;
}
.value {
  color: #1D1D1F;
}
.wrong {
  color: #D93025;
  font-weight: 600;
}
.right {
  color: #188038;
  font-weight: 600;
}
</style>
