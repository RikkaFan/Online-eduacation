<template>
  <div class="register-container">
    <div class="ambient-bg">
      <div class="shape shape1"></div>
      <div class="shape shape2"></div>
    </div>

    <div class="register-card">
      <div v-if="registerError" class="error-toast">
        {{ registerError }}
      </div>
      <div v-if="registerSuccess" class="success-toast">
        {{ registerSuccess }}
      </div>

      <div class="title-group">
        <h2 class="subtitle">在线教育测评系统</h2>
        <h1 class="title">用户注册</h1>
      </div>

      <form @submit.prevent="handleRegister" class="register-form">
        <div class="input-group">
          <div class="input-wrapper" :class="{ 'shake': isShaking }">
            <span class="input-icon">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z" clip-rule="evenodd" />
              </svg>
            </span>
            <input
              type="text"
              v-model="username"
              placeholder="账号"
              class="form-input"
            />
          </div>
        </div>

        <div class="input-group">
          <div class="input-wrapper" :class="{ 'shake': isShaking }">
            <span class="input-icon">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                <path d="M2.003 5.884L10 9.882l7.997-3.998A2 2 0 0016 4H4a2 2 0 00-1.997 1.884z" />
                <path d="M18 8.118l-8 4-8-4V14a2 2 0 002 2h12a2 2 0 002-2V8.118z" />
              </svg>
            </span>
            <input
              type="email"
              v-model="email"
              placeholder="邮箱"
              class="form-input"
            />
          </div>
        </div>

        <div class="input-group">
          <div class="input-wrapper" :class="{ 'shake': isShaking }">
            <span class="input-icon">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M10 1a4.5 4.5 0 00-4.5 4.5V9H5a2 2 0 00-2 2v6a2 2 0 002 2h10a2 2 0 002-2v-6a2 2 0 00-2-2h-.5V5.5A4.5 4.5 0 0010 1zm3 8V5.5a3 3 0 10-6 0V9h6z" clip-rule="evenodd" />
              </svg>
            </span>
            <input
              :type="passwordVisible ? 'text' : 'password'"
              v-model="password"
              placeholder="密码"
              class="form-input"
            />
            <span class="input-icon right" @click="passwordVisible = !passwordVisible">
              <svg v-if="!passwordVisible" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                <path d="M10 12.5a2.5 2.5 0 100-5 2.5 2.5 0 000 5z" />
                <path fill-rule="evenodd" d="M.664 10.59a1.651 1.651 0 010-1.18l.88-1.473a1.65 1.65 0 012.899 0l.88 1.473a1.65 1.65 0 010 1.18l-.88 1.473a1.65 1.65 0 01-2.899 0l-.88-1.473zM10 17a7 7 0 100-14 7 7 0 000 14z" clip-rule="evenodd" />
              </svg>
              <svg v-else xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M3.28 2.22a.75.75 0 00-1.06 1.06l14.5 14.5a.75.75 0 101.06-1.06L3.28 2.22zM7.06 7.06a2.5 2.5 0 003.88 3.88l-3.88-3.88zM10 12.5a2.5 2.5 0 01-2.5-2.5 1.65 1.65 0 01.05-.39l1.473-.88a1.65 1.65 0 011.18 0l1.473.88c.03.05.05.1.05.15a2.5 2.5 0 01-2.5 2.5z" clip-rule="evenodd" />
              </svg>
            </span>
          </div>
        </div>

        <div class="input-group">
          <div class="input-wrapper" :class="{ 'shake': isShaking }">
            <span class="input-icon">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M10 1a4.5 4.5 0 00-4.5 4.5V9H5a2 2 0 00-2 2v6a2 2 0 002 2h10a2 2 0 002-2v-6a2 2 0 00-2-2h-.5V5.5A4.5 4.5 0 0010 1zm3 8V5.5a3 3 0 10-6 0V9h6z" clip-rule="evenodd" />
              </svg>
            </span>
            <input
              :type="confirmPasswordVisible ? 'text' : 'password'"
              v-model="confirmPassword"
              placeholder="确认密码"
              class="form-input"
            />
            <span class="input-icon right" @click="confirmPasswordVisible = !confirmPasswordVisible">
              <svg v-if="!confirmPasswordVisible" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                <path d="M10 12.5a2.5 2.5 0 100-5 2.5 2.5 0 000 5z" />
                <path fill-rule="evenodd" d="M.664 10.59a1.651 1.651 0 010-1.18l.88-1.473a1.65 1.65 0 012.899 0l.88 1.473a1.65 1.65 0 010 1.18l-.88 1.473a1.65 1.65 0 01-2.899 0l-.88-1.473zM10 17a7 7 0 100-14 7 7 0 000 14z" clip-rule="evenodd" />
              </svg>
              <svg v-else xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M3.28 2.22a.75.75 0 00-1.06 1.06l14.5 14.5a.75.75 0 101.06-1.06L3.28 2.22zM7.06 7.06a2.5 2.5 0 003.88 3.88l-3.88-3.88zM10 12.5a2.5 2.5 0 01-2.5-2.5 1.65 1.65 0 01.05-.39l1.473-.88a1.65 1.65 0 011.18 0l1.473.88c.03.05.05.1.05.15a2.5 2.5 0 01-2.5 2.5z" clip-rule="evenodd" />
              </svg>
            </span>
          </div>
        </div>

        <div class="input-group">
          <div class="input-wrapper">
            <span class="input-icon">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M4 4a2 2 0 012-2h8a2 2 0 012 2v12a1 1 0 110 2h-3a1 1 0 01-1-1v-2a1 1 0 00-1-1H9a1 1 0 00-1 1v2a1 1 0 01-1 1H4a1 1 0 110-2V4z" clip-rule="evenodd" />
              </svg>
            </span>
            <input
              type="text"
              v-model="department"
              placeholder="院系（选填）"
              class="form-input"
            />
          </div>
        </div>

        <button type="submit" class="register-button" :disabled="isRegisterDisabled">
          <div v-if="isLoading" class="loading-spinner"></div>
          {{ isLoading ? '注册中...' : '注册' }}
        </button>
      </form>

      <div class="login-prompt">
        已有账号？ <router-link to="/login">立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { signup } from '@/api/auth';

