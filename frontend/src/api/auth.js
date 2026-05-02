import { API_BASE, getAuthHeaders } from './request';

const API = `${API_BASE}/api/auth`;

export async function signup(payload) {
  const res = await fetch(`${API}/signup`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(payload),
  });
  if (!res.ok) {
    const t = await res.text();
    throw new Error(`注册失败: ${res.status} ${t}`);
  }
  return res.json();
}

export async function resetPassword(payload) {
  const res = await fetch(`${API}/reset-password`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(payload),
  });
  if (!res.ok) {
    const t = await res.text();
    throw new Error(`重置密码失败: ${res.status} ${t}`);
  }
  return res.json();
}
