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
            <template #default>
              <el-tag type="info" effect="plain">单选 (客观题)</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="options" label="选项" min-width="150" show-overflow-tooltip />
          <el-table-column prop="answer" label="正确答案" width="100" align="center" />
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
          <el-select v-model="questionForm.type" placeholder="请选择题型" style="width: 100%;">
            <el-option label="单选题" value="SINGLE_CHOICE" />
            <el-option label="多选题" value="MULTIPLE_CHOICE" />
            <el-option label="判断题" value="TRUE_FALSE" />
          </el-select>
        </el-form-item>
        <el-form-item label="题干" prop="content">
          <el-input v-model="questionForm.content" type="textarea" rows="4" placeholder="请输入题干" />
        </el-form-item>
        <el-form-item label="选项内容" prop="options" v-if="questionForm.type !== 'TRUE_FALSE'">
          <el-input v-model="questionForm.options" placeholder="例如：A:苹果, B:香蕉, C:橘子, D:葡萄" />
          <div class="form-help">请按格式输入选项（判断题无需输入选项）</div>
        </el-form-item>
        <el-form-item label="正确答案" prop="answer">
          <el-input v-model="questionForm.answer" placeholder="例如：A（单选）、A,B（多选）、T/F（判断）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="questionDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitQuestionForm" :loading="submittingQuestion">确认</el-button>
        </span>
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
const questionForm = ref({
  type: 'SINGLE_CHOICE',
  content: '',
  options: '',
  answer: ''
});
const questionRules = {
  type: [{ required: true, message: '请选择题型', trigger: 'change' }],
  content: [{ required: true, message: '请输入题干', trigger: 'blur' }],
  answer: [{ required: true, message: '请输入正确答案', trigger: 'blur' }]
};

const showAddQuestionDialog = () => {
  if (!selectedCourseId.value) {
    ElMessage.warning('请先选择一门课程');
    return;
  }
  questionForm.value = {
    type: 'SINGLE_CHOICE',
    content: '',
    options: '',
    answer: ''
  };
  questionDialogVisible.value = true;
};

const submitQuestionForm = async () => {
  if (!questionFormRef.value) return;
  await questionFormRef.value.validate(async (valid) => {
    if (valid) {
      submittingQuestion.value = true;
      try {
        const payload = {
          ...questionForm.value,
          courseId: selectedCourseId.value
        };
        if (payload.type === 'TRUE_FALSE') {
          payload.options = null;
        }
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

/* 覆盖el-menu-item的高度，确保按钮对齐 */
:deep(.el-menu-item) {
  height: 46px;
  line-height: 46px;
}
</style>