const username = ref('');
const email = ref('');
const password = ref('');
const confirmPassword = ref('');
const department = ref('');
const passwordVisible = ref(false);
const confirmPasswordVisible = ref(false);
const isShaking = ref(false);

const isLoading = ref(false);
const registerError = ref('');
const registerSuccess = ref('');

const router = useRouter();

const isRegisterDisabled = computed(() => {
  return !username.value || !email.value || !password.value || !confirmPassword.value || isLoading.value;
});

const triggerShake = () => {
  isShaking.value = true;
  setTimeout(() => {
    isShaking.value = false;
  }, 500);
};

const handleRegister = async () => {
  if (isRegisterDisabled.value) return;

  if (password.value !== confirmPassword.value) {
    registerError.value = '两次输入的密码不一致';
    triggerShake();
    setTimeout(() => { registerError.value = ''; }, 3000);
    return;
  }

  registerError.value = '';
  registerSuccess.value = '';
  isLoading.value = true;

  try {
    await signup({
      username: username.value,
      password: password.value,
      email: email.value,
      department: department.value || undefined,
    });

    registerSuccess.value = '注册成功，即将跳转到登录页面...';
    setTimeout(() => {
      router.push('/login');
    }, 1500);
  } catch (error) {
    registerError.value = error.message || '注册失败，请重试';
    triggerShake();
    password.value = '';
    confirmPassword.value = '';
    setTimeout(() => { registerError.value = ''; }, 3000);
  } finally {
    isLoading.value = false;
  }
};
</script>

<style scoped>
*,
*::before,
*::after {
  box-sizing: border-box;
}

@keyframes float {
  0% { transform: translate(0, 0); }
  50% { transform: translate(20px, -30px); }
  100% { transform: translate(0, 0); }
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  10%, 30%, 50%, 70%, 90% { transform: translateX(-4px); }
  20%, 40%, 60%, 80% { transform: translateX(4px); }
}

