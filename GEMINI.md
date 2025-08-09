## 2025-08-09

*   **카카오 소셜 로그인 구현**:
    *   `build.gradle`: Spring Security OAuth2 Client 의존성을 추가.
    *   `application.properties`: 카카오 로그인을 위한 OAuth2 클라이언트 ID, 시크릿 및 프로바이더 정보를 설정.
    *   `User.java`, `UserRepository.java`: 기존의 비밀번호 기반 인증 필드를 제거하고, OAuth2 제공자(provider, providerId)와 이메일 필드를 추가. `findByProviderAndProviderId` 쿼리 메소드를 구현.
    *   `CustomOAuth2UserService.java`: 카카오로부터 사용자 정보를 받아온 후, 기존 사용자는 업데이트하고 신규 사용자는 데이터베이스에 새로 등록하는 핵심 로직을 구현.
    *   `SecurityConfig.java`: 기존의 폼 기반 로그인 설정을 모두 제거하고, `oauth2Login`을 사용하여 `CustomOAuth2UserService`를 사용하도록 변경. H2 콘솔 접근을 위해 개발/운영 프로파일 설정을 다시 분리.
    *   `CustomUserDetailsService.java`: 더 이상 사용되지 않아 삭제.
    *   `CourseController.java`: 기존의 `/register`, `/login` 엔드포인트를 삭제하고, `Principal` 객체에서 사용자 정보를 조회하는 로직을 OAuth2 방식에 맞게 수정.

*   **UI/UX 개선**:
    *   `index.html`: 
        *   기존의 로그인/회원가입 폼을 '카카오로 로그인하기' 버튼으로 교체.
        *   깨진 이미지 대신 카카오의 공식 로그인 버튼 이미지를 사용하도록 수정.
        *   사이트 이름 '서강이드'와 기능 소개 문구를 추가하여 초기 페이지 콘텐츠를 보강.
        *   '서강이드' 텍스트에 서강대학교 공식 색상(`#A60026`)을 적용.
        *   사용자 피드백에 따라 로고, 타이틀, 설명, 버튼 간의 수직 간격을 미세 조정.
    *   `recommend.html`: '이전으로', '전체 과목 보기' 버튼이 가로로 정렬되도록 Flexbox를 사용하여 스타일을 수정.
    *   `all-courses.html`: 페이지 바깥에 있던 '이전으로' 버튼을 메인 컨테이너 안으로 이동시키고 다른 페이지와 스타일을 통일.

*   **'전체 과목 보기' 기능 개선**:
    *   `CourseStatDto.java`: 총 수강생 수 정보를 과목 정보와 함께 다루기 위한 DTO를 생성.
    *   `SemesterCourseRepository.java`: 과목별 총 수강생 수를 효율적으로 계산하기 위해 `countDistinctUsersByCourseCode` 쿼리 메소드를 추가.
    *   `CourseService.java`: 특정 전공의 과목 목록 조회 시, 과목별 총 수강생 수를 계산하고 이 수를 기준으로 내림차순 정렬하는 로직으로 변경.
    *   `all-courses.html`:
        *   전공별 과목 목록에서 과목 코드 우측에 총 수강생 수를 회색 글씨로 표시.
        *   Flexbox를 사용하여 수강생 수가 버튼의 오른쪽 끝에 정렬되도록 스타일을 개선.

*   **보안 강화 및 환경 분리**:
    *   **Spring Security 도입**:
        *   `build.gradle`: `spring-boot-starter-security` 의존성을 추가하여 보안 프레임워크를 도입.
        *   비밀번호 암호화: `BCryptPasswordEncoder`를 사용하여 사용자의 비밀번호를 안전하게 해싱하여 데이터베이스에 저장.
        *   `CourseController.java`: 기존의 수동 로그인/회원가입 로직을 Spring Security가 인증을 처리하도록 위임하고, 비밀번호 암호화를 적용.
        *   `index.html`: CSRF(Cross-Site Request Forgery) 공격 방어를 위해 로그인 및 회원가입 폼에 `th:action`을 적용하여 CSRF 토큰을 자동으로 추가.
        *   `CustomUserDetailsService.java`: Spring Security가 사용자 정보를 데이터베이스에서 조회할 수 있도록 `UserDetailsService`를 구현.
    *   **Spring 프로파일(Profile)을 이용한 환경 분리**:
        *   `application.properties`: 기본 활성 프로파일을 `dev`로 설정.
        *   `application-dev.properties`: 개발 환경용 설정을 분리. H2 콘솔 활성화 및 개발용 DB 설정을 포함.
        *   `application-prod.properties`: 운영 환경용 설정 파일을 생성. 운영 DB 설정을 위한 플레이스홀더를 포함.
    *   **보안 설정(SecurityConfig) 프로파일 분리**:
        *   `SecurityConfig.java`: `@Profile` 어노테이션을 사용하여 개발(`dev`)과 운영(`!dev`) 환경에 따라 다른 보안 규칙을 적용하도록 수정.
            *   **개발 환경**: H2 콘솔 접근을 허용하고 관련 보안(CSRF, frame-options)을 설정.
            *   **운영 환경**: H2 콘솔 접근을 차단하고, CSRF 보호 기능을 전면 활성화하여 보안을 강화.

