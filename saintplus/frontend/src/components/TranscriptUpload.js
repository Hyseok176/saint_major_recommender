import React, { useState } from 'react';
import { extractMajors, getUploadUrl, notifyUploadComplete } from '../api/transcriptApi';
import './TranscriptUpload.css';

function TranscriptUpload() {
  const [file, setFile] = useState(null);
  const [majors, setMajors] = useState([]);
  const [selectedMajors, setSelectedMajors] = useState({
    major1: '',
    major2: '',
    major3: '',
  });
  const [isUploading, setIsUploading] = useState(false);
  const [message, setMessage] = useState('');

  // 파일 선택 핸들러
  const handleFileChange = async (e) => {
    const selectedFile = e.target.files[0];
    if (!selectedFile) return;

    setFile(selectedFile);
    setMessage('전공 정보를 추출 중입니다...');

    try {
      // 전공 추출
      const extractedMajors = await extractMajors(selectedFile);
      setMajors(extractedMajors);
      setMessage('전공을 선택해주세요.');
    } catch (error) {
      console.error('전공 추출 실패:', error);
      setMessage('전공 추출에 실패했습니다. 다시 시도해주세요.');
    }
  };

  // 전공 선택 핸들러
  const handleMajorChange = (majorKey, value) => {
    setSelectedMajors((prev) => ({
      ...prev,
      [majorKey]: value,
    }));
  };

  // 업로드 핸들러
  const handleUpload = async () => {
    if (!file) {
      setMessage('파일을 선택해주세요.');
      return;
    }

    if (!selectedMajors.major1) {
      setMessage('최소 하나의 전공을 선택해주세요.');
      return;
    }

    setIsUploading(true);
    setMessage('업로드 중입니다...');

    try {
      // 1. S3 업로드 URL 요청
      const { uploadUrl, fileKey } = await getUploadUrl(file.name, file.type);

      // 2. S3에 직접 업로드
      await fetch(uploadUrl, {
        method: 'PUT',
        body: file,
        headers: {
          'Content-Type': file.type,
        },
      });

      // 3. 업로드 완료 알림 및 파싱 요청
      await notifyUploadComplete(
        fileKey,
        selectedMajors.major1,
        selectedMajors.major2 || null,
        selectedMajors.major3 || null
      );

      setMessage('성적표가 성공적으로 업로드되었습니다! 파싱이 진행 중입니다.');
      
      // 폼 초기화
      setTimeout(() => {
        setFile(null);
        setMajors([]);
        setSelectedMajors({ major1: '', major2: '', major3: '' });
        setMessage('');
      }, 3000);
    } catch (error) {
      console.error('업로드 실패:', error);
      setMessage('업로드에 실패했습니다. 다시 시도해주세요.');
    } finally {
      setIsUploading(false);
    }
  };

  return (
    <div className="transcript-upload">
      <h2>성적표 업로드</h2>
      
      <div className="upload-section">
        <input
          type="file"
          onChange={handleFileChange}
          accept=".pdf,.jpg,.jpeg,.png"
          disabled={isUploading}
        />
        
        {file && <p className="file-name">선택된 파일: {file.name}</p>}
      </div>

      {majors.length > 0 && (
        <div className="major-selection">
          <h3>전공 선택</h3>
          
          <div className="major-field">
            <label>제1전공 *</label>
            <select
              value={selectedMajors.major1}
              onChange={(e) => handleMajorChange('major1', e.target.value)}
              disabled={isUploading}
            >
              <option value="">선택하세요</option>
              {majors.map((major, idx) => (
                <option key={idx} value={major}>
                  {major}
                </option>
              ))}
            </select>
          </div>

          <div className="major-field">
            <label>제2전공 (선택)</label>
            <select
              value={selectedMajors.major2}
              onChange={(e) => handleMajorChange('major2', e.target.value)}
              disabled={isUploading}
            >
              <option value="">선택하세요</option>
              {majors.map((major, idx) => (
                <option key={idx} value={major}>
                  {major}
                </option>
              ))}
            </select>
          </div>

          <div className="major-field">
            <label>제3전공 (선택)</label>
            <select
              value={selectedMajors.major3}
              onChange={(e) => handleMajorChange('major3', e.target.value)}
              disabled={isUploading}
            >
              <option value="">선택하세요</option>
              {majors.map((major, idx) => (
                <option key={idx} value={major}>
                  {major}
                </option>
              ))}
            </select>
          </div>

          <button
            className="upload-btn"
            onClick={handleUpload}
            disabled={isUploading || !selectedMajors.major1}
          >
            {isUploading ? '업로드 중...' : '업로드'}
          </button>
        </div>
      )}

      {message && (
        <div className={`message ${message.includes('실패') ? 'error' : 'success'}`}>
          {message}
        </div>
      )}
    </div>
  );
}

export default TranscriptUpload;
