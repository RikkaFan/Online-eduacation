<template>
  <div class="question-management-container">
    <h2 class="page-title">题库管理中心</h2>

    <div class="glass-card toolbar-card">
      <div class="toolbar-left">
        <el-select v-model="searchCourse" clearable placeholder="选择课程筛选" style="width: 220px;">
          <el-option v-for="course in courses" :key="course.id" :label="course.courseName || `课程#${course.id}`" :value="course.id" />
        </el-select>
        <el-input v-model="searchKeyword" clearable placeholder="搜索题干关键字" style="width: 320px;" />
      </div>
      <div class="toolbar-right">
        <el-button type="primary" round @click="handleDownloadTemplate">下载导入模板</el-button>
        <el-button type="primary" plain round @click="openImportDialog">导入题库</el-button>
        <el-button type="primary" round :disabled="!searchCourse" @click="showAddQuestionDialog">新增题目</el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="question-tabs">
      <el-tab-pane label="全部题目" name="all" />
      <el-tab-pane label="单选题" name="SINGLE" />
      <el-tab-pane label="多选题" name="MULTIPLE" />
      <el-tab-pane label="判断题" name="JUDGE" />
      <el-tab-pane label="主观题" name="SUBJECTIVE" />
    </el-tabs>

    <div class="glass-card table-card">
      <div class="table-header">
        <h3>题目列表</h3>
        <span class="table-sub">{{ filteredQuestions.length }} 道题</span>
      </div>
      <el-table :data="filteredQuestions" v-loading="loadingQuestions" style="width: 100%" border stripe>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="content" label="题干" min-width="380" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="content-cell">
              <span class="content-text">{{ row.content || '-' }}</span>
              <span v-if="row.imageUrl" class="image-flag">🖼️</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="题型" width="130" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTagType(row.type)" effect="plain">{{ rowTypeName(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="标准答案" min-width="260">
          <template #default="{ row }">
            <template v-if="normalizeType(row.type) === 'SUBJECTIVE'">
              <div class="answer-cell">
                <el-tooltip effect="dark" placement="top" :content="row.answer || '暂无答案'" raw-content>
                  <span class="answer-ellipsis answer-highlight">{{ answerPreview(row.answer) }}</span>
                </el-tooltip>
                <el-button link type="primary" size="small" @click="openAnswerPreview(row)">查看</el-button>
              </div>
            </template>
            <template v-else>
              <span class="answer-highlight">{{ row.answer || '-' }}</span>
            </template>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button link type="danger" @click="handleDeleteQuestion(row.id)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div>暂无匹配题目</div>
        </template>
      </el-table>
    </div>

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

    <el-dialog v-model="importDialogVisible" title="导入题库" width="760px">
      <div class="quick-import-wrap">
        <el-form label-width="92px">
          <el-form-item label="导入课程">
            <el-select v-model="importCourseId" placeholder="请选择课程" style="width: 100%;">
              <el-option v-for="course in courses" :key="course.id" :label="course.courseName || `课程#${course.id}`" :value="course.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="导入方式">
            <el-radio-group v-model="importMode">
              <el-radio-button label="excel">Excel批量导入</el-radio-button>
              <el-radio-button label="quick">Word/文本一键导入</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="importMode === 'excel'" label="上传文件">
            <el-upload
              accept=".xlsx,.xls"
              :show-file-list="false"
              :before-upload="handleExcelImportFile"
            >
              <el-button plain>选择 Excel</el-button>
            </el-upload>
            <span class="quick-file-name">{{ excelImportFileName || '未选择 Excel 文件' }}</span>
          </el-form-item>
          <el-form-item v-else label="上传文件">
            <el-upload
              accept=".docx,.txt"
              :show-file-list="false"
              :before-upload="handleQuickImportFile"
            >
              <el-button plain>选择 DOCX/TXT</el-button>
            </el-upload>
            <span class="quick-file-name">{{ quickImportFileName || '未选择文件（可仅粘贴文本）' }}</span>
          </el-form-item>
          <el-form-item v-if="importMode === 'quick'" label="粘贴内容">
            <el-input
              v-model="quickImportText"
              type="textarea"
              :rows="10"
              placeholder="可直接粘贴 Word 题库文本，如：1.题干...A....B....【答案】B。解析：..."
            />
          </el-form-item>
        </el-form>
        <div v-if="importResult" class="quick-result">
          <div class="quick-result-title">{{ importResult.message || '导入结果' }}</div>
          <div class="quick-result-meta">
            成功 {{ importResult.successCount || 0 }} 道，失败 {{ importResult.failedCount || 0 }} 道
          </div>
          <div v-if="importErrors.length" class="quick-result-errors">
            <div v-for="(item, idx) in importErrors" :key="`err-${idx}`">- {{ item }}</div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="importDialogVisible = false">关闭</el-button>
        <el-button :loading="importLoading" type="primary" @click="submitImport">开始导入</el-button>
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
  importQuestions,
  quickImportQuestions
} from '@/api/question';

