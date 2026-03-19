<template>
  <div class="course-management">
    <el-card class="page-card glass-card" shadow="never">
      <div class="action-bar">
        <h2>课程管理</h2>
        <div class="actions-right">
          <el-button type="primary" @click="showAddForm = !showAddForm">
            {{ showAddForm ? '取消新增' : '新增课程' }}
          </el-button>
        </div>
      </div>

      <el-card v-if="showAddForm" class="inner-card glass-card" shadow="never">
        <el-form @submit.prevent="handleAddCourse" :model="newCourse" label-width="110px">
          <el-form-item label="课程名称">
            <el-input v-model="newCourse.courseName" placeholder="请输入课程名称" />
          </el-form-item>
          <el-form-item label="课程简介">
            <el-input v-model="newCourse.description" type="textarea" rows="3" placeholder="请输入课程简介" />
          </el-form-item>
          <el-form-item>
            <el-button @click="resetForm">取消</el-button>
            <el-button type="primary" native-type="submit" :loading="isSubmitting">提交</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-table v-loading="isLoading" :data="courses" stripe style="width: 100%">
        <el-table-column type="index" label="#" width="80" />
        <el-table-column prop="courseName" label="课程名称" min-width="200" />
        <el-table-column prop="teacherId" label="任课教师ID" width="160" />
        <el-table-column label="课程简介" min-width="240">
          <template #default="{ row }">{{ row.description || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openChapterDrawer(row)">章节管理</el-button>
            <el-button link type="danger" @click="handleDeleteCourse(row.id, row.courseName)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-state">暂无课程数据</div>
        </template>
      </el-table>
    </el-card>

    <el-drawer
      v-model="chapterDrawerVisible"
      title="课程内容管理"
      direction="rtl"
      size="52%"
      :destroy-on-close="false"
      class="chapter-drawer"
    >
      <div class="drawer-head">
        <div class="drawer-title">{{ currentCourse?.courseName || '未选择课程' }}</div>
      </div>

      <el-card class="glass-card drawer-card" shadow="never">
        <el-form :model="chapterForm" label-width="90px">
          <el-form-item label="课时标题">
            <el-input v-model="chapterForm.title" placeholder="请输入课时标题" />
          </el-form-item>
          <el-form-item label="图文内容">
            <el-input v-model="chapterForm.content" type="textarea" :rows="4" placeholder="支持图文描述或外链地址" />
          </el-form-item>
          <el-form-item label="排序号">
            <el-input-number v-model="chapterForm.sortOrder" :min="1" :max="9999" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="chapterSubmitting" @click="handleAddChapter">发布课时</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card class="glass-card drawer-card" shadow="never">
        <template #header>
          <div class="list-header">已发布课时</div>
        </template>
        <div v-if="chapterLoading" class="empty-state">正在加载课时...</div>
        <div v-else-if="chapters.length === 0" class="empty-state">当前课程暂无课时</div>
        <div v-else class="chapter-list">
          <div v-for="item in chapters" :key="item.id" class="chapter-item">
            <div class="chapter-main">
              <div class="chapter-item-title">{{ item.sortOrder || '-' }}. {{ item.title || '未命名课时' }}</div>
              <div class="chapter-item-content">{{ item.content || '-' }}</div>
            </div>
            <el-button link type="danger" @click="handleDeleteChapter(item)">删除</el-button>
          </div>
        </div>
      </el-card>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getCourses, createCourse, deleteCourse } from '@/api/course';
import { getChapters, addChapter, deleteChapter } from '@/api/chapter';

const courses = ref([]);
const isLoading = ref(true);
const showAddForm = ref(false);
const isSubmitting = ref(false);
const chapterDrawerVisible = ref(false);
const chapterLoading = ref(false);
const chapterSubmitting = ref(false);
const currentCourse = ref(null);
const chapters = ref([]);

const chapterForm = ref({
  title: '',
  content: '',
  sortOrder: 1,
});

const newCourse = ref({
  courseName: '',
  description: ''
});

