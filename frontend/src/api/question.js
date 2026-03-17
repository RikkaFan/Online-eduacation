import { getAuthHeaders, API_BASE } from './request';
const CATEGORY_API_URL = `${API_BASE}/api/question-categories`;
const QUESTION_API_URL = `${API_BASE}/api/questions`;

// ================= 分类管理 API =================

export async function getCategories() {
  const response = await fetch(CATEGORY_API_URL, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    const errText = await response.text();
    throw new Error(`获取分类列表失败: ${response.status} ${errText}`);
  }

  return response.json();
}

export async function createCategory(data) {
  const response = await fetch(CATEGORY_API_URL, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const errText = await response.text();
    throw new Error(`创建分类失败: ${response.status} ${errText}`);
  }

  return response.json();
}

export async function deleteCategory(id) {
  const response = await fetch(`${CATEGORY_API_URL}/${id}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    const errText = await response.text();
    throw new Error(`删除分类失败: ${response.status} ${errText}`);
  }
  
  const contentType = response.headers.get("content-type");
  if (contentType && contentType.indexOf("application/json") !== -1) {
    return response.json();
  } else {
    return response.text();
  }
}

// ================= 题目管理 API =================

export async function getQuestionsByCategory(categoryId) {
  const response = await fetch(`${QUESTION_API_URL}/category/${categoryId}`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    const errText = await response.text();
    throw new Error(`获取题目列表失败: ${response.status} ${errText}`);
  }

  return response.json();
}

export async function createQuestion(data) {
  const response = await fetch(QUESTION_API_URL, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const errText = await response.text();
    throw new Error(`创建题目失败: ${response.status} ${errText}`);
  }

  return response.json();
}

export async function deleteQuestion(id) {
  const response = await fetch(`${QUESTION_API_URL}/${id}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    const errText = await response.text();
    throw new Error(`删除题目失败: ${response.status} ${errText}`);
  }
  
  const contentType = response.headers.get("content-type");
  if (contentType && contentType.indexOf("application/json") !== -1) {
    return response.json();
  } else {
    return response.text();
  }
}
