## 2025-08-05

*   **'전체과목 보기' 기능 추가**:
    *   `recommend.html`, `results.html`: '전체과목 보기' 버튼을 추가하여 `/all-courses` 페이지로 연결.
    *   `all-courses.html`: 새로운 전체 과목 보기 페이지를 생성.
        *   데이터베이스에 저장된 모든 과목을 버튼 형식으로 나열.
        *   과목 버튼을 클릭하면, 해당 버튼 바로 아래에 학기별 수강자 수 통계 차트(Bar Chart)를 표시하도록 구현.
    *   `CourseController.java`:
        *   `/all-courses` GET 요청을 처리하여 `all-courses.html` 페이지를 반환하는 핸들러를 추가.
        *   `/api/course-stats/{subjectCode}` GET API 엔드포인트를 추가하여, 과목별 학기 수강자 수 통계 데이터를 JSON 형식으로 제공.
    *   `CourseService.java`:
        *   `getAllCourses()`: `CourseMappingRepository`를 사용하여 데이터베이스에 저장된 모든 과목의 목록을 조회하는 기능을 추가.
        *   `getCourseStats()`: 특정 과목의 학기별 수강자 수를 계산하는 기능을 추가.
            *   차트의 X축은 항상 1-8학기를 기본으로 포함하며, 수강생이 없는 학기는 0으로 표시.
            *   계절학기(여름/겨울)는 수강 기록이 있는 경우에만 차트에 동적으로 추가하여 표시하도록 구현.
    *   `SubjectRepository.java`: 초기 구현에 사용되었으나, `CourseMappingRepository`를 사용하는 것으로 변경되면서 관련 코드 삭제.