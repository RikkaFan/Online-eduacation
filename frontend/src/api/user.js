import { API_BASE, getAuthHeaders } from './request';

const API = `${API_BASE}/api/users`;

export async function getUsers() {
  const res = await fetch(API, {
    method: 'GET',
    headers: getAuthHeaders(),
  });
  if (!res.ok) {
    const t = await res.text();
    throw new Error(`获取用户列表失败: ${res.status} ${t}`);
  }
  return res.json();
}

export async function createUser(payload) {
  const res = await fetch(API, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(payload),
  });
  if (!res.ok) {
    const t = await res.text();
    throw new Error(`新增用户失败: ${res.status} ${t}`);
  }
  return res.json();
}

export async function updateUser(id, payload) {
  const res = await fetch(`${API}/${id}`, {
    method: 'PUT',
    headers: getAuthHeaders(),
    body: JSON.stringify(payload),
  });
  if (!res.ok) {
    const t = await res.text();
    throw new Error(`更新用户失败: ${res.status} ${t}`);
  }
  return res.json();
}

export async function deleteUser(id) {
  const res = await fetch(`${API}/${id}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });
  if (!res.ok) {
    const t = await res.text();
    throw new Error(`删除用户失败: ${res.status} ${t}`);
  }
  const contentType = res.headers.get('content-type') || '';
  if (contentType.includes('application/json')) return res.json();
  return res.text();
}

export async function changePassword(payload) {
  const res = await fetch(`${API}/password`, {
    method: 'PUT',
    headers: getAuthHeaders(),
    body: JSON.stringify(payload),
  });
  if (!res.ok) {
    const t = await res.text();
    throw new Error(`修改密码失败: ${res.status} ${t}`);
  }
  const contentType = res.headers.get('content-type') || '';
  if (contentType.includes('application/json')) return res.json();
  return res.text();
}
