export function getAuthHeaders() {
  const userStr = localStorage.getItem('user') || sessionStorage.getItem('user');
  let token = '';
  if (userStr) {
    try {
      const user = JSON.parse(userStr);
      token = user?.accessToken || user?.token || '';
    } catch (e) {
      console.error('解析 user 存储信息失败', e);
    }
  }

  if (!token) {
    console.warn('未获取到有效 Token，请检查登录状态与 user 存储结构。');
  }

  const headers = {
    'Content-Type': 'application/json',
  };
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  return headers;
}

export async function authFetch(url, options = {}) {
  const headers = { ...(options.headers || {}), ...getAuthHeaders() };
  return fetch(url, { ...options, headers });
}

