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
        <div class="action-row">
          <el-button class="ai-tutor-btn" type="primary" plain @click="onSummonAiTutor(item)">✨ 召唤 AI 私教讲题</el-button>
          <el-button type="warning" plain @click="onFavorite(item)">⭐ 收藏本题</el-button>
        </div>
      </el-card>
    </div>

    <el-dialog v-model="aiDialogVisible" title="💡 AI 专属私教辅导中..." width="620px">
      <div v-loading="aiLoading" class="ai-dialog-body">
        <div v-if="!aiLoading" class="ai-chat-bubble">{{ aiReply || 'AI 私教暂时没有给出回复，请稍后重试。' }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { storeToRefs } from 'pinia';
import { ElMessage } from 'element-plus';
import { useAuthStore } from '@/store/auth';
import { getStudentMistakes } from '@/api/examTaking';
import { toggleFavorite } from '@/api/favorite';
import { summonAiTutor } from '@/api/question';

const authStore = useAuthStore();
const { user } = storeToRefs(authStore);

const loading = ref(false);
const mistakes = ref([]);
const aiDialogVisible = ref(false);
const aiLoading = ref(false);
const aiReply = ref('');

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

async function onFavorite(item) {
  const questionId = item?.question?.id;
  if (!questionId) {
    ElMessage.warning('题目信息缺失，无法收藏');
    return;
  }
  try {
    const data = await toggleFavorite(questionId);
    if (data?.favorited === false) {
      ElMessage.success('已从收藏本移除');
      return;
    }
    ElMessage.success('已收藏到题目本');
  } catch (e) {
    ElMessage.error(e.message || '收藏失败');
  }
}

async function onSummonAiTutor(item) {
  const question = item?.question?.content || '';
  const standardAnswer = item?.question?.answer || '';
  const studentAnswer = item?.selectedAnswer || '未作答';
  if (!question || !standardAnswer) {
    ElMessage.warning('题目信息不完整，暂时无法召唤 AI 私教');
    return;
  }
  aiDialogVisible.value = true;
  aiLoading.value = true;
  aiReply.value = '';
  try {
    const result = await summonAiTutor({
      question,
      standardAnswer,
      studentAnswer,
    });
    aiReply.value = result?.explanation || result?.message || (typeof result === 'string' ? result : '');
  } catch (e) {
    ElMessage.error(e.message || 'AI 私教讲题失败');
    aiReply.value = '';
  } finally {
    aiLoading.value = false;
  }
}
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
.action-row {
  margin-top: 12px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.ai-tutor-btn {
  box-shadow: 0 8px 20px rgba(59, 130, 246, 0.22);
}
.ai-dialog-body {
  min-height: 180px;
  padding: 8px 2px;
}
.ai-chat-bubble {
  background: rgba(238, 246, 255, 0.92);
  border: 1px solid rgba(191, 219, 254, 0.95);
  border-radius: 14px;
  padding: 14px;
  color: #1E293B;
  line-height: 1.8;
  white-space: pre-wrap;
}
</style>
