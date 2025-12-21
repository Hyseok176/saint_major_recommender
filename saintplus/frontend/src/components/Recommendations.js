import React, { useState, useEffect } from 'react';
import { getRecommendations } from '../api/courseApi';
import './Recommendations.css';

function Recommendations() {
  const [recommendations, setRecommendations] = useState([]);
  const [semester, setSemester] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    loadRecommendations();
  }, []);

  const loadRecommendations = async (selectedSemester = null) => {
    setLoading(true);
    setError('');
    
    try {
      const data = await getRecommendations(selectedSemester);
      setRecommendations(data);
    } catch (err) {
      console.error('추천 목록 로드 실패:', err);
      setError('추천 목록을 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleSemesterChange = (e) => {
    const value = e.target.value;
    const semesterValue = value === '' ? null : parseInt(value);
    setSemester(semesterValue);
    loadRecommendations(semesterValue);
  };

  return (
    <div className="recommendations">
      <h2>통계 기반 과목 추천</h2>
      
      <div className="filter-section">
        <label>학기 선택:</label>
        <select value={semester || ''} onChange={handleSemesterChange}>
          <option value="">전체</option>
          <option value="1">1학기</option>
          <option value="2">2학기</option>
          <option value="3">여름학기</option>
          <option value="4">겨울학기</option>
        </select>
      </div>

      {loading && <div className="loading">로딩 중...</div>}
      
      {error && <div className="error-message">{error}</div>}

      {!loading && !error && recommendations.length === 0 && (
        <div className="no-data">추천할 과목이 없습니다.</div>
      )}

      {!loading && recommendations.length > 0 && (
        <div className="recommendations-list">
          {recommendations.map((course, index) => (
            <div key={index} className="recommendation-card">
              <h3>{course}</h3>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default Recommendations;
