<template>
  <div class="course-management">
    <el-card class="page-card" shadow="never">
      <div class="action-bar">
        <h2>课程管理</h2>
        <div class="actions-right">
          <el-button type="primary" @click="showAddForm = !showAddForm">
            {{ showAddForm ? '取消新增' : '新增课程' }}
          </el-button>
        </div>
      </div>

      <el-card v-if="showAddForm" class="inner-card" shadow="never">
        <el-form @submit.prevent="handleAddCourse" :model="newCourse" label-width="110px">
          <el-form-item label="课程名称">
            <el-input v-model="newCourse.courseName" placeholder="请输入课程名称" />
          </el-form-item>
          <!-- 教师ID由后端根据当前登录用户自动注入，前端不再填写 -->
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
        <el-table-column prop="id" label="ID" width="100" />
        <el-table-column prop="courseName" label="课程名称" min-width="200" />
        <el-table-column prop="teacherId" label="任课教师ID" width="160" />
        <el-table-column label="课程简介" min-width="240">
          <template #default="{ row }">{{ row.description || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button link type="danger" @click="handleDeleteCourse(row.id, row.courseName)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-state">暂无课程数据</div>
        </template>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { getCourses, createCourse, deleteCourse } from '../api/course';

const courses = ref([]);
const isLoading = ref(true);
const showAddForm = ref(false);
const isSubmitting = ref(false);

const newCourse = ref({
  courseName: '',
  description: ''
});

// 加载课程列表
const fetchCourses = async () => {
  isLoading.value = true;
  try {
    const data = await getCourses();
    courses.value = data;
  } catch (error) {
    console.error('获取课程列表失败:', error);
    alert(error.message || '获取课程列表失败，请重试');
  } finally {
    isLoading.value = false;
  }
};

// 重置表单
const resetForm = () => {
  newCourse.value = {
    courseName: '',
    teacherId: null,
    description: ''
  };
  showAddForm.value = false;
};

// 处理新增课程
const handleAddCourse = async () => {
  if (!newCourse.value.courseName) {
    alert('请填写完整必填信息');
    return;
  }

  isSubmitting.value = true;
  try {
    await createCourse(newCourse.value);
    alert('创建课程成功');
    resetForm();
    await fetchCourses(); // 重新加载列表
  } catch (error) {
    console.error('创建课程失败:', error);
    alert(error.message || '创建课程失败，请重试');
  } finally {
    isSubmitting.value = false;
  }
};

// 处理删除课程
const handleDeleteCourse = async (id, courseName) => {
  if (!confirm(`确定要删除课程“${courseName}”吗？此操作不可恢复！`)) {
    return;
  }

  try {
    await deleteCourse(id);
    alert('删除课程成功');
    await fetchCourses(); // 重新加载列表
  } catch (error) {
    console.error('删除课程失败:', error);
    alert(error.message || '删除课程失败，请重试');
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
  color: #666;
}
</style>
