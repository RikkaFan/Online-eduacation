<template>
  <div class="exam-management">
    <el-card class="toolbar glass-card" shadow="never">
      <div class="toolbar-row">
        <div class="actions-left">
          <el-button type="primary" :disabled="!selectedCourseId" @click="openCreateDialog">
            发布新考试
          </el-button>
        </div>
        <div class="actions-right">
          <el-select
            v-model="selectedCourseId"
            placeholder="选择课程"
            filterable
            clearable
            @change="handleCourseChange"
            style="width: 260px"
          >
            <el-option
              v-for="c in courses"
              :key="c.id"
              :label="c.courseName || c.name || c.title || `课程#${c.id}`"
              :value="c.id"
            />
          </el-select>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="mt-12 glass-card">
      <el-table :data="exams" v-loading="loading" empty-text="暂无考试数据" stripe>
        <el-table-column prop="title" label="考试名称" min-width="180">
          <template #default="{ row }">{{ row.title || row.name }}</template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" min-width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="endTime" label="结束时间" min-width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button size="small" type="info" plain @click="openPreview(row)">🔍 预览试卷</el-button>
            <el-popconfirm
              title="确定删除该考试？"
              confirm-button-text="删除"
              cancel-button-text="取消"
              @confirm="onDelete(row)"
            >
              <template #reference>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="createDialogVisible" title="发布新考试" width="520px">
      <el-form :model="createForm" :rules="rules" ref="formRef" label-width="110px">
        <el-form-item label="所属课程" prop="courseId">
          <el-select v-model="createForm.courseId" placeholder="选择课程" filterable>
            <el-option
              v-for="c in courses"
              :key="c.id"
              :label="c.courseName || c.name || c.title || `课程#${c.id}`"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="考试名称" prop="title">
          <el-input v-model="createForm.title" placeholder="请输入考试名称" />
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
            v-model="createForm.startTime"
            type="datetime"
            placeholder="选择开始时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            :default-time="defaultStartTime"
          />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker
            v-model="createForm.endTime"
            type="datetime"
            placeholder="选择结束时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            :default-time="defaultEndTime"
          />
        </el-form-item>
        <el-form-item label="组卷策略">
          <el-radio-group v-model="createForm.generateMode">
            <el-radio-button label="random">🎲 系统自动化组卷</el-radio-button>
            <el-radio-button label="manual">✍️ 手动勾选题目</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <div class="smart-rule-box">
          <div class="smart-rule-title">智能组卷规则配置</div>
          <div v-if="createForm.generateMode === 'random'" class="config-matrix" style="background: rgba(255,255,255,0.5); padding: 15px; border-radius: 8px;">
            <el-row :gutter="10" style="display: flex; align-items: center; text-align: center; margin-bottom: 15px; font-weight: bold; color: #606266;">
              <el-col :span="5" style="text-align: center;">题型</el-col>
              <el-col :span="6">抽取数量</el-col>
              <el-col :span="6">每题分值</el-col>
              <el-col :span="7">题型总分</el-col>
            </el-row>
            <el-row :gutter="10" style="display: flex; align-items: center; text-align: center; margin-bottom: 15px;">
              <el-col :span="5" style="font-weight: 600; text-align: center;">单选题</el-col>
              <el-col :span="6"><el-input-number v-model="createForm.singleCount" :min="0" style="width: 100%" @change="calcTotal('single')" controls-position="right" /></el-col>
              <el-col :span="6"><el-input-number v-model="createForm.singleScore" :min="0" style="width: 100%" @change="calcTotal('single')" controls-position="right" /></el-col>
              <el-col :span="7"><el-input-number v-model="createForm.singleTotal" :min="0" style="width: 100%" @change="calcReverse('single')" controls-position="right" /></el-col>
            </el-row>
            <el-row :gutter="10" style="display: flex; align-items: center; text-align: center; margin-bottom: 15px;">
              <el-col :span="5" style="font-weight: 600; text-align: center;">多选题</el-col>
              <el-col :span="6"><el-input-number v-model="createForm.multipleCount" :min="0" style="width: 100%" @change="calcTotal('multiple')" controls-position="right" /></el-col>
              <el-col :span="6"><el-input-number v-model="createForm.multipleScore" :min="0" style="width: 100%" @change="calcTotal('multiple')" controls-position="right" /></el-col>
              <el-col :span="7"><el-input-number v-model="createForm.multipleTotal" :min="0" style="width: 100%" @change="calcReverse('multiple')" controls-position="right" /></el-col>
            </el-row>
            <el-row :gutter="10" style="display: flex; align-items: center; text-align: center; margin-bottom: 15px;">
              <el-col :span="5" style="font-weight: 600; text-align: center;">判断题</el-col>
              <el-col :span="6"><el-input-number v-model="createForm.judgeCount" :min="0" style="width: 100%" @change="calcTotal('judge')" controls-position="right" /></el-col>
              <el-col :span="6"><el-input-number v-model="createForm.judgeScore" :min="0" style="width: 100%" @change="calcTotal('judge')" controls-position="right" /></el-col>
              <el-col :span="7"><el-input-number v-model="createForm.judgeTotal" :min="0" style="width: 100%" @change="calcReverse('judge')" controls-position="right" /></el-col>
            </el-row>
            <el-row :gutter="10" style="display: flex; align-items: center; text-align: center; margin-bottom: 15px;">
              <el-col :span="5" style="font-weight: 600; text-align: center;">主观题</el-col>
              <el-col :span="6"><el-input-number v-model="createForm.subjectiveCount" :min="0" style="width: 100%" @change="calcTotal('subjective')" controls-position="right" /></el-col>
              <el-col :span="6"><el-input-number v-model="createForm.subjectiveScore" :min="0" style="width: 100%" @change="calcTotal('subjective')" controls-position="right" /></el-col>
              <el-col :span="7"><el-input-number v-model="createForm.subjectiveTotal" :min="0" style="width: 100%" @change="calcReverse('subjective')" controls-position="right" /></el-col>
            </el-row>
            <p class="rule-tip">
              系统将自动按题型规则抽题并组卷，确保每份试卷结构一致且题目组合可随机变化。
            </p>
          </div>
          <div v-if="createForm.generateMode === 'random'" style="text-align: center; margin-top: 25px; margin-bottom: 10px; border-top: 1px dashed #D1D1D6; padding-top: 20px;">
            <span style="font-size: 16px; font-weight: 600; color: #1c1c1e;">当前试卷总分：</span>
            <span style="font-size: 24px; font-weight: 700; color: #007AFF;">{{ (createForm.singleCount * createForm.singleScore) + (createForm.multipleCount * createForm.multipleScore) + (createForm.judgeCount * createForm.judgeScore) + (createForm.subjectiveCount * createForm.subjectiveScore) }}</span>
            <span style="font-size: 16px; font-weight: 600; color: #1c1c1e; margin-left: 4px;">分</span>
          </div>
          <div v-else>
            <div v-if="singleQs.length > 0" style="margin-bottom: 20px;">
              <div style="display: flex; gap: 15px;">
                <div style="flex: 1;">
                  <div style="text-align: center; color: #606266; font-size: 13px; margin-bottom: 8px;">单选题每题分值</div>
                  <el-input-number v-model="createForm.singleScore" :min="0" style="width: 100%" controls-position="right" />
                </div>
                <div style="flex: 3;">
                  <div style="text-align: center; color: #606266; font-size: 13px; margin-bottom: 8px;">勾选题目 (已选 {{ manualSingleIds.length }} 题)</div>
                  <el-select v-model="manualSingleIds" multiple collapse-tags placeholder="请选择单选题" style="width: 100%">
                    <el-option v-for="q in singleQs" :key="q.id" :label="q.content" :value="q.id" />
                  </el-select>
                </div>
              </div>
            </div>
            <div v-if="multipleQs.length > 0" style="margin-bottom: 20px;">
              <div style="display: flex; gap: 15px;">
                <div style="flex: 1;">
                  <div style="text-align: center; color: #606266; font-size: 13px; margin-bottom: 8px;">多选题每题分值</div>
                  <el-input-number v-model="createForm.multipleScore" :min="0" style="width: 100%" controls-position="right" />
                </div>
                <div style="flex: 3;">
                  <div style="text-align: center; color: #606266; font-size: 13px; margin-bottom: 8px;">勾选题目 (已选 {{ manualMultipleIds.length }} 题)</div>
                  <el-select v-model="manualMultipleIds" multiple collapse-tags placeholder="请选择多选题" style="width: 100%">
                    <el-option v-for="q in multipleQs" :key="q.id" :label="q.content" :value="q.id" />
                  </el-select>
                </div>
              </div>
            </div>
            <div v-if="judgeQs.length > 0" style="margin-bottom: 20px;">
              <div style="display: flex; gap: 15px;">
                <div style="flex: 1;">
                  <div style="text-align: center; color: #606266; font-size: 13px; margin-bottom: 8px;">判断题每题分值</div>
                  <el-input-number v-model="createForm.judgeScore" :min="0" style="width: 100%" controls-position="right" />
                </div>
                <div style="flex: 3;">
                  <div style="text-align: center; color: #606266; font-size: 13px; margin-bottom: 8px;">勾选题目 (已选 {{ manualJudgeIds.length }} 题)</div>
                  <el-select v-model="manualJudgeIds" multiple collapse-tags placeholder="请选择判断题" style="width: 100%">
                    <el-option v-for="q in judgeQs" :key="q.id" :label="q.content" :value="q.id" />
                  </el-select>
                </div>
              </div>
            </div>
            <div v-if="subjectiveQs.length > 0" style="margin-bottom: 20px;">
              <div style="display: flex; gap: 15px;">
                <div style="flex: 1;">
                  <div style="text-align: center; color: #606266; font-size: 13px; margin-bottom: 8px;">主观题每题分值</div>
                  <el-input-number v-model="createForm.subjectiveScore" :min="0" style="width: 100%" controls-position="right" />
                </div>
                <div style="flex: 3;">
                  <div style="text-align: center; color: #606266; font-size: 13px; margin-bottom: 8px;">勾选题目 (已选 {{ manualSubjectiveIds.length }} 题)</div>
                  <el-select v-model="manualSubjectiveIds" multiple collapse-tags placeholder="请选择主观题" style="width: 100%">
                    <el-option v-for="q in subjectiveQs" :key="q.id" :label="q.content" :value="q.id" />
                  </el-select>
                </div>
              </div>
            </div>
            <div class="total-preview">当前试卷总分：{{ totalScore }} 分</div>
          </div>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreate">确认发布</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="previewVisible" title="📝 试卷全真预览" size="50%">
      <div v-loading="previewLoading" class="preview-container">
        <div v-if="previewQuestions.length === 0" class="preview-empty">暂无题目数据</div>
        <div v-else>
          <div v-for="(q, idx) in previewQuestions" :key="q.id || idx" class="glass-card preview-question">
            <div class="preview-title">
              <span class="preview-no">{{ idx + 1 }}.</span>
              <span>{{ q.content }}</span>
              <el-tag size="small" class="preview-tag">{{ previewTypeName(q.type) }}</el-tag>
            </div>

            <template v-if="q.type === 'SUBJECTIVE'">
              <el-input
                type="textarea"
                :rows="4"
                disabled
                placeholder="主观题作答区（预览）"
                class="mt-12"
              ></el-input>
            </template>
            <template v-else-if="q.type === 'JUDGE'">
              <el-radio-group class="options-group" disabled>
                <el-radio label="T">正确 (True)</el-radio>
                <el-radio label="F">错误 (False)</el-radio>
              </el-radio-group>
            </template>
            <template v-else-if="isMultipleType(q.type)">
              <el-checkbox-group class="options-group" disabled>
                <el-checkbox v-for="opt in parsePreviewOptions(q)" :key="opt.key" :label="opt.key">{{ opt.key }}. {{ opt.text }}</el-checkbox>
              </el-checkbox-group>
            </template>
            <template v-else>
              <el-radio-group class="options-group" disabled>
                <el-radio v-for="opt in parsePreviewOptions(q)" :key="opt.key" :label="opt.key">{{ opt.key }}. {{ opt.text }}</el-radio>
              </el-radio-group>
            </template>

            <div class="answer-tip">
              <div>💡 标准答案：{{ q.answer || '暂无' }}</div>
              <div v-if="q.type === 'SUBJECTIVE'">💡 解析：{{ q.analysis || '暂无' }}</div>
            </div>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch, computed } from 'vue';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import { getCourses } from '@/api/course';
