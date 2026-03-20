<template>
  <div class="question-management-container">
    <el-row :gutter="20" class="full-height">
      <el-col :span="6" class="category-panel">
        <div class="panel-header">
          <h3>课程题库</h3>
        </div>
        <div class="category-list">
          <el-menu :default-active="selectedCourseId?.toString()" @select="handleCourseSelect" class="el-menu-vertical apple-menu">
            <el-menu-item
              v-for="course in courses"
              :key="course.id"
              :index="course.id.toString()"
            >
              <div class="category-item-content">
                <span class="category-name" :title="course.courseName || `课程#${course.id}`">{{ course.courseName || `课程#${course.id}` }}</span>
              </div>
            </el-menu-item>
          </el-menu>
          <div v-if="courses.length === 0" class="empty-tip">
            暂无课程，请先在课程管理中创建。
          </div>
        </div>
      </el-col>

      <el-col :span="18" class="question-panel">
        <div class="panel-header">
          <h3>题目列表 <span v-if="activeCourseName">- {{ activeCourseName }}</span></h3>
          <div class="action-buttons">
            <el-button size="small" @click="handleDownloadTemplate">下载导入模板</el-button>
            <el-upload accept=".xlsx,.xls" :show-file-list="false" :before-upload="handleImport">
              <el-button type="success" size="small">批量导入题目</el-button>
            </el-upload>
            <el-button type="primary" size="small" :disabled="!selectedCourseId" @click="showAddQuestionDialog">新增题目</el-button>
          </div>
        </div>

        <el-table :data="questions" v-loading="loadingQuestions" style="width: 100%" border stripe>
          <el-table-column prop="id" label="ID" width="60" align="center" />
          <el-table-column prop="content" label="题干" min-width="250" show-overflow-tooltip />
          <el-table-column label="题型" width="120">
            <template #default="{ row }">
              <el-tag type="info" effect="plain">{{ rowTypeName(row.type) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="options" label="选项" min-width="150" show-overflow-tooltip />
          <el-table-column label="正确答案" min-width="260">
            <template #default="{ row }">
              <template v-if="normalizeType(row.type) === 'SUBJECTIVE'">
                <div class="answer-cell">
                  <el-tooltip
                    effect="dark"
                    placement="top"
                    :content="row.answer || '暂无答案'"
                    raw-content
                  >
                    <span class="answer-ellipsis">{{ answerPreview(row.answer) }}</span>
                  </el-tooltip>
                  <el-button link type="primary" size="small" @click="openAnswerPreview(row)">查看</el-button>
                </div>
              </template>
              <template v-else>
                <span>{{ row.answer || '-' }}</span>
              </template>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="center">
            <template #default="{ row }">
              <el-button link type="danger" @click="handleDeleteQuestion(row.id)">删除</el-button>
            </template>
          </el-table-column>
          <template #empty>
            <div v-if="!selectedCourseId">请先在左侧选择课程</div>
            <div v-else>该课程下暂无题目</div>
          </template>
        </el-table>
      </el-col>
    </el-row>

    <el-dialog v-model="questionDialogVisible" title="新增题目" width="50%">
      <el-form :model="questionForm" :rules="questionRules" ref="questionFormRef" label-width="100px">
        <el-form-item label="题型" prop="type">
          <el-radio-group v-model="questionForm.type">
            <el-radio-button label="SINGLE">单选题</el-radio-button>
            <el-radio-button label="MULTIPLE">多选题</el-radio-button>
            <el-radio-button label="JUDGE">判断题</el-radio-button>
            <el-radio-button label="SUBJECTIVE">主观题</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="题干" prop="content">
          <el-input v-model="questionForm.content" type="textarea" rows="4" placeholder="请输入题干" />
        </el-form-item>
        <el-form-item v-if="questionForm.type === 'SINGLE'" label="单选配置">
          <div class="option-editor">
            <div v-for="opt in optionLabels" :key="`single-${opt}`" class="option-editor-row">
              <el-radio v-model="questionForm.answer" :label="opt">{{ opt }}</el-radio>
              <el-input v-model="questionForm[`option${opt}`]" :placeholder="`请输入选项 ${opt} 内容`" />
            </div>
          </div>
        </el-form-item>
        <el-form-item v-else-if="questionForm.type === 'MULTIPLE'" label="多选配置">
          <div class="option-editor">
            <div v-for="opt in optionLabels" :key="`multiple-${opt}`" class="option-editor-row">
              <el-checkbox v-model="questionForm.answerArray" :label="opt">{{ opt }}</el-checkbox>
              <el-input v-model="questionForm[`option${opt}`]" :placeholder="`请输入选项 ${opt} 内容`" />
            </div>
          </div>
        </el-form-item>
        <el-form-item v-else-if="questionForm.type === 'JUDGE'" label="判断题答案">
          <el-radio-group v-model="questionForm.answer">
            <el-radio label="T">正确 (True)</el-radio>
            <el-radio label="F">错误 (False)</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-else label="参考答案">
          <el-input v-model="questionForm.answer" type="textarea" rows="5" placeholder="请输入主观题参考答案" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="questionDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitQuestionForm" :loading="submittingQuestion">确认</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog v-model="answerPreviewVisible" title="主观题参考答案" width="640px">
      <div class="answer-preview-content">{{ answerPreviewText || '暂无答案' }}</div>
      <template #footer>
        <el-button @click="answerPreviewVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus';
import { getCourses } from '@/api/course';
import {
  getQuestionsByCourse,
  createQuestion,
  deleteQuestion,
  downloadTemplate,
  importQuestions
} from '@/api/question';

const courses = ref([]);
const selectedCourseId = ref(null);
const questions = ref([]);
const loadingQuestions = ref(false);

const activeCourseName = computed(() => {
  if (!selectedCourseId.value) return '';
  const course = courses.value.find(c => c.id === selectedCourseId.value);
  return course ? (course.courseName || '') : '';
});

onMounted(async () => {
  await loadCourses();
});

const loadCourses = async () => {
  try {
    const data = await getCourses();
    courses.value = Array.isArray(data) ? data : [];
    if (courses.value.length > 0 && !selectedCourseId.value) {
      selectedCourseId.value = courses.value[0].id;
      await loadQuestions(selectedCourseId.value);
    } else if (courses.value.length === 0) {
      selectedCourseId.value = null;
      questions.value = [];
    }
  } catch (error) {
    ElMessage.error(error.message || '加载课程失败');
  }
};

const handleCourseSelect = async (index) => {
  const courseId = Number(index);
  selectedCourseId.value = Number.isNaN(courseId) ? null : courseId;
  await loadQuestions(selectedCourseId.value);
};

const loadQuestions = async (courseId) => {
  if (!courseId) return;
  loadingQuestions.value = true;
  try {
    questions.value = await getQuestionsByCourse(courseId);
  } catch (error) {
    ElMessage.error(error.message || '加载题目失败');
    questions.value = [];
  } finally {
    loadingQuestions.value = false;
  }
};

const questionDialogVisible = ref(false);
const submittingQuestion = ref(false);
const questionFormRef = ref(null);
const answerPreviewVisible = ref(false);
const answerPreviewText = ref('');
const questionForm = ref({
  type: 'SINGLE',
  content: '',
  answer: '',
  answerArray: [],
  optionA: '',
  optionB: '',
  optionC: '',
  optionD: ''
});
const optionLabels = ['A', 'B', 'C', 'D'];
const questionRules = {
  type: [{ required: true, message: '请选择题型', trigger: 'change' }],
  content: [{ required: true, message: '请输入题干', trigger: 'blur' }]
};

const showAddQuestionDialog = () => {
  if (!selectedCourseId.value) {
    ElMessage.warning('请先选择一门课程');
    return;
  }
  questionForm.value = {
    type: 'SINGLE',
    content: '',
    answer: '',
    answerArray: [],
    optionA: '',
    optionB: '',
    optionC: '',
    optionD: ''
  };
  questionDialogVisible.value = true;
};

const rowTypeName = (type) => {
  const normalized = normalizeType(type);
  if (normalized === 'MULTIPLE') return '多选 (客观题)';
  if (normalized === 'JUDGE') return '判断题';
  if (normalized === 'SUBJECTIVE') return '主观题';
  return '单选 (客观题)';
};

const normalizeType = (type) => {
  if (!type) return 'SINGLE';
  const normalized = String(type).toUpperCase();
  if (normalized === 'MULTIPLE' || normalized === 'MULTIPLE_CHOICE') return 'MULTIPLE';
  if (normalized === 'JUDGE' || normalized === 'TRUE_FALSE') return 'JUDGE';
  if (normalized === 'SUBJECTIVE') return 'SUBJECTIVE';
  return 'SINGLE';
};

const answerPreview = (text) => {
  const raw = String(text || '').trim();
  if (!raw) return '暂无答案';
  if (raw.length <= 28) return raw;
  return `${raw.slice(0, 28)}...`;
};

const openAnswerPreview = (row) => {
  answerPreviewText.value = String(row?.answer || '');
  answerPreviewVisible.value = true;
};

const submitQuestionForm = async () => {
  if (!questionFormRef.value) return;
  await questionFormRef.value.validate(async (valid) => {
    if (valid) {
      submittingQuestion.value = true;
      try {
        const type = normalizeType(questionForm.value.type);
        const optionMap = optionLabels.map(opt => ({
          key: opt,
          text: String(questionForm.value[`option${opt}`] || '').trim()
        }));
        const options = (type === 'SINGLE' || type === 'MULTIPLE')
          ? optionMap.map(item => `${item.key}:${item.text}`).join(', ')
          : '';
        const answer = type === 'MULTIPLE'
          ? [...(questionForm.value.answerArray || [])].sort().join(',')
          : String(questionForm.value.answer || '').trim();
        if (type === 'SINGLE' || type === 'MULTIPLE') {
          if (optionMap.some(item => !item.text)) {
            ElMessage.warning('请完整填写 A-D 四个选项内容');
            submittingQuestion.value = false;
            return;
          }
        }
        if (!answer) {
          ElMessage.warning('请先选择正确答案');
          submittingQuestion.value = false;
          return;
        }
        const payload = {
          type,
          content: questionForm.value.content,
          options,
          answer,
          courseId: selectedCourseId.value
        };
        await createQuestion(payload);
        ElMessage.success('题目创建成功');
        questionDialogVisible.value = false;
        await loadQuestions(selectedCourseId.value);
      } catch (error) {
        ElMessage.error(error.message || '创建题目失败');
      } finally {
        submittingQuestion.value = false;
      }
    }
  });
};

const handleDeleteQuestion = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该题目吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    });
    await deleteQuestion(id);
    ElMessage.success('题目删除成功');
    await loadQuestions(selectedCourseId.value);
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除题目失败');
    }
  }
};

