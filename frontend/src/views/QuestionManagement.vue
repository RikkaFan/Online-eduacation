<template>
  <div class="question-management-container">
    <el-row :gutter="20" class="full-height">
      <!-- 左侧：分类栏 -->
      <el-col :span="6" class="category-panel">
        <div class="panel-header">
          <h3>题库分类</h3>
          <el-button type="primary" size="small" @click="showAddCategoryDialog">新增分类</el-button>
        </div>
        <div class="category-list">
          <el-menu :default-active="activeCategoryId?.toString()" @select="handleCategorySelect" class="el-menu-vertical apple-menu">
            <el-menu-item 
              v-for="category in categories" 
              :key="category.id" 
              :index="category.id.toString()"
            >
              <div class="category-item-content">
                <span class="category-name" :title="category.categoryName || category.name">{{ category.categoryName || category.name }}</span>
                <el-button 
                  type="danger" 
                  icon="Delete" 
                  circle 
                  size="small" 
                  @click.stop="handleDeleteCategory(category.id)"
                  class="delete-btn"
                />
              </div>
            </el-menu-item>
          </el-menu>
          <div v-if="categories.length === 0" class="empty-tip">
            暂无分类，请先添加。
          </div>
        </div>
      </el-col>

      <!-- 右侧：题目展示区 -->
      <el-col :span="18" class="question-panel">
        <div class="panel-header">
          <h3>题目列表 <span v-if="activeCategoryName">- {{ activeCategoryName }}</span></h3>
          <div class="action-buttons">
            <el-button size="small" @click="handleDownloadTemplate">下载导入模板</el-button>
            <el-upload
              accept=".xlsx,.xls"
              :show-file-list="false"
              :before-upload="handleImport"
            >
              <el-button type="success" size="small">批量导入题目</el-button>
            </el-upload>
            <el-button 
              type="primary" 
              size="small" 
              :disabled="!activeCategoryId" 
              @click="showAddQuestionDialog"
            >
              新增题目
            </el-button>
          </div>
        </div>
        
        <el-table :data="questions" v-loading="loadingQuestions" style="width: 100%" border stripe>
          <el-table-column prop="id" label="ID" width="60" align="center" />
          <el-table-column prop="content" label="题干" min-width="250" show-overflow-tooltip />
          <el-table-column prop="type" label="题型" width="100">
            <template #default="scope">
              <el-tag :type="getTypeTag(scope.row.type)">{{ getTypeName(scope.row.type) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="options" label="选项" min-width="150" show-overflow-tooltip />
          <el-table-column prop="answer" label="正确答案" width="100" align="center" />
          <el-table-column label="操作" width="100" align="center">
            <template #default="scope">
              <el-button link type="danger" @click="handleDeleteQuestion(scope.row.id)">删除</el-button>
            </template>
          </el-table-column>
          <template #empty>
            <div v-if="!activeCategoryId">请先在左侧选择一个分类</div>
            <div v-else>该分类下暂无题目</div>
          </template>
        </el-table>
      </el-col>
    </el-row>

    <!-- 新增分类弹窗 -->
    <el-dialog v-model="categoryDialogVisible" title="新增分类" width="30%">
      <el-form :model="categoryForm" :rules="categoryRules" ref="categoryFormRef" @submit.prevent>
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="categoryForm.name" placeholder="请输入分类名称" @keyup.enter="submitCategoryForm" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="categoryDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitCategoryForm" :loading="submittingCategory">
            确认
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 新增题目弹窗 -->
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
          <el-button type="primary" @click="submitQuestionForm" :loading="submittingQuestion">
            确认
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus';
import { Delete } from '@element-plus/icons-vue';
import { 
  getCategories, 
  createCategory, 
  deleteCategory, 
  getQuestionsByCategory, 
  createQuestion, 
  deleteQuestion,
  downloadTemplate,
  importQuestions
} from '@/api/question';

// ================= 状态数据 =================
const categories = ref([]);
const activeCategoryId = ref(null);
const questions = ref([]);
const loadingQuestions = ref(false);

const activeCategoryName = computed(() => {
  if (!activeCategoryId.value) return '';
  const category = categories.value.find(c => c.id === activeCategoryId.value);
  return category ? (category.categoryName || category.name || '') : '';
});

// ================= 生命周期 =================
onMounted(async () => {
  await loadCategories();
});

// ================= 分类管理逻辑 =================
const loadCategories = async () => {
  try {
    categories.value = await getCategories();
    // 默认选中第一个分类并加载其题目
    if (categories.value.length > 0 && !activeCategoryId.value) {
      activeCategoryId.value = categories.value[0].id;
      await loadQuestions(activeCategoryId.value);
    } else if (categories.value.length === 0) {
      activeCategoryId.value = null;
      questions.value = [];
    }
  } catch (error) {
    ElMessage.error(error.message || '加载分类失败');
  }
};

const handleCategorySelect = async (index) => {
  const categoryId = parseInt(index, 10);
  activeCategoryId.value = categoryId;
  await loadQuestions(categoryId);
};

// ================= 题目管理逻辑 =================
const loadQuestions = async (categoryId) => {
  if (!categoryId) return;
  loadingQuestions.value = true;
  try {
    questions.value = await getQuestionsByCategory(categoryId);
  } catch (error) {
    ElMessage.error(error.message || '加载题目失败');
    questions.value = [];
  } finally {
    loadingQuestions.value = false;
  }
};

const getTypeName = (type) => {
  const map = {
    'SINGLE_CHOICE': '单选题',
    'MULTIPLE_CHOICE': '多选题',
    'TRUE_FALSE': '判断题'
  };
  return map[type] || type;
};

const getTypeTag = (type) => {
  const map = {
    'SINGLE_CHOICE': '',
    'MULTIPLE_CHOICE': 'success',
    'TRUE_FALSE': 'warning'
  };
  return map[type] || 'info';
};

// ================= 弹窗与表单：新增分类 =================
const categoryDialogVisible = ref(false);
const submittingCategory = ref(false);
const categoryFormRef = ref(null);
const categoryForm = ref({ name: '' });
const categoryRules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
};

const showAddCategoryDialog = () => {
  categoryForm.value = { name: '' };
  categoryDialogVisible.value = true;
};

const submitCategoryForm = async () => {
  if (!categoryFormRef.value) return;
  await categoryFormRef.value.validate(async (valid) => {
    if (valid) {
      submittingCategory.value = true;
      try {
        await createCategory(categoryForm.value);
        ElMessage.success('分类创建成功');
        categoryDialogVisible.value = false;
        await loadCategories(); // 重新加载分类列表
      } catch (error) {
        ElMessage.error(error.message || '创建分类失败');
      } finally {
        submittingCategory.value = false;
      }
    }
  });
};

const handleDeleteCategory = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该分类吗？关联的题目也会一并受到影响。', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    });
    
    await deleteCategory(id);
    ElMessage.success('分类删除成功');
    
    // 如果删除的是当前选中的分类，重置选中状态
    if (activeCategoryId.value === id) {
      activeCategoryId.value = null;
      questions.value = [];
    }
    await loadCategories();
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除分类失败');
    }
  }
};

// ================= 弹窗与表单：新增题目 =================
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
  if (!activeCategoryId.value) {
    ElMessage.warning('请先选择一个分类');
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
          categoryId: activeCategoryId.value
        };
        // 如果是判断题，不需要 options 字段
        if (payload.type === 'TRUE_FALSE') {
          payload.options = null;
        }
        
        await createQuestion(payload);
        ElMessage.success('题目创建成功');
        questionDialogVisible.value = false;
        await loadQuestions(activeCategoryId.value); // 重新加载当前分类的题目列表
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
    await loadQuestions(activeCategoryId.value);
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
  if (!activeCategoryId.value) {
    ElMessage.warning('请先选择要把题目导入到哪门课程');
    return false;
  }
  const loading = ElLoading.service({
    lock: true,
    text: '正在导入题目，请稍候...',
    background: 'rgba(255, 255, 255, 0.6)',
  });
  try {
    await importQuestions(activeCategoryId.value, file);
    ElMessage.success('导入成功');
    await loadQuestions(activeCategoryId.value);
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