import { getExamsByCourse, createExam, deleteExam, getExamQuestions } from '@/api/exam';
import { getQuestionsByCourse } from '@/api/question';

const route = useRoute();
const router = useRouter();
const courses = ref([]);
const selectedCourseId = ref(null);
const exams = ref([]);
const loading = ref(false);
const availableQuestions = ref([]);

const createDialogVisible = ref(false);
const submitting = ref(false);
const formRef = ref(null);
const previewVisible = ref(false);
const previewLoading = ref(false);
const previewQuestions = ref([]);

const createForm = reactive({
  courseId: null,
  title: '',
  startTime: '',
  endTime: '',
  singleCount: 0,
  singleScore: 0,
  singleTotal: 0,
  multipleCount: 0,
  multipleScore: 0,
  multipleTotal: 0,
  judgeCount: 0,
  judgeScore: 0,
  judgeTotal: 0,
  subjectiveCount: 0,
  subjectiveScore: 0,
  subjectiveTotal: 0,
  generateMode: 'random',
  questionIds: []
});

const manualSingleIds = ref([]);
const manualMultipleIds = ref([]);
const manualJudgeIds = ref([]);
const manualSubjectiveIds = ref([]);

const calcTotal = (type) => {
  createForm[`${type}Total`] = (createForm[`${type}Count`] || 0) * (createForm[`${type}Score`] || 0);
};