const handleDownloadTemplate = async () => {
  try {
    const blob = await downloadTemplate();
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'Question_Template.xlsx';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
    ElMessage.success('模板下载成功');
  } catch (error) {
    ElMessage.error(error.message || '下载模板失败');
  }
};

const handleImport = async (file) => {
  if (!selectedCourseId.value) {
    ElMessage.warning('请先选择要把题目导入到哪门课程');
    return false;
  }
  const loading = ElLoading.service({
    lock: true,
    text: '正在导入题目，请稍候...',
    background: 'rgba(255, 255, 255, 0.6)',
  });
  try {
    await importQuestions(selectedCourseId.value, file);
    ElMessage.success('导入成功');
    await loadQuestions(selectedCourseId.value);
  } catch (error) {
    ElMessage.error(error.message || '导入失败');
  } finally {
    loading.close();
  }
  return false;
};
</script>

<style scoped>
.question-management-container {
  padding: 20px;
  height: calc(100vh - 100px); /* 视具体项目布局调整 */
  background: #f5f5f7; /* Apple 浅灰背景 */
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Text", "Segoe UI", Roboto, "Helvetica Neue", Arial, "Noto Sans", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
}

.full-height {
  height: 100%;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #ebeef5;
}

.panel-header h3 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 8px;
}

