<template>
  <div class="admin-user-management">
    <el-card class="apple-card" shadow="never">
      <div class="toolbar">
        <el-button type="primary" @click="openCreateDialog">新增用户</el-button>
        <el-input
          v-model="keyword"
          placeholder="按用户名搜索"
          clearable
          style="width: 260px;"
        />
      </div>

      <el-table :data="filteredUsers" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="180" />
        <el-table-column prop="department" label="所属部门/班级" min-width="200">
          <template #default="{ row }">{{ row.department || '暂无' }}</template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="220">
          <template #default="{ row }">{{ row.email || '-' }}</template>
        </el-table-column>
        <el-table-column label="角色" min-width="180">
          <template #default="{ row }">
            <el-tag
              v-for="role in row.roles || []"
              :key="role"
              :type="roleTagType(role)"
              class="role-tag"
            >
              {{ roleLabel(role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-popconfirm
              title="确认删除该用户？"
              confirm-button-text="删除"
              cancel-button-text="取消"
              @confirm="handleDelete(row)"
            >
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="isEdit" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" :prop="isEdit ? '' : 'password'">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            :placeholder="isEdit ? '留空则不修改密码' : '请输入密码'"
          />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="部门/班级" prop="department">
          <el-input v-model="form.department" placeholder="请输入所属部门或班级名称（如：计科2201班）" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-radio-group v-model="form.role">
            <el-radio label="ROLE_ADMIN">管理员</el-radio>
            <el-radio label="ROLE_TEACHER">教师</el-radio>
            <el-radio label="ROLE_STUDENT">学生</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { createUser, deleteUser, getUsers, updateUser } from '@/api/user';

const loading = ref(false);
const submitting = ref(false);
const keyword = ref('');
const users = ref([]);

const dialogVisible = ref(false);
const isEdit = ref(false);
const editingId = ref(null);
const formRef = ref(null);

const form = reactive({
  username: '',
  password: '',
  email: '',
  department: '',
  role: 'ROLE_STUDENT',
});

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
};

const filteredUsers = computed(() => {
  const key = keyword.value.trim().toLowerCase();
  if (!key) return users.value;
  return users.value.filter(u => String(u.username || '').toLowerCase().includes(key));
});

function roleLabel(role) {
  if (role === 'ROLE_ADMIN') return '管理员';
  if (role === 'ROLE_TEACHER') return '教师';
  if (role === 'ROLE_STUDENT') return '学生';
  return role;
}

function roleTagType(role) {
  if (role === 'ROLE_ADMIN') return 'danger';
  if (role === 'ROLE_TEACHER') return 'primary';
  return 'success';
}

async function loadUsers() {
  loading.value = true;
  try {
    const data = await getUsers();
    users.value = Array.isArray(data) ? data : [];
  } catch (e) {
    ElMessage.error(e.message || '加载用户失败');
  } finally {
    loading.value = false;
  }
}

function openCreateDialog() {
  isEdit.value = false;
  editingId.value = null;
  form.username = '';
  form.password = '';
  form.email = '';
  form.department = '';
  form.role = 'ROLE_STUDENT';
  dialogVisible.value = true;
}

function openEditDialog(row) {
  isEdit.value = true;
  editingId.value = row.id;
  form.username = row.username || '';
  form.password = '';
  form.email = row.email || '';
  form.department = row.department || '';
  form.role = (row.roles && row.roles[0]) || 'ROLE_STUDENT';
  dialogVisible.value = true;
}

async function submitForm() {
  if (!formRef.value) return;
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;

  submitting.value = true;
  try {
    if (isEdit.value) {
      const payload = {
        email: form.email?.trim() || '',
        department: form.department?.trim() || '',
        role: form.role,
      };
      if (form.password && form.password.trim()) payload.password = form.password.trim();
      await updateUser(editingId.value, payload);
      ElMessage.success('用户更新成功');
    } else {
      await createUser({
        username: form.username.trim(),
        password: form.password.trim(),
        email: form.email?.trim() || '',
        department: form.department?.trim() || '',
        role: form.role,
      });
      ElMessage.success('用户创建成功');
    }
    dialogVisible.value = false;
    await loadUsers();
  } catch (e) {
    ElMessage.error(e.message || '保存失败');
  } finally {
    submitting.value = false;
  }
}

async function handleDelete(row) {
  try {
    await deleteUser(row.id);
    ElMessage.success('删除成功');
    await loadUsers();
  } catch (e) {
    ElMessage.error(e.message || '删除失败');
  }
}

onMounted(loadUsers);
</script>

<style scoped>
.admin-user-management {
  padding: 0;
}

.apple-card {
  border: none;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.06);
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.role-tag {
  margin-right: 6px;
}
</style>
