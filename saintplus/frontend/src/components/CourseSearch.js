import React, { useState } from 'react';
import { searchCourses } from '../api/courseApi';
import './CourseSearch.css';

function CourseSearch() {
  const [query, setQuery] = useState('');
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSearch = async (e) => {
    e.preventDefault();
    
    if (!query.trim()) {
      setError('검색어를 입력해주세요.');
      return;
    }

    setLoading(true);
    setError('');
    
    try {
      const data = await searchCourses(query);
      setCourses(data);
    } catch (err) {
      console.error('검색 실패:', err);
      setError('과목 검색에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="course-search">
      <h2>과목 검색</h2>
      
      <form onSubmit={handleSearch} className="search-form">
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="과목명, 교수명, 과목코드로 검색하세요"
        />
        <button type="submit" disabled={loading}>
          {loading ? '검색 중...' : '검색'}
        </button>
      </form>

      {error && <div className="error-message">{error}</div>}

      {courses.length > 0 && (
        <div className="search-results">
          <p className="results-count">{courses.length}개의 과목을 찾았습니다.</p>
          <div className="courses-list">
            {courses.map((course) => (
              <div key={course.id} className="course-item">
                <div className="course-header">
                  <h3>{course.courseName}</h3>
                  <span className="course-code">{course.courseCode}</span>
                </div>
                <div className="course-info">
                  <span>교수: {course.professor}</span>
                  <span>학점: {course.credit}</span>
                  <span>학과: {course.department}</span>
                </div>
                {course.time && (
                  <div className="course-time">
                    시간: {course.time}
                  </div>
                )}
                <button className="add-to-cart-btn">장바구니에 추가</button>
              </div>
            ))}
          </div>
        </div>
      )}

      {!loading && courses.length === 0 && query && (
        <div className="no-results">검색 결과가 없습니다.</div>
      )}
    </div>
  );
}

export default CourseSearch;
