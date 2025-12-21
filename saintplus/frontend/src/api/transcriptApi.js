import api from './api';

// 성적표에서 전공 정보 추출
export const extractMajors = async (file) => {
  const formData = new FormData();
  formData.append('file', file);
  
  const response = await api.post('/api/v1/transcripts/extract-majors', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
  return response.data;
};

// 파일 업로드 및 파싱
export const uploadAndParse = async (file, major1, major2, major3) => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('major1', major1);
  formData.append('major2', major2);
  formData.append('major3', major3);
  
  const response = await api.post('/api/v1/transcripts/upload-and-parse', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
  return response.data;
};

// S3 업로드 URL 요청
export const getUploadUrl = async (filename, contentType) => {
  const response = await api.post('/api/v1/transcripts/upload-url', {
    filename,
    contentType,
  });
  return response.data;
};

// 업로드 완료 후 파싱 요청
export const notifyUploadComplete = async (fileKey, major1, major2, major3) => {
  const response = await api.post('/api/v1/transcripts/parse', {
    fileKey,
    major1,
    major2,
    major3,
  });
  return response.data;
};
