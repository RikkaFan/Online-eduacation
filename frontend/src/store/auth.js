import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

export const useAuthStore = defineStore('auth', () => {
  const user = ref(JSON.parse(localStorage.getItem('user') || sessionStorage.getItem('user')) || null);

  const isAuthenticated = computed(() => !!user.value);
  const roles = computed(() => user.value?.roles || []);

  async function login(credentials, rememberMe) {
    const response = await fetch('http://localhost:8081/api/auth/signin', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(credentials),
    });

    if (!response.ok) {
      const errorData = await response.json();
      throw new Error(errorData.message || '登录失败');
    }

    const userData = await response.json();
    user.value = userData;

    if (rememberMe) {
      localStorage.setItem('user', JSON.stringify(userData));
    } else {
      sessionStorage.setItem('user', JSON.stringify(userData));
    }
  }

  function logout() {
    user.value = null;
    localStorage.removeItem('user');
    sessionStorage.removeItem('user');
  }

  return { user, isAuthenticated, roles, login, logout };
});