.category-panel {
  background: #ffffffcc;
  border-radius: 14px;
  padding: 16px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.06);
  height: 100%;
  overflow-y: auto;
  backdrop-filter: saturate(180%) blur(10px);
}

.category-list {
  margin-top: 10px;
}

.category-item-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.category-name {
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #1d1d1f;
  font-weight: 500;
  letter-spacing: 0.2px;
}

.category-item-content .delete-btn {
  visibility: hidden;
}

.el-menu-item:hover .category-item-content .delete-btn {
  visibility: visible;
}

.apple-menu :deep(.el-menu-item) {
  height: 46px;
  line-height: 46px;
  border-radius: 10px;
  margin: 4px 6px;
  transition: background 0.25s ease, color 0.25s ease;
}

.apple-menu :deep(.el-menu-item.is-active) {
  background: #e8f0fe;
  color: #0a84ff;
}

.apple-menu :deep(.el-menu-item:hover) {
  background: #f2f2f7;
}

.empty-tip {
  color: #909399;
  text-align: center;
  padding: 30px 0;
  font-size: 14px;
}

.question-panel {
  background: #ffffffcc;
  border-radius: 14px;
  padding: 16px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.06);
  height: 100%;
  overflow-y: auto;
  backdrop-filter: saturate(180%) blur(10px);
}

.form-help {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
  line-height: 1.5;
}

.option-editor {
  width: 100%;
  display: grid;
  gap: 8px;
}

.option-editor-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.answer-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.answer-ellipsis {
  display: inline-block;
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #334155;
}

.answer-preview-content {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.8;
  color: #1f2937;
  max-height: 60vh;
  overflow: auto;
}

/* 覆盖el-menu-item的高度，确保按钮对齐 */
:deep(.el-menu-item) {
  height: 46px;
  line-height: 46px;
}
</style>
