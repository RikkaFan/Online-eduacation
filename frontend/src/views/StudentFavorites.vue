<template>
  <div class="student-favorites">
    <el-card class="glass-card header-card" shadow="never">
      <div class="page-title">我的题目收藏本</div>
      <div class="page-sub">聚合重点题目，随时回看巩固。</div>
    </el-card>

    <el-empty v-if="!loading && favorites.length === 0" description="你还没有收藏题目" />

    <div class="favorite-list" v-loading="loading">
      <el-card v-for="item in favorites" :key="item.id" class="glass-card favorite-card" shadow="never">
        <div class="card-top">
          <el-tag effect="plain" size="small">{{ typeName(item.question?.type) }}</el-tag>
          <el-button type="danger" plain @click="removeFavorite(item)">取消收藏</el-button>
        </div>
        <div class="question-content">{{ item.question?.content || '-' }}</div>
        <div class="block-title">选项</div>
        <div class="option-list">
          <div v-for="(op, idx) in optionItems(item.question?.options)" :key="`${item.id}-${idx}`" class="option-item">
            {{ op }}
          </div>
          <div v-if="optionItems(item.question?.options).length === 0" class="option-item">-</div>
        </div>
        <div class="answer-row">
          <span class="label">正确答案：</span>
          <span class="value right">{{ item.question?.answer || '-' }}</span>
        </div>
        <div class="analysis-row">
          <span class="label">解析：</span>
          <span class="value">{{ item.question?.analysis || item.question?.explanation || '-' }}</span>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { getFavorites, toggleFavorite } from '@/api/favorite';
import { parseOptionDisplayItems } from '@/utils/questionOptions';

const loading = ref(false);
const favorites = ref([]);

function typeName(type) {
  if (type === 'SINGLE_CHOICE') return '单选题';
  if (type === 'MULTIPLE_CHOICE') return '多选题';
  if (type === 'TRUE_FALSE') return '判断题';
  return type || '未知题型';
}

function optionItems(options) {
  return parseOptionDisplayItems(options || '');
}

async function loadFavorites() {
  loading.value = true;
  try {
    const data = await getFavorites();
    favorites.value = Array.isArray(data) ? data : [];
  } catch (e) {
    ElMessage.error(e.message || '加载收藏列表失败');
    favorites.value = [];
  } finally {
    loading.value = false;
  }
}

async function removeFavorite(item) {
  const qid = item?.question?.id;
  if (!qid) return;
  try {
    await toggleFavorite(qid);
    favorites.value = favorites.value.filter(f => f.id !== item.id);
    ElMessage.success('已取消收藏');
  } catch (e) {
    ElMessage.error(e.message || '取消收藏失败');
  }
}

onMounted(loadFavorites);
</script>

<style scoped>
.student-favorites {
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
.header-card {
  padding: 22px 24px;
}
.page-title {
  font-size: 26px;
  color: #0F172A;
  font-weight: 700;
}
.page-sub {
  margin-top: 6px;
  color: #64748B;
}
.favorite-list {
  display: grid;
  gap: 16px;
}
.favorite-card {
  padding: 18px;
}
.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.question-content {
  color: #0F172A;
  line-height: 1.75;
  font-size: 15px;
  margin-bottom: 10px;
}
.block-title {
  color: #334155;
  font-weight: 600;
  margin-bottom: 6px;
}
.option-list {
  display: grid;
  gap: 4px;
  margin-bottom: 10px;
}
.option-item {
  color: #1E293B;
  line-height: 1.6;
}
.label {
  color: #64748B;
}
.value {
  color: #1E293B;
}
.right {
  color: #188038;
  font-weight: 600;
}
.answer-row,
.analysis-row {
  margin-top: 8px;
  line-height: 1.6;
}
</style>
