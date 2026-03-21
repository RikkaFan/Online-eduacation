<template>
  <div class="teacher-grading">
    <el-row :gutter="16">
      <el-col :xs="24" :md="8" :lg="7">
        <el-card class="glass-card list-card" shadow="never">
          <el-tabs v-model="activeTab" @tab-change="loadList">
            <el-tab-pane label="待批改" name="pending" />
            <el-tab-pane label="已批阅" name="graded" />
          </el-tabs>
          <div class="panel-head">
            <h3>{{ activeTab === 'pending' ? '待批改列表' : '已批阅列表' }}</h3>
            <el-tag :type="activeTab === 'pending' ? 'warning' : 'success'" effect="light">{{ paperList.length }}</el-tag>
          </div>
          <div v-loading="loading" class="paper-list">
            <div v-if="!loading && paperList.length === 0" class="empty-state">
              {{ activeTab === 'pending' ? '当前暂无待批改主观题' : '当前暂无已批阅主观题试卷' }}
            </div>
            <div
              v-for="paper in paperList"
              :key="paper.key"
              class="paper-item"
              :class="{ active: selectedPaperKey === paper.key }"
              @click="selectedPaperKey = paper.key"
            >
              <div class="paper-title">{{ paper.studentName }}</div>
              <div class="paper-meta">{{ paper.examTitle }}</div>
              <div class="paper-meta">交卷时间：{{ formatDateTime(paper.submitTime) }}</div>
              <div class="paper-count">
                {{ activeTab === 'pending' ? '待批改主观题' : '已评阅主观题' }}：{{ paper.questions.length }}
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="16" :lg="17">
        <el-card class="glass-card work-card" shadow="never">
          <template v-if="selectedPaper">
            <div class="panel-head">
              <h3>{{ selectedPaper.studentName }} - {{ selectedPaper.examTitle }}</h3>
              <el-tag type="primary" effect="light">主观题 {{ selectedPaper.questions.length }} 道</el-tag>
            </div>
            <div class="question-list">
              <div v-for="q in selectedPaper.questions" :key="q.id" class="glass-card question-card">
                <div class="field">
                  <div class="label">题目内容</div>
                  <div class="content">{{ q.questionContent || '-' }}</div>
                </div>
                <div class="field">
                  <div class="label">标准答案</div>
                  <div class="content">{{ q.standardAnswer || '-' }}</div>
                </div>
                <div class="field">
                  <div class="label">学生回答</div>
                  <div class="content">{{ q.studentAnswer || '未作答' }}</div>
                </div>
                <div v-if="activeTab === 'graded'" class="score-view">
                  <span class="score-label">给出分数</span>
                  <span class="score-value">{{ q.giveScore }} / {{ q.maxScore }}</span>
                </div>
                <div class="ops-bar">
                  <el-input-number
                    v-model="q.giveScore"
                    :max="q.maxScore"
                    :min="0"
                    label="手动打分"
                    :precision="0"
                    :disabled="activeTab === 'graded'"
                  />
                  <el-button
                    type="success"
                    plain
                    :icon="Lightning"
                    :loading="Boolean(aiLoadingMap[q.id])"
                    :disabled="activeTab === 'graded'"
                    @click="handleAIAutoGrade(q)"
                  >
                    AI 智能批改
                  </el-button>
                  <el-button
                    type="primary"
                    :loading="Boolean(manualLoadingMap[q.id])"
                    :disabled="activeTab === 'graded'"
                    @click="handleManualGrade(q)"
                  >
                    确认给分
                  </el-button>
                </div>
              </div>
            </div>
          </template>
          <div v-else class="empty-state work-empty">
            {{ activeTab === 'pending' ? '请先在左侧选择一份待批改试卷' : '请先在左侧选择一份已批阅试卷' }}
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { Lightning } from '@element-plus/icons-vue';
import { autoGradeWithAI, getGradedResults, getPendingGrading, manualGrade } from '@/api/score';

const loading = ref(false);
const paperList = ref([]);
const selectedPaperKey = ref('');
const activeTab = ref('pending');
const aiLoadingMap = reactive({});
const manualLoadingMap = reactive({});

const selectedPaper = computed(() => (
  paperList.value.find(item => item.key === selectedPaperKey.value) || null
));

onMounted(() => {
  loadList();
});

function toNumber(value, fallback = 0) {
  const n = Number(value);
  return Number.isFinite(n) ? n : fallback;
}

function buildQuestionItem(answer) {
  const maxScore = toNumber(answer?.exam?.subjectiveScore, 0);
  return {
    id: answer?.id,
    questionContent: answer?.question?.content || '',
    standardAnswer: answer?.question?.answer || '',
    studentAnswer: answer?.selectedAnswer || '',
    maxScore: maxScore > 0 ? maxScore : 100,
    giveScore: toNumber(answer?.score, 0),
  };
}

