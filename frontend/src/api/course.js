const API_URL = 'http://localhost:8081/api/courses';

function getAuthHeaders() {
  const userStr = localStorage.getItem('user') || sessionStorage.getItem('user');
  let token = '';
  if (userStr) {
    try {
      const user = JSON.parse(userStr);
      // Spring Security JwtResponse 通常把 token 放在 accessToken 或 token 字段中
      token = user.accessToken || user.token;
    } catch (e) {
      console.error('Failed to parse user info', e);
    }
  }

  return {
    'Content-Type': 'application/json',
    ...(token ? { 'Authorization': `Bearer ${token}` } : {})
  };
}

export async function getCourses() {
  const response = await fetch(API_URL, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    const errText = await response.text();
    throw new Error(`获取课程列表失败: ${response.status} ${errText}`);
  }

  return response.json();
}

export async function createCourse(courseData) {
  const response = await fetch(API_URL, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(courseData),
  });

  if (!response.ok) {
    const errText = await response.text();
    throw new Error(`创建课程失败: ${response.status} ${errText}`);
  }

  return response.json();
}

export async function deleteCourse(id) {
  const response = await fetch(`${API_URL}/${id}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    const errText = await response.text();
    throw new Error(`删除课程失败: ${response.status} ${errText}`);
  }
  
  // 有些 delete 接口可能不返回 body，只返回 200/204
  const contentType = response.headers.get("content-type");
  if (contentType && contentType.indexOf("application/json") !== -1) {
    return response.json();
  } else {
    return response.text();
  }
}
