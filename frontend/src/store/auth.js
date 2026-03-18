import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { API_BASE } from '@/api/request';

export const useAuthStore = defineStore('auth', () => {
  const user = ref(JSON.parse(localStorage.getItem('user') || sessionStorage.getItem('user')) || null);

  const isAuthenticated = computed(() => !!user.value);
  const roles = computed(() => user.value?.roles || []);

  async function buildLoginError(response) {
    const contentType = response.headers.get('content-type') || '';
    let backendMessage = '';
    try {
      if (contentType.includes('application/json')) {
        const data = await response.json();
        backendMessage = data?.message || '';
      } else {
        backendMessage = await response.text();
      }
    } catch (_) {}

    const err = new Error('登录失败，请重试');
    if (response.status === 401 || response.status === 403) {
      err.message = '账号或密码错误，请重试';
      err.code = 'INVALID_CREDENTIALS';
      return err;
    }
    if (response.status === 404) {
      err.message = '登录接口不可用，请检查前端代理或后端服务地址';
      err.code = 'PROXY_OR_ROUTE_ERROR';
      return err;
    }
    if (response.status >= 500) {
      err.message = '后端服务异常，请稍后再试';
      err.code = 'BACKEND_SERVER_ERROR';
      return err;
    }
    err.message = backendMessage ? `登录失败：${backendMessage}` : `登录失败（HTTP ${response.status}）`;
    err.code = 'LOGIN_HTTP_ERROR';
    return err;
  }

  async function requestLogin(url, credentials) {
    return fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(credentials),
    });
  }

  async function login(credentials, rememberMe) {
    const endpoints = [];
    if (API_BASE) endpoints.push(`${API_BASE}/api/auth/signin`);
    endpoints.push('/api/auth/signin');
    endpoints.push('http://localhost:8081/api/auth/signin');

    let response;
    let lastError;
    for (const url of [...new Set(endpoints)]) {
      try {
        response = await requestLogin(url, credentials);
        break;
      } catch (e) {
        lastError = e;
      }
    }
    if (!response) {
      const err = new Error('请求失败：后端未启动或代理不可用，请检查 8081 服务与前端代理');
      err.code = 'NETWORK_OR_PROXY_ERROR';
      err.cause = lastError;
      throw err;
    }

    if (!response.ok) {
      throw await buildLoginError(response);
    }

    const raw = await response.json();
    const normalized = {
      ...raw,
      accessToken: raw.accessToken || raw.token || '',
      token: raw.token || raw.accessToken || '',
      roles: Array.isArray(raw.roles) ? raw.roles : [],
      username: raw.username || raw.user || raw.name || ''
    };
    user.value = normalized;

    if (rememberMe) {
      localStorage.setItem('user', JSON.stringify(normalized));
    } else {
      sessionStorage.setItem('user', JSON.stringify(normalized));
    }
  }

  function logout() {
    user.value = null;
    localStorage.removeItem('user');
    sessionStorage.removeItem('user');
  }

  return { user, isAuthenticated, roles, login, logout };
});