const courses = ref([]);
const selectedCourseId = ref(null);
const searchCourse = ref(null);
const searchKeyword = ref('');
const activeTab = ref('all');
const questions = ref([]);
const loadingQuestions = ref(false);

const filteredQuestions = computed(() => {
  let result = [...questions.value];
  if (searchCourse.value) {
    result = result.filter(q => Number(q.courseId) === Number(searchCourse.value));
  }
  if (searchKeyword.value) {
    const keyword = String(searchKeyword.value).trim();
    result = result.filter(q => q.content && q.content.includes(keyword));
  }
  if (activeTab.value !== 'all') {
    result = result.filter(q => normalizeType(q.type) === activeTab.value);
  }
  return result.sort((a, b) => Number(b.id || 0) - Number(a.id || 0));
});

onMounted(async () => {
  await loadCourses();
});

const loadCourses = async () => {
  try {
    const data = await getCourses();
    courses.value = Array.isArray(data) ? data : [];
    if (courses.value.length > 0) {
      searchCourse.value = courses.value[0].id;
      selectedCourseId.value = courses.value[0].id;
      await loadQuestions();
      return;
    }
    searchCourse.value = null;
    selectedCourseId.value = null;
    questions.value = [];
  } catch (error) {
    ElMessage.error(error.message || '加载课程失败');
  }
};

