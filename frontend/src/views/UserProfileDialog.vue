<template>
  <el-dialog v-model="visibleProxy" title="个人中心" width="560px">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="基本信息" name="info">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="用户名">{{ profile.username || '-' }}</el-descriptions-item>
          <el-descriptions-item label="角色">{{ roleText }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ profile.email || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-tab-pane>
      <el-tab-pane label="修改密码" name="password">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
          <el-form-item label="旧密码" prop="oldPassword">
            <el-input v-model="form.oldPassword" type="password" show-password placeholder="请输入旧密码" />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="form.newPassword" type="password" show-password placeholder="请输入新密码" />
          </el-form-item>
          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input v-model="form.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
          </el-form-item>
        </el-form>
      </el-tab-pane>
    </el-tabs>
    <template #footer>
      <el-button @click="visibleProxy = false">关闭</el-button>
      <el-button
        v-if="activeTab === 'password'"
        type="primary"
        :loading="submitting"
        @click="submitChangePassword"
      >
        修改密码
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue';
import { storeToRefs } from 'pinia';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useAuthStore } from '@/store/auth';
import { changePassword } from '@/api/user';

const props = defineProps({
  modelValue: { type: Boolean, default: false },
});
const emit = defineEmits(['update:modelValue']);

const authStore = useAuthStore();
const { user, roles } = storeToRefs(authStore);

const visibleProxy = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
});

const activeTab = ref('info');
const submitting = ref(false);
const formRef = ref(null);
const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
});

const profile = computed(() => ({
  username: user.value?.username || '',
  email: user.value?.email || '',
}));

const roleText = computed(() => {
  const list = roles.value || [];
  return list.map(r => {
    if (r === 'ROLE_ADMIN') return '管理员';
    if (r === 'ROLE_TEACHER') return '教师';
    if (r === 'ROLE_STUDENT') return '学生';
    return r;
  }).join('、') || '-';
});

const rules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '新密码至少6位', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value && value === form.oldPassword) return callback(new Error('新密码不能与旧密码相同'));
        callback();
      },
      trigger: 'blur',
    },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== form.newPassword) return callback(new Error('两次输入的新密码不一致'));
        callback();
      },
      trigger: 'blur',
    },
  ],
};

watch(() => props.modelValue, (v) => {
  if (v) {
    activeTab.value = 'info';
    form.oldPassword = '';
    form.newPassword = '';
    form.confirmPassword = '';
  }
});

async function submitChangePassword() {
  if (!formRef.value) return;
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;
  try {
    await ElMessageBox.confirm(
      '修改密码后将退出当前登录状态，是否继续？',
      '二次确认',
      {
        confirmButtonText: '继续修改',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );
  } catch {
    return;
  }
  submitting.value = true;
  try {
    await changePassword({
      oldPassword: form.oldPassword,
      newPassword: form.newPassword,
    });
    ElMessage.success('密码修改成功，请使用新密码登录');
    visibleProxy.value = false;
    authStore.logout();
    window.location.href = '/login';
  } catch (e) {
    ElMessage.error(e.message || '修改密码失败');
  } finally {
    submitting.value = false;
  }
}
</script>