const calcReverse = (type) => {
  const total = createForm[`${type}Total`] || 0;
  const count = createForm[`${type}Count`] || 0;
  if (count > 0) {
    createForm[`${type}Score`] = Number((total / count).toFixed(1));
  }
};

const totalScore = computed(() => (
  ((createForm.generateMode === 'manual' ? manualSingleIds.value.length : createForm.singleCount) * createForm.singleScore)
  + ((createForm.generateMode === 'manual' ? manualMultipleIds.value.length : createForm.multipleCount) * createForm.multipleScore)
  + ((createForm.generateMode === 'manual' ? manualJudgeIds.value.length : createForm.judgeCount) * createForm.judgeScore)
  + ((createForm.generateMode === 'manual' ? manualSubjectiveIds.value.length : createForm.subjectiveCount) * createForm.subjectiveScore)
));

const totalQuestionCount = computed(() => (
  createForm.singleCount
  + createForm.multipleCount
  + createForm.judgeCount
  + createForm.subjectiveCount
));

const singleQs = computed(() => availableQuestions.value.filter(q => !q.type || q.type === 'SINGLE'));
const multipleQs = computed(() => availableQuestions.value.filter(q => q.type === 'MULTIPLE'));
const judgeQs = computed(() => availableQuestions.value.filter(q => q.type === 'JUDGE'));
const subjectiveQs = computed(() => availableQuestions.value.filter(q => q.type === 'SUBJECTIVE'));

