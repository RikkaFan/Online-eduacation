<template>
  <div class="course-management">
    <div class="header-actions">
      <h2>课程管理</h2>
      <button class="btn btn-primary" @click="showAddForm = !showAddForm">
        {{ showAddForm ? '取消新增' : '新增课程' }}
      </button>
    </div>

    <!-- 新增课程表单 -->
    <div v-if="showAddForm" class="add-course-panel">
      <h3>新增课程</h3>
      <form @submit.prevent="handleAddCourse" class="course-form">
        <div class="form-group">
          <label for="courseName">课程名称：</label>
          <input 
            type="text" 
            id="courseName" 
            v-model="newCourse.courseName" 
            required
            placeholder="请输入课程名称"
          />
        </div>
        
        <div class="form-group">
          <label for="teacherId">任课教师ID：</label>
          <input 
            type="number" 
            id="teacherId" 
            v-model.number="newCourse.teacherId" 
            required
            placeholder="请输入教师ID"
          />
        </div>
        
        <div class="form-group">
          <label for="description">课程简介：</label>
          <textarea 
            id="description" 
            v-model="newCourse.description" 
            rows="3"
            placeholder="请输入课程简介"
          ></textarea>
        </div>
        
        <div class="form-actions">
          <button type="button" class="btn btn-secondary" @click="resetForm">取消</button>
          <button type="submit" class="btn btn-primary" :disabled="isSubmitting">
            {{ isSubmitting ? '提交中...' : '提交' }}
          </button>
        </div>
      </form>
    </div>

    <!-- 数据展示区 -->
    <div class="course-list">
      <div v-if="isLoading" class="loading-state">加载中...</div>
      
      <div v-else-if="courses.length === 0" class="empty-state">
        暂无课程数据
      </div>

      <table v-else class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>课程名称</th>
            <th>任课教师ID</th>
            <th>课程简介</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="course in courses" :key="course.id">
            <td>{{ course.id }}</td>
            <td>{{ course.courseName }}</td>
            <td>{{ course.teacherId }}</td>
            <td class="description-cell">{{ course.description || '-' }}</td>
            <td>
              <button 
                class="btn btn-danger btn-sm" 
                @click="handleDeleteCourse(course.id, course.courseName)"
              >
                删除
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
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
  teacherId: null,
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
  if (!newCourse.value.courseName || !newCourse.value.teacherId) {
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

.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.header-actions h2 {
  margin: 0;
  color: #333;
}

/* 按钮通用样式 */
.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-primary {
  background-color: #4CAF50;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background-color: #45a049;
}

.btn-secondary {
  background-color: #f1f1f1;
  color: #333;
  border: 1px solid #ccc;
}

.btn-secondary:hover {
  background-color: #e1e1e1;
}

.btn-danger {
  background-color: #f44336;
  color: white;
}

.btn-danger:hover {
  background-color: #da190b;
}

.btn-sm {
  padding: 4px 8px;
  font-size: 12px;
}

/* 表单面板样式 */
.add-course-panel {
  background-color: #f9f9f9;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 25px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
  border: 1px solid #eee;
}

.add-course-panel h3 {
  margin-top: 0;
  margin-bottom: 15px;
  font-size: 16px;
  color: #333;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  font-weight: bold;
  font-size: 14px;
  color: #555;
}

.form-group input,
.form-group textarea {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
}

.form-group input:focus,
.form-group textarea:focus {
  outline: none;
  border-color: #4CAF50;
  box-shadow: 0 0 5px rgba(76, 175, 80, 0.2);
}

.form-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 20px;
}

/* 表格样式 */
.data-table {
  width: 100%;
  border-collapse: collapse;
  background-color: white;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

.data-table th,
.data-table td {
  padding: 12px 15px;
  text-align: left;
  border-bottom: 1px solid #ddd;
}

.data-table th {
  background-color: #f8f9fa;
  font-weight: 600;
  color: #333;
}

.data-table tbody tr:hover {
  background-color: #f5f5f5;
}

.description-cell {
  max-width: 300px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.loading-state,
.empty-state {
  text-align: center;
  padding: 40px;
  color: #666;
  background-color: #f9f9f9;
  border-radius: 4px;
}
</style>
