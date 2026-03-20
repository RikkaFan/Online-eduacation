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
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
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
            <el-radio-button label="random">🎲 系统随机抽题 (防作弊)</el-radio-button>
            <el-radio-button label="manual">✍️ 手动勾选题目</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <div class="smart-rule-box">
          <div class="smart-rule-title">智能组卷规则配置</div>
          <div v-if="createForm.generateMode === 'random'" class="matrix-wrap">
            <el-row class="matrix-header" :gutter="12">
              <el-col :span="8">题型</el-col>
              <el-col :span="8">抽取数量</el-col>
              <el-col :span="8">每题分值</el-col>
            </el-row>
            <el-row class="matrix-row" :gutter="12">
              <el-col :span="8">单选题</el-col>
              <el-col :span="8"><el-input-number v-model="createForm.singleCount" :min="0" :max="200" /></el-col>
              <el-col :span="8"><el-input-number v-model="createForm.singleScore" :min="0" :max="100" /></el-col>
            </el-row>
            <el-row class="matrix-row" :gutter="12">
              <el-col :span="8">多选题</el-col>
              <el-col :span="8"><el-input-number v-model="createForm.multipleCount" :min="0" :max="200" /></el-col>
              <el-col :span="8"><el-input-number v-model="createForm.multipleScore" :min="0" :max="100" /></el-col>
            </el-row>
            <el-row class="matrix-row" :gutter="12">
              <el-col :span="8">判断题</el-col>
              <el-col :span="8"><el-input-number v-model="createForm.judgeCount" :min="0" :max="200" /></el-col>
              <el-col :span="8"><el-input-number v-model="createForm.judgeScore" :min="0" :max="100" /></el-col>
            </el-row>
            <el-row class="matrix-row" :gutter="12">
              <el-col :span="8">主观题</el-col>
              <el-col :span="8"><el-input-number v-model="createForm.subjectiveCount" :min="0" :max="200" /></el-col>
              <el-col :span="8"><el-input-number v-model="createForm.subjectiveScore" :min="0" :max="100" /></el-col>
            </el-row>
            <div class="total-preview">当前试卷总分：{{ totalScore }} 分</div>
            <p class="rule-tip">
              系统将自动按题型规则抽题并组卷，确保每份试卷结构一致且题目组合可随机变化。
            </p>
          </div>
          <el-form-item v-else label="选择客观题" class="rule-item">
            <div class="count-config">
              <el-select
                v-model="createForm.questionIds"
                multiple
                collapse-tags
                collapse-tags-tooltip
                placeholder="请从该课程题库中手动勾选题目"
                style="width: 100%;"
              >
                <el-option
                  v-for="q in availableQuestions"
                  :key="q.id"
                  :label="q.content"
                  :value="q.id"
                />
              </el-select>
            </div>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreate">确认发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch, computed } from 'vue';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import { getCourses } from '@/api/course';
import { getExamsByCourse, createExam, deleteExam } from '@/api/exam';
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

const createForm = reactive({
  courseId: null,
  title: '',
  startTime: '',
  endTime: '',
  singleCount: 0,
  singleScore: 0,
  multipleCount: 0,
  multipleScore: 0,
  judgeCount: 0,
  judgeScore: 0,
  subjectiveCount: 0,
  subjectiveScore: 0,
  generateMode: 'random',
  questionIds: []
});

const totalScore = computed(() => (
  (createForm.singleCount * createForm.singleScore)
  + (createForm.multipleCount * createForm.multipleScore)
  + (createForm.judgeCount * createForm.judgeScore)
  + (createForm.subjectiveCount * createForm.subjectiveScore)
));

const totalQuestionCount = computed(() => (
  createForm.singleCount
  + createForm.multipleCount
  + createForm.judgeCount
  + createForm.subjectiveCount
));

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
  createForm.multipleCount = 0;
  createForm.multipleScore = 0;
  createForm.judgeCount = 0;
  createForm.judgeScore = 0;
  createForm.subjectiveCount = 0;
  createForm.subjectiveScore = 0;
  createForm.generateMode = 'random';
  createForm.questionIds = [];
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
      if (!createForm.questionIds || createForm.questionIds.length === 0) {
        ElMessage.warning('请至少勾选一道题目');
        submitting.value = false;
        return;
      }
      examPayload.questionIds = [...createForm.questionIds];
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
</style>