const loadQuestions = async () => {
  loadingQuestions.value = true;
  try {
    const all = await Promise.all(courses.value.map(async (course) => {
      const list = await getQuestionsByCourse(course.id);
      return (Array.isArray(list) ? list : []).map(q => ({
        ...q,
        courseId: q.courseId ?? course.id
      }));
    }));
    questions.value = all.flat();
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
const importDialogVisible = ref(false);
const importLoading = ref(false);
const importMode = ref('excel');
const excelImportFile = ref(null);
const excelImportFileName = ref('');
const quickImportText = ref('');
const quickImportFile = ref(null);
const quickImportFileName = ref('');
const importResult = ref(null);
const importErrors = computed(() => {
  const list = importResult.value?.errors;
  return Array.isArray(list) ? list.slice(0, 10) : [];
});
const importCourseId = ref(null);
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
  selectedCourseId.value = searchCourse.value;
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

const typeTagType = (type) => {
  const normalized = normalizeType(type);
  if (normalized === 'MULTIPLE') return 'warning';
  if (normalized === 'JUDGE') return 'success';
  if (normalized === 'SUBJECTIVE') return 'info';
  return '';
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
        await loadQuestions();
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
    await loadQuestions();
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

const openImportDialog = () => {
  importDialogVisible.value = true;
  importMode.value = 'excel';
  importCourseId.value = null;
  excelImportFile.value = null;
  excelImportFileName.value = '';
  quickImportText.value = '';
  quickImportFile.value = null;
  quickImportFileName.value = '';
  importResult.value = null;
};

const handleExcelImportFile = (file) => {
  const name = String(file?.name || '').toLowerCase();
  if (!name.endsWith('.xlsx') && !name.endsWith('.xls')) {
    ElMessage.warning('仅支持 .xlsx 或 .xls 文件');
    return false;
  }
  excelImportFile.value = file;
  excelImportFileName.value = file.name;
  return false;
};

const handleQuickImportFile = (file) => {
  const name = String(file?.name || '').toLowerCase();
  if (!name.endsWith('.docx') && !name.endsWith('.txt')) {
    ElMessage.warning('仅支持 .docx 或 .txt 文件');
    return false;
  }
  quickImportFile.value = file;
  quickImportFileName.value = file.name;
  return false;
};

const submitImport = async () => {
  if (!importCourseId.value) {
    ElMessage.warning('请先选择导入课程');
    return;
  }
  const course = courses.value.find(item => Number(item?.id) === Number(importCourseId.value));
  const courseName = course?.courseName || `课程#${importCourseId.value}`;
  try {
    await ElMessageBox.confirm(`将把题目导入到《${courseName}》，是否继续？`, '确认导入课程', {
      confirmButtonText: '继续导入',
      cancelButtonText: '取消',
      type: 'warning',
    });
  } catch {
    return;
  }
  importLoading.value = true;
  try {
    let result = null;
    if (importMode.value === 'excel') {
      if (!excelImportFile.value) {
        ElMessage.warning('请先选择 Excel 文件');
        return;
      }
      const loading = ElLoading.service({
        lock: true,
        text: '正在导入题目，请稍候...',
        background: 'rgba(255, 255, 255, 0.6)',
      });
      try {
        const raw = await importQuestions(importCourseId.value, excelImportFile.value);
        result = {
          message: raw?.message || '导入完成',
          successCount: Number(raw?.count || 0),
          failedCount: 0,
          errors: []
        };
      } finally {
        loading.close();
      }
    } else {
      if (!quickImportFile.value && !String(quickImportText.value || '').trim()) {
        ElMessage.warning('请上传文件或粘贴题库文本');
        return;
      }
      result = await quickImportQuestions(importCourseId.value, {
        text: quickImportText.value,
        file: quickImportFile.value
      });
    }
    importResult.value = result || {};
    if ((result?.successCount || 0) > 0) {
      ElMessage.success(`导入成功 ${result.successCount} 道题`);
      searchCourse.value = Number(importCourseId.value);
      await loadQuestions();
      if (importMode.value === 'excel') {
        excelImportFile.value = null;
        excelImportFileName.value = '';
      }
    } else {
      ElMessage.warning(result?.message || '未导入任何题目');
    }
  } catch (error) {
    ElMessage.error(error.message || '导入失败');
  } finally {
    importLoading.value = false;
  }
};
</script>

<style scoped>
.question-management-container {
  padding: 8px 4px 0;
  min-height: auto;
  background: transparent;
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Text", "Segoe UI", Roboto, "Helvetica Neue", Arial, "Noto Sans", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-title {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  color: #1d1d1f;
}
.glass-card {
  background: rgba(255, 255, 255, 0.84);
  border: 1px solid rgba(212, 224, 244, 0.95);
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06), 0 2px 6px rgba(15, 23, 42, 0.03);
  backdrop-filter: blur(16px) saturate(180%);
  border-radius: 20px;
}
.toolbar-card {
  padding: 18px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}
.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.question-tabs {
  padding: 0 8px;
}
.table-card {
  padding: 18px;
}
.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.table-header h3 {
  margin: 0;
  font-size: 18px;
  color: #1d1d1f;
}
.table-sub {
  color: #64748b;
  font-size: 13px;
}
.content-cell {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}
.content-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.image-flag {
  font-size: 13px;
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
}
.answer-highlight {
  color: #059669;
  font-weight: 600;
  background: #ecfdf5;
  padding: 2px 8px;
  border-radius: 4px;
}

.answer-preview-content {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.8;
  color: #1f2937;
  max-height: 60vh;
  overflow: auto;
}
.quick-import-wrap {
  display: grid;
  gap: 12px;
}
.quick-file-name {
  margin-left: 10px;
  color: #64748b;
  font-size: 13px;
}
.quick-result {
  border: 1px dashed rgba(148, 163, 184, 0.45);
  border-radius: 12px;
  padding: 12px;
  background: rgba(248, 252, 255, 0.86);
}
.quick-result-title {
  font-weight: 600;
  color: #0f172a;
}
.quick-result-meta {
  margin-top: 6px;
  color: #334155;
}
.quick-result-errors {
  margin-top: 8px;
  max-height: 160px;
  overflow: auto;
  color: #b91c1c;
  font-size: 13px;
  line-height: 1.6;
}
@media (max-width: 1100px) {
  .toolbar-left .el-select,
  .toolbar-left .el-input {
    width: 100% !important;
  }
}
</style>
