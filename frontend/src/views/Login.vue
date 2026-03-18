<template>
  <div class="login-container">
    <div class="ambient-bg">
      <div class="shape shape1"></div>
      <div class="shape shape2"></div>
    </div>

    <div class="login-card">
      <div v-if="loginError" class="error-toast">
        {{ loginError }}
      </div>
      <div v-if="loginSuccess" class="success-toast">
        {{ loginSuccess }}
      </div>

      <div class="title-group">
        <h2 class="subtitle">在线教育测评系统</h2>
        <h1 class="title">欢迎登录</h1>
      </div>

      <form @submit.prevent="handleLogin" class="login-form">
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
                <path fill-rule="evenodd" d="M10 1a4.5 4.5 0 00-4.5 4.5V9H5a2 2 0 00-2 2v6a2 2 0 002 2h10a2 2 0 002-2v-6a2 2 0 00-2-2h-.5V5.5A4.5 4.5 0 0010 1zm3 8V5.5a3 3 0 10-6 0V9h6z" clip-rule="evenodd" />
              </svg>
            </span>
            <input
              ref="passwordInput"
              :type="passwordVisible ? 'text' : 'password'"
              v-model="password"
              placeholder="密码"
              class="form-input"
            />
            <span class="input-icon right" @click="togglePasswordVisibility">
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

        <!-- Slider Captcha -->
        <div ref="sliderContainer" class="slider-captcha" :class="{ 'verified': isVerified }">
          <div class="slider-track" :style="{ width: sliderValue + 'px' }"></div>
          <div 
            class="slider-handle" 
            :style="{ left: sliderValue + 'px' }"
            @mousedown="startDrag"
            @touchstart="startDrag"
          >
            <svg v-if="!isVerified" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5" />
            </svg>
            <svg v-else xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="3" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5" />
            </svg>
          </div>
          <span class="slider-text">{{ isVerified ? '验证通过' : '请按住滑块，拖动到最右侧' }}</span>
        </div>

        <div class="actions-bar">
          <label class="remember-me">
            <input type="checkbox" v-model="rememberMe" class="ios-switch" />
            记住账号
          </label>
          <a href="#" class="forgot-password">忘记密码？</a>
        </div>

        <button type="submit" class="login-button" :disabled="isLoginDisabled">
          <div v-if="isLoading" class="loading-spinner"></div>
          {{ isLoading ? '登录中...' : '登录' }}
        </button>
      </form>

      <div class="register-prompt">
        还没有账号？ <a href="#">立即注册</a>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/store/auth';

// Form State
const username = ref('');
const password = ref('');
const passwordInput = ref(null);
const passwordVisible = ref(false);
const rememberMe = ref(false);
const isShaking = ref(false);

// Status State
const isLoading = ref(false);
const loginError = ref('');
const loginSuccess = ref('');

// Slider Captcha State
const sliderContainer = ref(null);
const sliderValue = ref(0);
const isDragging = ref(false);
const isVerified = ref(false);
const startX = ref(0);
const maxSliderValue = ref(0);

const router = useRouter();
const authStore = useAuthStore();

// Computed property to disable login button
const isLoginDisabled = computed(() => {
  return !username.value || !password.value || !isVerified.value || isLoading.value;
});

const togglePasswordVisibility = () => {
  passwordVisible.value = !passwordVisible.value;
};

const triggerShake = () => {
  isShaking.value = true;
  setTimeout(() => {
    isShaking.value = false;
  }, 500);
};

// --- Slider Logic ---
const onDrag = (e) => {
  if (!isDragging.value || isVerified.value) return;
  const eventX = e.type.includes('touch') ? e.touches[0].clientX : e.clientX;
  const moveX = eventX - startX.value;
  sliderValue.value = Math.max(0, Math.min(moveX, maxSliderValue.value));
};

const stopDrag = () => {
  if (!isDragging.value || isVerified.value) return;
  isDragging.value = false;
  if (sliderValue.value >= maxSliderValue.value - 2) { // A little tolerance
    sliderValue.value = maxSliderValue.value;
    isVerified.value = true;
  } else {
    sliderValue.value = 0; // Snap back
  }
  document.removeEventListener('mousemove', onDrag);
  document.removeEventListener('mouseup', stopDrag);
  document.removeEventListener('touchmove', onDrag);
  document.removeEventListener('touchend', stopDrag);
};

const startDrag = (e) => {
  if (isVerified.value) return;
  // Prevent text selection during drag, but allow touch scroll
  if (e.type.includes('mouse')) {
    e.preventDefault();
  }
  isDragging.value = true;
  startX.value = e.type.includes('touch') ? e.touches[0].clientX : e.clientX;
  document.addEventListener('mousemove', onDrag);
  document.addEventListener('mouseup', stopDrag);
  document.addEventListener('touchmove', onDrag);
  document.addEventListener('touchend', stopDrag);
};

const updateSliderDimensions = () => {
  if (sliderContainer.value) {
    const containerWidth = sliderContainer.value.offsetWidth;
    const handleWidth = sliderContainer.value.querySelector('.slider-handle').offsetWidth;
    maxSliderValue.value = containerWidth - handleWidth;
  }
};

onMounted(() => {
  updateSliderDimensions();
  window.addEventListener('resize', updateSliderDimensions);
});

onUnmounted(() => {
  window.removeEventListener('resize', updateSliderDimensions);
});
// --- End Slider Logic ---