.shake {
  animation: shake 0.5s ease-in-out;
}

.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f5f5f7;
  font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Helvetica Neue", Arial, sans-serif;
  position: relative;
  overflow: hidden;
  padding: 20px 0;
}

.ambient-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
}

.shape {
  position: absolute;
  border-radius: 50%;
  filter: blur(200px);
  opacity: 0.5;
}

.shape1 {
  width: 600px;
  height: 600px;
  background: rgba(0, 122, 255, 0.4);
  top: -20%;
  left: -20%;
  animation: float 18s ease-in-out infinite;
}

.shape2 {
  width: 550px;
  height: 550px;
  background: rgba(175, 82, 222, 0.3);
  bottom: -15%;
  right: -15%;
  animation: float 22s ease-in-out infinite reverse;
}

.register-card {
  width: 360px;
  padding: 40px;
  background-color: rgba(255, 255, 255, 0.8);
  border-radius: 24px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  display: flex;
  flex-direction: column;
  gap: 20px;
  z-index: 1;
  position: relative;
}

.title-group {
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.subtitle {
  font-size: 16px;
  font-weight: 500;
  color: #86868b;
  margin: 0;
}

.title {
  font-size: 28px;
  font-weight: 600;
  color: #1d1d1f;
  text-align: center;
  margin: 0;
}

.register-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.input-group {
  position: relative;
  width: 100%;
}

.input-wrapper {
  display: flex;
  align-items: center;
  width: 100%;
  height: 48px;
  padding: 0 16px;
  background-color: #f0f0f2;
  border: 1px solid transparent;
  border-radius: 12px;
  transition: all 0.2s ease-in-out;
}

.input-wrapper:focus-within {
  background-color: white;
  border-color: #007aff;
  box-shadow: 0 0 0 3px rgba(0, 122, 255, 0.3);
}

.input-icon {
  color: #86868b;
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.input-icon.right {
  cursor: pointer;
}

.form-input {
  flex: 1;
  width: 0;
  height: 100%;
  padding: 0 12px;
  background-color: transparent;
  border: none;
  outline: none;
  font-size: 16px;
  color: #1d1d1f;
  -webkit-appearance: none;
}

.form-input::placeholder {
  color: #8e8e93;
}

.register-button {
  width: 100%;
  height: 48px;
  background-color: #007aff;
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 17px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  margin-top: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.register-button:hover {
  opacity: 0.9;
}

.register-button:active {
  transform: scale(0.98);
}

.register-button:disabled {
  background-color: #007aff;
  opacity: 0.5;
  cursor: not-allowed;
  transform: scale(1);
}

.register-button:disabled:hover {
  opacity: 0.5;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.5);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.success-toast {
  position: absolute;
  top: -60px;
  left: 50%;
  transform: translateX(-50%);
  background-color: #34c759;
  color: white;
  padding: 10px 20px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 500;
  box-shadow: 0 4px 12px rgba(52, 199, 89, 0.3);
  animation: slide-down 0.5s cubic-bezier(0.25, 0.8, 0.25, 1) forwards;
  white-space: nowrap;
}

.error-toast {
  position: absolute;
  top: -60px;
  left: 50%;
  transform: translateX(-50%);
  background-color: #ff3b30;
  color: white;
  padding: 10px 20px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 500;
  box-shadow: 0 4px 12px rgba(255, 59, 48, 0.3);
  animation: slide-down 0.5s cubic-bezier(0.25, 0.8, 0.25, 1) forwards;
  white-space: nowrap;
}

@keyframes slide-down {
  from { top: -60px; opacity: 0; }
  to { top: 20px; opacity: 1; }
}

.login-prompt {
  text-align: center;
  font-size: 14px;
  color: #86868b;
  margin-top: -4px;
}

.login-prompt a {
  color: #007aff;
  font-weight: 500;
  text-decoration: none;
}

.login-prompt a:hover {
  text-decoration: underline;
}
</style>
