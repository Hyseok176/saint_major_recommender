import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { updateMajors } from '../api/authApi';
import { uploadAndParse } from '../api/transcriptApi';
import './InitialSetup.css';

// 헤더 로고 이미지
const headerLogo = "/e42d8a0a92d55b3ebbe924858099877a88bc7d34.png";

function InitialSetup() {
  const [major1, setMajor1] = useState('');
  const [major2, setMajor2] = useState('');
  const [major3, setMajor3] = useState('');
  const [credits, setCredits] = useState('');
  const [selectedFile, setSelectedFile] = useState(null);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleFileChange = async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    
    setSelectedFile(file);
    
    // 파일 선택 즉시 업로드
    setLoading(true);
    try {
      
      // 추출된 전공을 자동으로 입력
      if (majors.length > 0) setMajor1(majors[0] || '');
      if (majors.length > 1) setMajor2(majors[1] || '');
      if (majors.length > 2) setMajor3(majors[2] || '');
      
      alert('파일에서 전공 정보를 추출했습니다!');
    } catch (error) {
      console.error('파일 업로드 실패:', error);
      console.error('에러 응답:', error.response);
      const errorMessage = error.response?.data?.message || error.message || '알 수 없는 오류';
      alert(`파일 업로드에 실패했습니다.\n오류: ${errorMessage}`);
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async () => {
    if (!major1) {
      alert('1전공을 입력해주세요.');
      return;
    }

    setLoading(true);
    try {
      // 전공 정보 저장
      await updateMajors(major1, major2 || '', major3 || '');
      
      // 파일이 있으면 파싱도 수행
      if (selectedFile) {
        await uploadAndParse(selectedFile, major1, major2 || '', major3 || '');
        alert('전공 정보 및 수강 이력이 저장되었습니다!');
      } else {
        alert('전공 정보가 저장되었습니다!');
      }
      
      navigate('/recommend');
    } catch (error) {
      console.error('저장 실패:', error);
      const errorMessage = error.response?.data?.message || error.message || '알 수 없는 오류';
      alert(`저장에 실패했습니다.\n오류: ${errorMessage}`);
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    navigate('/login');
  };

  return (
    <div className="initial-setup-container">
      {/* 헤더 */}
      <header className="setup-header">
        <img src={headerLogo} alt="Saint+ Logo" className="header-logo" />
        <nav className="header-nav">
          <button className="nav-item">과목 추천</button>
          <button className="nav-item">시간표 수정하기</button>
          <button className="nav-item">시간표 공유하기</button>
        </nav>
        <div className="header-actions">
          <button className="mypage-btn">마이페이지</button>
          <button className="logout-btn" onClick={handleLogout}>로그아웃</button>
        </div>
      </header>

      {/* 메인 콘텐츠 */}
      <main className="setup-content">
        {/* 파일 다운로드 안내 */}
        <div className="download-guide">
          <h2>파일 다운로드 방법</h2>
          <p>SAINT에서 성적표를 다운로드하여 업로드해주세요.</p>
        </div>

        {/* 전공 선택 */}
        <div className="major-selection">
          <h3>전공 선택하기</h3>
          
          <div className="major-input-group">
            <label>1전공 :</label>
            <input
              type="text"
              value={major1}
              onChange={(e) => setMajor1(e.target.value)}
              placeholder="컴퓨터 공학과"
              className="major-input active"
            />
          </div>

          <div className="major-input-group">
            <label>2전공 :</label>
            <select
              value={major2}
              onChange={(e) => setMajor2(e.target.value)}
              className="major-select"
            >
              <option value="">선택하세요</option>
              <option value="컴퓨터공학과">컴퓨터공학과</option>
              <option value="소프트웨어학과">소프트웨어학과</option>
              <option value="전자공학과">전자공학과</option>
            </select>
          </div>

          <div className="major-input-group">
            <label>3전공 :</label>
            <select
              value={major3}
              onChange={(e) => setMajor3(e.target.value)}
              className="major-select"
            >
              <option value="">선택하세요</option>
              <option value="컴퓨터공학과">컴퓨터공학과</option>
              <option value="소프트웨어학과">소프트웨어학과</option>
              <option value="전자공학과">전자공학과</option>
            </select>
          </div>

          <div className="credits-group">
            <label>이수 학점</label>
            <input
              type="number"
              value={credits}
              onChange={(e) => setCredits(e.target.value)}
              className="credits-input"
            />
            <span className="credits-total">/ 130</span>
          </div>
        </div>

        {/* 버튼 영역 */}
        <div className="action-buttons">
          <input
            type="file"
            accept=".txt"
            onChange={handleFileChange}
            style={{ display: 'none' }}
            id="file-upload-input"
          />
          <button 
            className="upload-btn"
            onClick={() => document.getElementById('file-upload-input').click()}
            disabled={loading}
          >
            {loading ? '처리 중...' : '파일 업로드'}
          </button>
          <button className="save-btn" onClick={handleSave} disabled={loading}>
            {loading ? '저장 중...' : '저장'}
          </button>
        </div>
      </main>
    </div>
  );
}

export default InitialSetup;
