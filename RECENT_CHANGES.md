# Recent Changes

## 7. '전체 과목 보기' 필터 UI 개선 (2025-08-07)
- **`all-courses.html`**: 전공 선택 드롭다운을 수평 버튼으로 변경하여 사용성을 개선했습니다.
- **`all-courses.html`**: 활성화된 필터 버튼에 스타일을 적용하여 현재 선택된 필터를 명확히 표시하도록 했습니다.
- **`all-courses.html`**: Thymeleaf의 URL 생성 기능을 사용하도록 `th:onclick` 속성을 수정하여 필터링 기능의 안정성을 높였습니다.
- **`CourseController.java`**: 파일 업로드 시 전공명의 공백을 제거하여 데이터 일관성을 확보했습니다.

## 6. 로그인 페이지 로고 이미지 추가
- **`index.html`**: 로그인 화면에 `logo.jpg` 이미지를 추가하고, 이미지 크기 조절을 위한 CSS를 적용했습니다.
- **`index.html`**: 이미지 파일 참조를 `logo.png`에서 `logo.jpg`로 변경했습니다.

## 5. 로그아웃 기능 추가
- **`CourseController.java`**: 세션을 무효화하고 로그인 페이지로 리디렉션하는 `/logout` 엔드포인트를 추가했습니다.
- **`upload-file.html`, `results.html`, `recommend.html`**: 로그인 페이지를 제외한 모든 페이지의 우측 상단에 로그아웃 버튼을 추가했습니다.
- **`upload-file.html`, `results.html`, `recommend.html`**: 로그아웃 버튼의 CSS `position`을 `absolute`에서 `fixed`로 변경하여 스크롤 시에도 항상 같은 위치에 고정되도록 했습니다.

## 4. UI 개선
- **`results.html`**: 학기 표시에 "학기"라는 단어를 추가하여 "1.0 학기", "1.5 학기"와 같이 표시되도록 수정했습니다.
- **`CourseController.java`**: `/recommend` 페이지의 제목을 "Recommended Courses for Next Semester"에서 "과목 추천"으로 변경했습니다.
- **`CourseController.java`**: 회원가입 성공 시 표시되는 메시지를 "Registration successful. Please log in."에서 "회원가입이 완료되었습니다."로 변경했습니다.

## 3. Hibernate SQL 쿼리 출력 비활성화
- **`application.properties`**: `spring.jpa.show-sql` 속성을 `true`에서 `false`로 변경하여 Hibernate SQL 쿼리가 콘솔에 표시되지 않도록 했습니다.

## 2. 콘솔 로깅 개선
- **`CourseController.java`**: 
    - 로그인 및 회원가입 성공 시 콘솔에 날짜, 시간, 사용자 ID, 클라이언트 IP 주소를 영문 형식으로 출력하도록 추가했습니다.
    - 기존의 한글 콘솔 출력 메시지를 제거했습니다.

## 1. 보안 강화: 세션 기반 인증 구현
- **`CourseController.java`**: `userId`를 URL 파라미터 대신 `HttpSession`을 통해 관리하도록 수정했습니다.
- **`upload-file.html`, `results.html`, `recommend.html`**: URL 파라미터로 `userId`를 전달하는 부분을 제거하고 세션 기반으로 변경했습니다.