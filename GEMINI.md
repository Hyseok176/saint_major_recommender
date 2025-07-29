*   **로그인 및 새로운 과목 매핑 콘솔 로깅 추가**:
    *   `CourseController.java`:
        *   로그인 시도 시 콘솔에 일시, 사용자 ID, IP 주소를 출력하도록 수정.
        *   파일 업로드 시 `HttpServletRequest`를 통해 IP 주소를 가져와 `CourseService.analyzeFile` 메서드에 사용자 ID와 IP 주소를 함께 전달하도록 수정.
        *   `javax.servlet.http` 관련 import 문 제거 (컴파일 오류 해결).
    *   `CourseService.java`:
        *   `analyzeFile` 메서드의 시그니처를 `InputStream`, `userId`, `ipAddress`를 받도록 변경.
        *   새로운 과목 코드가 `COURSE_MAPPING` 테이블에 추가될 때 콘솔에 `NewCourse: <일시>, User:<USER>, IP:<IP>, CODE: <CODE>` 형식으로 출력하도록 수정.