const handleLogin = async () => {
  if (isLoginDisabled.value) return;

  loginError.value = '';
  loginSuccess.value = '';
  isLoading.value = true;

  try {
    // 构造发送给后端的参数
    const credentials = {
      username: username.value,
      password: password.value
    };

    // 调用 auth.js 中的真实接口发送 HTTP 请求
    await authStore.login(credentials, rememberMe.value);

    // --- 登录成功 ---
    loginSuccess.value = '登录成功，即将跳转...';
    
    setTimeout(() => {
      isLoading.value = false;
      const roles = authStore.roles || [];
      const roleList = Array.isArray(roles.value) ? roles.value : roles;
      if (roleList.includes('ROLE_STUDENT')) {
        router.push('/student/dashboard');
      } else if (roleList.includes('ROLE_TEACHER') || roleList.includes('ROLE_ADMIN')) {
        router.push('/dashboard');
      } else {
        router.push('/dashboard');
      }
    }, 1000);

  } catch (error) {
    // --- 登录失败 ---
    console.error('登录失败:', error);
    // 展示后端返回的具体错误信息，如果没有则使用默认提示
    loginError.value = error.message || '账号或密码错误，请重试';
    triggerShake();
    
    // 重置状态
    password.value = '';
    isVerified.value = false; 
    sliderValue.value = 0;    
    
    // 重新聚焦密码框
    passwordInput.value?.focus();

    // 3秒后隐藏错误提示
    setTimeout(() => {
      loginError.value = '';
    }, 3000);
    
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
  0% {
    transform: translate(0, 0);
  }
  50% {
    transform: translate(20px, -30px);
  }
  100% {
    transform: translate(0, 0);
  }
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  10%, 30%, 50%, 70%, 90% { transform: translateX(-4px); }
  20%, 40%, 60%, 80% { transform: translateX(4px); }
}

.shake {
  animation: shake 0.5s ease-in-out;
}

.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f5f5f7;
  font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Helvetica Neue",
    Arial, sans-serif;
  position: relative;
  overflow: hidden;
}

/* --- Ambient Background --- */
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
  background: rgba(0, 122, 255, 0.4); /* Tech Blue */
  top: -20%;
  left: -20%;
  animation: float 18s ease-in-out infinite;
}

.shape2 {
  width: 550px;
  height: 550px;
  background: rgba(175, 82, 222, 0.3); /* Academic Purple */
  bottom: -15%;
  right: -15%;
  animation: float 22s ease-in-out infinite reverse;
}

.login-card {
  width: 360px;
  padding: 40px;
  background-color: rgba(255, 255, 255, 0.8);
  border-radius: 24px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05),
    0 2px 4px -1px rgba(0, 0, 0, 0.03);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  display: flex;
  flex-direction: column;
  gap: 24px;
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

/* Form */
.login-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
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

/* --- Slider Captcha --- */
.slider-captcha {
  position: relative;
  width: 100%;
  height: 48px;
  background-color: #f0f0f2;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  cursor: pointer;
  user-select: none;
  -webkit-user-select: none;
  -moz-user-select: none;
  transition: all 0.3s ease-in-out;
}

.slider-track {
  position: absolute;
  left: 0;
  top: 0;
  height: 100%;
  background-color: rgba(0, 122, 255, 0.2);
  border-radius: 12px;
  transition: all 0.3s ease-in-out;
}

.slider-handle {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 40px;
  height: 40px;
  background-color: white;
  border-radius: 10px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: grab;
  z-index: 2;
  transition: all 0.3s ease-in-out;
}
.slider-handle:active {
  cursor: grabbing;
}
.slider-handle svg {
  width: 24px;
  height: 24px;
  color: #8e8e93;
  transition: all 0.3s ease-in-out;
}

.slider-text {
  color: #8e8e93;
  font-size: 14px;
  z-index: 1;
  transition: all 0.3s ease-in-out;
}

.slider-captcha.verified {
  cursor: default;
}

.slider-captcha.verified .slider-track {
  background-color: rgba(52, 199, 89, 0.1);
}

.slider-captcha.verified .slider-handle {
  background-color: #34c759;
  cursor: default;
}

.slider-captcha.verified .slider-handle svg {
  color: white;
}

.slider-captcha.verified .slider-text {
  color: #34c759;
  font-weight: 500;
}


.actions-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  margin-top: 4px;
  margin-bottom: 4px;
}

.remember-me {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #333;
  cursor: pointer;
}

.forgot-password {
  color: #86868b;
  text-decoration: none;
  transition: color 0.2s ease;
}

.forgot-password:hover {
  color: #007aff;
}

.ios-switch {
  position: relative;
  width: 40px;
  height: 24px;
  -webkit-appearance: none;
  appearance: none;
  background: #e9e9eb;
  border-radius: 12px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.ios-switch::before {
  content: "";
  position: absolute;
  top: 2px;
  left: 2px;
  width: 20px;
  height: 20px;
  background: white;
  border-radius: 50%;
  box-shadow: 0 1px 2px rgba(0,0,0,0.1);
  transition: transform 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.ios-switch:checked {
  background-color: #34c759; /* Apple Green */
}

.ios-switch:checked::before {
  transform: translateX(16px);
}

.login-button {
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
  margin-top: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.login-button:hover {
  opacity: 0.9;
}

.login-button:active {
  transform: scale(0.98);
}

.login-button:disabled {
  background-color: #007aff;
  opacity: 0.5;
  cursor: not-allowed;
  transform: scale(1);
}

.login-button:disabled:hover {
  opacity: 0.5;
}

/* --- Loading & Error State --- */
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
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
}

@keyframes slide-down {
  from {
    top: -60px;
    opacity: 0;
  }
  to {
    top: 20px;
    opacity: 1;
  }
}

.register-prompt {
  text-align: center;
  font-size: 14px;
  color: #86868b;
  margin-top: -8px;
}

.register-prompt a {
  color: #007aff;
  font-weight: 500;
  text-decoration: none;
}

.register-prompt a:hover {
  text-decoration: underline;
}
</style>