const fetchCourses = async () => {
  isLoading.value = true;
  try {
    const data = await getCourses();
    courses.value = Array.isArray(data) ? data : [];
  } catch (error) {
    ElMessage.error(error.message || '获取课程列表失败，请重试');
  } finally {
    isLoading.value = false;
  }
};

const resetForm = () => {
  newCourse.value = {
    courseName: '',
    description: ''
  };
  showAddForm.value = false;
};

const handleAddCourse = async () => {
  if (!newCourse.value.courseName) {
    ElMessage.warning('请填写完整必填信息');
    return;
  }

  isSubmitting.value = true;
  try {
    await createCourse(newCourse.value);
    ElMessage.success('创建课程成功');
    resetForm();
    await fetchCourses();
  } catch (error) {
    ElMessage.error(error.message || '创建课程失败，请重试');
  } finally {
    isSubmitting.value = false;
  }
};

const handleDeleteCourse = async (id, courseName) => {
  try {
    await ElMessageBox.confirm(`确定要删除课程“${courseName}”吗？此操作不可恢复！`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    });
  } catch {
    return;
  }
  try {
    await deleteCourse(id);
    ElMessage.success('删除课程成功');
    await fetchCourses();
  } catch (error) {
    ElMessage.error(error.message || '删除课程失败，请重试');
  }
};

function resetChapterForm() {
  chapterForm.value = {
    title: '',
    content: '',
    sortOrder: 1,
  };
}

async function loadChapters(courseId) {
  chapterLoading.value = true;
  try {
    const data = await getChapters(courseId);
    chapters.value = Array.isArray(data) ? data : [];
  } catch (e) {
    chapters.value = [];
    ElMessage.error(e.message || '加载课时失败');
  } finally {
    chapterLoading.value = false;
  }
}

async function openChapterDrawer(course) {
  currentCourse.value = course;
  chapterDrawerVisible.value = true;
  resetChapterForm();
  await loadChapters(course.id);
}

async function handleAddChapter() {
  if (!currentCourse.value?.id) return;
  if (!chapterForm.value.title?.trim()) {
    ElMessage.warning('请输入课时标题');
    return;
  }
  chapterSubmitting.value = true;
  try {
    await addChapter(currentCourse.value.id, {
      title: chapterForm.value.title.trim(),
      content: chapterForm.value.content?.trim() || '',
      sortOrder: Number(chapterForm.value.sortOrder || 1),
    });
    ElMessage.success('课时发布成功');
    resetChapterForm();
    await loadChapters(currentCourse.value.id);
  } catch (e) {
    ElMessage.error(e.message || '课时发布失败');
  } finally {
    chapterSubmitting.value = false;
  }
}

async function handleDeleteChapter(chapter) {
  try {
    await ElMessageBox.confirm(`确认删除课时「${chapter.title || '未命名课时'}」吗？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    });
  } catch {
    return;
  }
  try {
    await deleteChapter(chapter.id);
    ElMessage.success('课时删除成功');
    if (currentCourse.value?.id) {
      await loadChapters(currentCourse.value.id);
    }
  } catch (e) {
    ElMessage.error(e.message || '课时删除失败');
  }
};

onMounted(() => {
  fetchCourses();
});
</script>

<style scoped>
.course-management {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}
.page-card {
  padding: 16px;
}
.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.action-bar h2 { margin: 0; }
.inner-card { margin-bottom: 16px; }
.empty-state {
  text-align: center;
  padding: 24px;
  color: #64748B;
}
.chapter-drawer :deep(.el-drawer__body) {
  background: rgba(248, 250, 252, 0.7);
}
.drawer-head {
  margin-bottom: 12px;
}
.drawer-title {
  font-size: 16px;
  color: #1E293B;
  font-weight: 600;
}
.drawer-card {
  margin-bottom: 14px;
}
.list-header {
  color: #1E293B;
  font-weight: 600;
}
.chapter-list {
  display: grid;
  gap: 10px;
}
.chapter-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(226, 232, 240, 0.8);
}
.chapter-main {
  flex: 1;
  min-width: 0;
}
.chapter-item-title {
  color: #1E293B;
  font-weight: 600;
}
.chapter-item-content {
  margin-top: 6px;
  color: #64748B;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
