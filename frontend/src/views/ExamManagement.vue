<template>
  <div class="exam-management">
    <el-card class="toolbar" shadow="never">
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

    <el-card shadow="never" class="mt-12">
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
        <el-form-item label="出题方式">
          <el-radio-group v-model="createForm.questionMode">
            <el-radio label="random">随机组卷</el-radio>
            <el-radio label="manual">题库选题</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="createForm.questionMode === 'random'" label="题目数量" prop="numberOfQuestions">
          <el-input-number v-model="createForm.numberOfQuestions" :min="1" :max="500" />
        </el-form-item>
        <el-form-item v-else label="选择题目">
          <el-select
            v-model="createForm.selectedQuestionIds"
            multiple
            filterable
            clearable
            collapse-tags
            collapse-tags-tooltip
            placeholder="从题库中选择题目"
            style="width: 100%;"
          >
            <el-option
              v-for="q in questionOptions"
              :key="q.id"
              :label="`#${q.id} ${q.content || '未命名题目'}`"
              :value="q.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreate">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue';
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
const questionOptions = ref([]);

const createDialogVisible = ref(false);
const submitting = ref(false);
const formRef = ref(null);

const createForm = reactive({
  courseId: null,
  title: '',
  startTime: '',
  endTime: '',
  numberOfQuestions: 10,
  questionMode: 'random',
  selectedQuestionIds: []
});

const rules = {
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  title: [{ required: true, message: '请输入考试名称', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  numberOfQuestions: [{ required: true, message: '请输入题目数量', trigger: 'change' }],
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
  loadQuestionOptions(selectedCourseId.value);
}

function openCreateDialog() {
  createForm.courseId = selectedCourseId.value || null;
  createForm.title = '';
  createForm.startTime = '';
  createForm.endTime = '';
  createForm.numberOfQuestions = 10;
  createForm.questionMode = 'random';
  createForm.selectedQuestionIds = [];
  createDialogVisible.value = true;
}

async function loadQuestionOptions(courseId) {
  if (!courseId) {
    questionOptions.value = [];
    return;
  }
  try {
    const data = await getQuestionsByCourse(courseId);
    questionOptions.value = Array.isArray(data) ? data : [];
  } catch (e) {
    ElMessage.error(e.message || '加载题库题目失败');
    questionOptions.value = [];
  }
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
    if (createForm.questionMode === 'manual' && (!createForm.selectedQuestionIds || createForm.selectedQuestionIds.length === 0)) {
      ElMessage.warning('请选择至少一道题目');
      submitting.value = false;
      return;
    }
    const examPayload = {
      title: createForm.title,
      startTime: createForm.startTime,
      endTime: createForm.endTime
    };
    if (createForm.questionMode === 'manual') {
      examPayload.questions = createForm.selectedQuestionIds.map(id => ({ id }));
    }
    const count = createForm.questionMode === 'random' ? createForm.numberOfQuestions : 0;
    await createExam(createForm.courseId, examPayload, count);
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
  await loadQuestionOptions(selectedCourseId.value);
  if (route.query.create === '1') {
    openCreateDialog();
    const nextQuery = { ...route.query };
    delete nextQuery.create;
    router.replace({ path: route.path, query: nextQuery });
  }
});

watch(() => createForm.courseId, (val) => {
  loadQuestionOptions(val);
  createForm.selectedQuestionIds = [];
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
</style>
