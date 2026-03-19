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
  // 兼容后端实体字段 categoryName，容错将入参的 name 映射为 categoryName
  const payload = {
    ...(data || {}),
    ...(data?.name && !data?.categoryName ? { categoryName: data.name } : {})
  };
  const response = await fetch(CATEGORY_API_URL, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(payload),
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

export async function getQuestionsByCourse(courseId) {
  const response = await fetch(`${QUESTION_API_URL}/course/${courseId}`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    const errText = await response.text();
    throw new Error(`获取课程题库失败: ${response.status} ${errText}`);
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

export async function downloadTemplate() {
  const response = await fetch(`${QUESTION_API_URL}/import/template`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    const errText = await response.text();
    throw new Error(`下载导入模板失败: ${response.status} ${errText}`);
  }

  return response.blob();
}

export async function importQuestions(courseId, file) {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('courseId', courseId);

  const headers = getAuthHeaders();
  delete headers['Content-Type'];

  const response = await fetch(`${QUESTION_API_URL}/import`, {
    method: 'POST',
    headers,
    body: formData,
  });

  if (!response.ok) {
    const errText = await response.text();
    throw new Error(`批量导入失败: ${response.status} ${errText}`);
  }

  const contentType = response.headers.get("content-type") || '';
  if (contentType.includes("application/json")) {
    return response.json();
  }
  return response.text();
}