function normalizeSubmitTime(answer) {
  return answer?.submitTime
    || answer?.submitAt
    || answer?.createTime
    || answer?.createAt
    || answer?.exam?.endTime
    || '';
}

function buildPaperList(data) {
  const grouped = new Map();
  (Array.isArray(data) ? data : []).forEach(answer => {
    const examId = answer?.exam?.id || 'unknown-exam';
    const studentId = answer?.student?.id || 'unknown-student';
    const key = `${examId}-${studentId}`;
    if (!grouped.has(key)) {
      grouped.set(key, {
        key,
        examTitle: answer?.exam?.title || `考试#${examId}`,
        studentName: answer?.student?.username || `学生#${studentId}`,
        submitTime: normalizeSubmitTime(answer),
        questions: [],
      });
    }
    grouped.get(key).questions.push(buildQuestionItem(answer));
  });
  return Array.from(grouped.values());
}

async function loadList() {
  loading.value = true;
  try {
    const data = activeTab.value === 'pending'
      ? await getPendingGrading()
      : await getGradedResults();
    const list = buildPaperList(data);
    paperList.value = list;
    if (!selectedPaperKey.value || !list.some(item => item.key === selectedPaperKey.value)) {
      selectedPaperKey.value = list[0]?.key || '';
    }
  } catch (e) {
    ElMessage.error(e.message || '加载试卷列表失败');
    paperList.value = [];
    selectedPaperKey.value = '';
  } finally {
    loading.value = false;
  }
}

async function handleAIAutoGrade(question) {
  if (!question?.id || activeTab.value === 'graded') return;
  aiLoadingMap[question.id] = true;
  try {
    const res = await autoGradeWithAI(question.id);
    const finalScore = res?.score ?? res?.data?.score ?? res;
    if (finalScore !== undefined && finalScore !== null) {
      question.giveScore = Number(finalScore);
      ElMessage.success(`AI 批改成功，建议给分：${question.giveScore} 分`);
      await loadList();
    } else {
      ElMessage.warning('未能读取到分数，请检查后端返回格式');
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || 'AI 批改超时或出错，请稍后重试');
  } finally {
    aiLoadingMap[question.id] = false;
  }
}

async function handleManualGrade(question) {
  if (!question?.id || activeTab.value === 'graded') return;
  manualLoadingMap[question.id] = true;
  try {
    const result = await manualGrade(question.id, toNumber(question.giveScore, 0));
    question.giveScore = toNumber(result?.score, question.giveScore);
    ElMessage.success('给分已保存');
    await loadList();
  } catch (e) {
    ElMessage.error(e.message || '手动给分失败');
  } finally {
    manualLoadingMap[question.id] = false;
  }
}

function formatDateTime(value) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleString();
}
</script>

<style scoped>
.teacher-grading {
  display: grid;
}

.list-card,
.work-card {
  min-height: calc(100vh - 132px);
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.panel-head h3 {
  margin: 0;
  color: #0F172A;
  font-size: 17px;
  font-weight: 600;
}

.paper-list {
  display: grid;
  gap: 10px;
  max-height: calc(100vh - 210px);
  overflow-y: auto;
  padding-right: 2px;
}

.paper-item {
  border-radius: 14px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  background: rgba(255, 255, 255, 0.72);
  padding: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.paper-item:hover {
  border-color: rgba(99, 102, 241, 0.45);
  transform: translateY(-1px);
}

.paper-item.active {
  border-color: rgba(79, 70, 229, 0.6);
  background: rgba(238, 242, 255, 0.82);
  box-shadow: 0 8px 20px rgba(79, 70, 229, 0.12);
}

.paper-title {
  color: #0F172A;
  font-weight: 600;
}

.paper-meta {
  margin-top: 4px;
  color: #64748B;
  font-size: 13px;
}

.paper-count {
  margin-top: 8px;
  color: #4F46E5;
  font-size: 13px;
}

.question-list {
  display: grid;
  gap: 12px;
  max-height: calc(100vh - 220px);
  overflow-y: auto;
  padding-right: 2px;
}

.question-card {
  padding: 14px;
  border-radius: 14px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  background: rgba(255, 255, 255, 0.72);
}

.field + .field {
  margin-top: 10px;
}

.label {
  color: #64748B;
  font-size: 13px;
}

.content {
  margin-top: 4px;
  color: #1E293B;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.ops-bar {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.score-view {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.score-label {
  color: #64748B;
  font-size: 13px;
}

.score-value {
  color: #4F46E5;
  font-weight: 600;
}

.empty-state {
  color: #64748B;
  text-align: center;
  padding: 24px 0;
}

.work-empty {
  padding-top: 80px;
}
</style>