## 2025-08-07

*   **'전체 과목 보기' 필터 UI 개선**:
    *   `all-courses.html`: 기존의 전공 선택 드롭다운 메뉴를 '전체' 및 이수 중인 전공을 나타내는 수평 버튼 그룹으로 변경.
        *   활성화된 필터 버튼은 시각적으로 구분되도록 스타일(배경색, 글자색)을 적용.
        *   필터 버튼들을 왼쪽으로 정렬.
        *   Thymeleaf의 URL 생성 기능(`@{...}`)을 사용하도록 `th:onclick` 속성을 수정하여, 공백이 포함된 전공 이름도 안전하게 처리하고 필터링이 정상적으로 동작하도록 함.
    *   `CourseController.java`: 파일 업로드 시 전공 이름에 포함된 공백을 제거하여 데이터 일관성을 유지하도록 수정.

## 2025-08-05

*   **'전체과목 보기' 기능 추가**:
    *   `recommend.html`, `results.html`: '전체과목 보기' 버튼을 추가하여 `/all-courses` 페이지로 연결.
    *   `all-courses.html`: 새로운 전체 과목 보기 페이지를 생성.
        *   데이터베이스에 저장된 모든 과목을 버튼 형식으로 나열.
        *   과목 버튼을 클릭하면, 해당 버튼 바로 아래에 학기별 수강자 수 통계 차트(Bar Chart)를 표시하도록 구현.
        *   과목명으로 실시간 검색 및 필터링 기능을 추가.
        *   차트 y축을 정수로 표시하고, 크기를 조정.
    *   `CourseController.java`:
        *   `/all-courses` GET 요청을 처리하여 `all-courses.html` 페이지를 반환하는 핸들러를 추가.
        *   `/api/course-stats/{subjectCode}` GET API 엔드포인트를 추가하여, 과목별 학기 수강자 수 통계 데이터를 JSON 형식으로 제공.
        *   로그인하지 않은 사용자가 접근 제한 페이지에 접속 시 로그인 페이지로 리디렉션하고 경고 메시지 표시.
    *   `CourseService.java`:
        *   `getAllCourses()`: `CourseMappingRepository`를 사용하여 데이터베이스에 저장된 모든 과목의 목록을 조회하는 기능을 추가.
        *   `getCourseStats()`: 특정 과목의 학기별 수강자 수를 계산하는 기능을 추가.
            *   차트의 X축은 항상 1-8학기를 기본으로 포함하며, 수강생이 없는 학기는 0으로 표시.
            *   계절학기(여름/겨울)는 수강 기록이 있는 경우에만 차트에 동적으로 추가하여 표시하도록 구현.
    *   `SubjectRepository.java`: 초기 구현에 사용되었으나, `CourseMappingRepository`를 사용하는 것으로 변경되면서 관련 코드 삭제.
    *   `CourseMappingRepository.java`: `findAllExcludingCodes()` 메서드를 추가하여 특정 과목 코드(ETS, COR, SHS, HLU)를 제외한 과목만 조회.

*   **전공 선택 드롭다운 추가**:
    *   `upload-file.html`: 파일 첨부 페이지에 1전공, 2전공, 3전공을 선택할 수 있는 드롭다운 메뉴 3개를 추가.
        *   드롭다운 메뉴를 가로로 한 줄에 표시.
        *   드롭다운의 기본 선택 문구를 "미선택"으로 변경.
    *   `CourseController.java`: `uploadFile` 메서드에서 `major1`, `major2`, `major3` 파라미터를 받아 `User` 엔티티의 해당 필드에 저장하도록 수정.

*   **Commit Message Convention**:
    *   Commit messages will always be in English.