const rules = {
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  title: [{ required: true, message: '请输入考试名称', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
};

const defaultStartTime = new Date(2000, 1, 1, 9, 0, 0);
const defaultEndTime = new Date(2000, 1, 1, 11, 0, 0);

function formatDateTime(val) {
  if (!val) return '-';
  try {
    const d = typeof val === 'string' ? new Date(val) : val;
    if (Number.isNaN(d.getTime())) return val;
    return d.toLocaleString();
  } catch (e) {
    return String(val);
  }
}

async function loadCourses() {
  try {
    const data = await getCourses();
    courses.value = data || [];
  } catch (e) {
    ElMessage.error(e.message || '加载课程失败');
  }
}

async function loadExams() {
  if (!selectedCourseId.value) {
    exams.value = [];
    return;
  }
  loading.value = true;
  try {
    const data = await getExamsByCourse(selectedCourseId.value);
    exams.value = Array.isArray(data) ? data : [];
  } catch (e) {
    ElMessage.error(e.message || '加载考试列表失败');
  } finally {
    loading.value = false;
  }
}

function handleCourseChange() {
  loadExams();
}

function isMultipleType(type) {
  const normalized = String(type || '').toUpperCase();
  return normalized === 'MULTIPLE' || normalized === 'MULTIPLE_CHOICE';
}

function previewTypeName(type) {
  const normalized = String(type || '').toUpperCase();
  if (normalized === 'SUBJECTIVE') return '主观题';
  if (normalized === 'JUDGE') return '判断题';
  return isMultipleType(normalized) ? '多选题' : '单选题';
}

function parsePreviewOptions(q) {
  const raw = q.options || '';
  return raw.split(',').map(s => s.trim()).filter(Boolean).map(chunk => {
    const idx = chunk.indexOf(':');
    if (idx > 0) {
      return { key: chunk.slice(0, idx).trim(), text: chunk.slice(idx + 1).trim() };
    }
    const dotIndex = chunk.indexOf('.');
    if (dotIndex > 0) {
      return { key: chunk.slice(0, dotIndex).trim(), text: chunk.slice(dotIndex + 1).trim() };
    }
    return { key: chunk, text: chunk };
  });
}

async function loadAvailableQuestions(courseId) {
  if (!courseId) {
    availableQuestions.value = [];
    return;
  }
  try {
    const data = await getQuestionsByCourse(courseId);
    availableQuestions.value = Array.isArray(data) ? data : [];
  } catch (e) {
    ElMessage.error(e.message || '加载课程题库失败');
    availableQuestions.value = [];
  }
}

function openCreateDialog() {
  createForm.courseId = selectedCourseId.value || null;
  createForm.title = '';
  createForm.startTime = '';
  createForm.endTime = '';
  createForm.singleCount = 0;
  createForm.singleScore = 0;
  createForm.singleTotal = 0;
  createForm.multipleCount = 0;
  createForm.multipleScore = 0;
  createForm.multipleTotal = 0;
  createForm.judgeCount = 0;
  createForm.judgeScore = 0;
  createForm.judgeTotal = 0;
  createForm.subjectiveCount = 0;
  createForm.subjectiveScore = 0;
  createForm.subjectiveTotal = 0;
  createForm.generateMode = 'random';
  createForm.questionIds = [];
  manualSingleIds.value = [];
  manualMultipleIds.value = [];
  manualJudgeIds.value = [];
  manualSubjectiveIds.value = [];
  loadAvailableQuestions(createForm.courseId);
  createDialogVisible.value = true;
}

async function submitCreate() {
  if (!formRef.value) return;
  try {
    await formRef.value.validate();
  } catch {
    return;
  }
  submitting.value = true;
  try {
    const examPayload = {
      title: createForm.title,
      startTime: createForm.startTime,
      endTime: createForm.endTime,
      singleCount: createForm.singleCount,
      singleScore: createForm.singleScore,
      multipleCount: createForm.multipleCount,
      multipleScore: createForm.multipleScore,
      judgeCount: createForm.judgeCount,
      judgeScore: createForm.judgeScore,
      subjectiveCount: createForm.subjectiveCount,
      subjectiveScore: createForm.subjectiveScore,
      totalScore: totalScore.value
    };
    let numberOfQuestions = totalQuestionCount.value;
    if (createForm.generateMode === 'manual') {
      createForm.singleCount = manualSingleIds.value.length;
      createForm.multipleCount = manualMultipleIds.value.length;
      createForm.judgeCount = manualJudgeIds.value.length;
      createForm.subjectiveCount = manualSubjectiveIds.value.length;
      createForm.questionIds = [
        ...manualSingleIds.value,
        ...manualMultipleIds.value,
        ...manualJudgeIds.value,
        ...manualSubjectiveIds.value
      ];
      createForm.totalScore = (createForm.singleCount * createForm.singleScore)
        + (createForm.multipleCount * createForm.multipleScore)
        + (createForm.judgeCount * createForm.judgeScore)
        + (createForm.subjectiveCount * createForm.subjectiveScore);
      examPayload.singleCount = createForm.singleCount;
      examPayload.multipleCount = createForm.multipleCount;
      examPayload.judgeCount = createForm.judgeCount;
      examPayload.subjectiveCount = createForm.subjectiveCount;
      if (!createForm.questionIds || createForm.questionIds.length === 0) {
        ElMessage.warning('请至少勾选一道题目');
        submitting.value = false;
        return;
      }
      examPayload.questionIds = [...createForm.questionIds];
      examPayload.totalScore = createForm.totalScore;
      numberOfQuestions = 0;
    } else if (numberOfQuestions <= 0) {
      ElMessage.warning('请至少配置一道题目的抽取数量');
      submitting.value = false;
      return;
    }
    await createExam(createForm.courseId, examPayload, numberOfQuestions);
    ElMessage.success('发布考试成功');
    createDialogVisible.value = false;
    await loadExams();
  } catch (e) {
    ElMessage.error(e.message || '发布考试失败');
  } finally {
    submitting.value = false;
  }
}

async function onDelete(row) {
  try {
    await deleteExam(row.id);
    ElMessage.success('删除成功');
    await loadExams();
  } catch (e) {
    ElMessage.error(e.message || '删除失败');
  }
}

async function openPreview(row) {
  previewVisible.value = true;
  previewLoading.value = true;
  previewQuestions.value = [];
  try {
    const data = await getExamQuestions(row.id);
    previewQuestions.value = Array.isArray(data) ? data : [];
  } catch (e) {
    ElMessage.error(e.message || '加载试卷预览失败');
  } finally {
    previewLoading.value = false;
  }
}

onMounted(async () => {
  await loadCourses();
  if (!selectedCourseId.value && courses.value.length > 0) {
    selectedCourseId.value = courses.value[0].id;
    await loadExams();
  }
  if (route.query.create === '1') {
    openCreateDialog();
    const nextQuery = { ...route.query };
    delete nextQuery.create;
    router.replace({ path: route.path, query: nextQuery });
  }
});

watch(() => createForm.courseId, (value) => {
  createForm.questionIds = [];
  manualSingleIds.value = [];
  manualMultipleIds.value = [];
  manualJudgeIds.value = [];
  manualSubjectiveIds.value = [];
  loadAvailableQuestions(value);
});

</script>

<style scoped>
.toolbar {
  margin-bottom: 12px;
}
.toolbar-row {
  display: flex;
  gap: 12px;
  align-items: center;
}
.mt-12 {
  margin-top: 12px;
}
.smart-rule-box {
  border: 1px dashed #bfdbfe;
  background: #f8fbff;
  border-radius: 12px;
  padding: 12px 12px 2px;
}
.smart-rule-title {
  color: #1d4ed8;
  font-weight: 600;
  margin-bottom: 8px;
}
.rule-item {
  margin-bottom: 12px;
}
.matrix-wrap {
  width: 100%;
}
.matrix-header,
.matrix-row {
  width: 100%;
  align-items: center;
  padding: 6px 0;
}
.matrix-header {
  color: #334155;
  font-weight: 600;
  border-bottom: 1px dashed #cbd5e1;
  margin-bottom: 6px;
}
.matrix-row {
  border-bottom: 1px dashed #e2e8f0;
}
.total-preview {
  margin-top: 10px;
  display: inline-block;
  background: #eff6ff;
  color: #1d4ed8;
  border: 1px solid #bfdbfe;
  border-radius: 10px;
  padding: 6px 10px;
  font-weight: 600;
}
.count-config {
  width: 100%;
}
.rule-tip {
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: #64748b;
}
.preview-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.preview-empty {
  color: #64748b;
}
.preview-question {
  padding: 14px;
}
.preview-title {
  display: flex;
  align-items: center;
  gap: 8px;
}
.preview-no {
  color: #0a84ff;
  font-weight: 600;
}
.preview-tag {
  margin-left: auto;
}
.answer-tip {
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  color: #065f46;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
</style>
