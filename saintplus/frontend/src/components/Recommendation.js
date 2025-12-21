import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getAiRecommendations, getStatisticsRecommendations } from '../api/recommendationApi';
import './Recommendation.css';

// 헤더 로고 이미지
const headerLogo = "/e42d8a0a92d55b3ebbe924858099877a88bc7d34.png";

function Recommendation() {
  const navigate = useNavigate();
  const [prompt, setPrompt] = useState('');
  const [statisticsRecommendations, setStatisticsRecommendations] = useState([]);
  const [aiRecommendations, setAiRecommendations] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // 컴포넌트 마운트 시 통계 기반 추천 가져오기
  useEffect(() => {
    fetchStatisticsRecommendations();
  }, []);

  const fetchStatisticsRecommendations = async () => {
    try {
      const data = await getStatisticsRecommendations();
      setStatisticsRecommendations(data);
    } catch (err) {
      console.error('통계 추천 가져오기 실패:', err);
      setError('통계 기반 추천을 불러올 수 없습니다.');
    }
  };

  const handleSearch = async () => {
    if (!prompt.trim()) {
      alert('검색어를 입력해주세요.');
      return;
    }

    setLoading(true);
    setError(null);
    
    try {
      const data = await getAiRecommendations(prompt, 'CSE');
      setAiRecommendations(data);
    } catch (err) {
      console.error('AI 추천 가져오기 실패:', err);
      setError('AI 추천을 불러올 수 없습니다. 다시 시도해주세요.');
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    navigate('/login');
  };

  return (
    <div className="recommendation-container">
      {/* 헤더 */}
      <header className="recommendation-header">
        <div className="header-logo">
          <img src={headerLogo} alt="SAINT+" style={{ width: '54px', height: '54px' }} />
        </div>
        <nav className="header-nav">
          <button className="nav-item">과목 추천</button>
          <button className="nav-item">장바구니</button>
          <button className="nav-item">과목 정보</button>
        </nav>
        <div className="header-actions">
          <button className="mypage-btn">마이페이지</button>
          <button className="logout-btn" onClick={handleLogout}>로그아웃</button>
        </div>
      </header>

      {/* 메인 콘텐츠 */}
      <div className="recommendation-content">
        {/* 왼쪽: 통계 기반 추천 */}
        <div className="statistics-section">
          <h2 className="section-title">통계 기반 추천 과목</h2>
          <div className="course-list">
            {statisticsRecommendations.length === 0 ? (
              <p className="no-data">추천 과목이 없습니다.</p>
            ) : (
              statisticsRecommendations.map((item, index) => (
                <div key={index} className="course-card statistics-card">
                  <div className="course-number">{index + 1}</div>
                  <div className="course-info">
                    <div className="course-code">{item.course?.courseCode || 'N/A'}</div>
                    <div className="course-name">{item.course?.courseName || '과목명 없음'}</div>
                    {item.score && <div className="course-score">점수: {item.score.toFixed(2)}</div>}
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        {/* 오른쪽: AI 기반 추천 */}
        <div className="ai-section">
          {/* 검색 영역 */}
          <div className="search-area">
            <h2 className="section-title">AI 기반 과목 추천</h2>
            <div className="search-box">
              <input
                type="text"
                className="search-input"
                placeholder="예: 데이터 분석 관련 과목 추천해줘"
                value={prompt}
                onChange={(e) => setPrompt(e.target.value)}
                onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
              />
              <button 
                className="search-button" 
                onClick={handleSearch}
                disabled={loading}
              >
                {loading ? '검색 중...' : '검색'}
              </button>
            </div>
          </div>

          {/* AI 추천 결과 */}
          <div className="ai-results">
            {error && <p className="error-message">{error}</p>}
            {aiRecommendations.length === 0 && !error ? (
              <p className="no-data">검색어를 입력하고 과목을 추천받아보세요!</p>
            ) : (
              aiRecommendations.map((item, index) => (
                <div key={index} className="course-card ai-card">
                  <div className="course-number">{index + 1}</div>
                  <div className="course-info">
                    <div className="course-code">{item.course?.courseCode || 'N/A'}</div>
                    <div className="course-name">{item.course?.courseName || '과목명 없음'}</div>
                    <div className="course-score">유사도: {(item.score * 100).toFixed(1)}%</div>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

export default Recommendation;
