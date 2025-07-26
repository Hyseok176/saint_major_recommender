* The user has installed IntelliJ IDEA and will reboot. I need to guide them through license activation (step 3) when they return.
* 현재 진행 중인 프로젝트는 학생들이 대학에서 자신이 수강했던 과목들의 기록이 담긴 텍스트파일을 올리면 정리해서 보여주고 있습니다.
* 모든 개발 작업은 'taeyoon' 브랜치에서 진행됩니다.
* The user will manually install Maven and Gradle using Chocolatey with administrator privileges.
* 사용자가 JAVA_HOME 환경 변수 설정을 완료했으며, 시스템을 재시작한 후에는 'gradlew bootRun' 명령을 다시 실행하여 Spring Boot 애플리케이션을 시작해야 합니다.
* 앞으로 대화를 종료하기전에 GEMINI.md에 대화내역을 기록해두고, 내가 대화를 시작하기 전에 GEMINI.md파일을 읽어오라고하면 그걸 읽고 적힌 내용을 토대로 대화를 이어나갈 것입니다.
* 현재 웹사이트는 다음 3단계로 구성됩니다:
    1.  **로그인 페이지**: 사용자가 로그인합니다.
    2.  **파일 입력 페이지**: 로그인 후 텍스트 파일을 입력합니다.
    3.  **결과 페이지**: 파일 입력 후 결과를 확인합니다.

* **최근 작업 요약:**
    *   H2 데이터베이스를 사용하여 사용자 정보 및 수강 기록을 영구 저장하도록 구현 중.
    *   `User` 및 `SemesterCourse` 엔티티와 관련 리포지토리 생성.
    *   `CourseController` 및 `CourseService`를 수정하여 로그인, 회원가입, 파일 업로드 시 데이터베이스 저장 및 조회 기능 추가.
    *   `application.properties`에 H2 파일 기반 데이터베이스 설정을 명시적으로 추가 (경로 변경, `AUTO_SERVER=TRUE`, `DB_CLOSE_ON_EXIT=FALSE`, `MVCC=TRUE`, `ddl-auto=update`, `generate-ddl=true`, `show-sql=true`, `format_sql=true`, `hibernate.temp.use_jdbc_metadata_defaults=false`).
    *   `index.html` 및 `upload-file.html` 웹 페이지 수정.
    *   콘솔 출력 인코딩 문제 해결을 위해 `build.gradle`에 JVM 인코딩 설정 추가.
    *   **H2 데이터베이스 문제 해결**: H2 데이터베이스 파일이 지정된 경로에 생성되지 않고, 서버 재시작 시 데이터가 사라지는 문제 해결.
    *   **계절학기 처리 로직 수정**: 계절학기 과목을 직전 학기에 0.5를 더한 학기(예: 3.5학기)로 별도 분류하도록 수정.
        *   `CourseService.java`: 계절학기 식별 및 ".5" 학기 그룹화 로직 구현.
        *   `SemesterCourse.java`: `semester` 필드 타입을 `int`에서 `double`로 변경.
        *   `CourseController.java`: `showResults` 메서드에서 `Double` 타입 학기 정보를 처리하도록 수정.
*   **사용자 ID 및 순서 관리 개선**:
    *   `User` 엔티티의 `id` 필드를 사용자의 학번(String)으로 직접 사용하도록 변경. 기존 `username` 필드는 제거하여 중복을 없앰.
    *   `UserRepository`: `findByUsername` 메서드 제거. `findMaxUserOrder()` 메서드를 추가하여 가장 큰 `userOrder` 값을 조회.
    *   `CourseController`:
        *   회원가입 시 사용자가 입력한 학번을 `User` 엔티티의 `id` 필드에 직접 저장.
        *   `User` 엔티티에 `userOrder` 필드(Long 타입)를 추가하여 회원가입 순서대로 1, 2, 3... 값을 부여.
        *   로그인 및 회원가입 로직에서 `user.getUsername()` 대신 `user.getId()`를 참조하도록 수정.
    *   H2 데이터베이스 파일(`*.mv.db`, `*.trace.db`) 및 `error.txt` 파일은 `.gitignore`에 추가하여 Git 추적에서 제외.
*   **수강 과목 저장 방식 변경**:
    *   `CourseService.java`: `saveCoursesToDatabase` 메서드에서 `SemesterCourse` 엔티티에 과목 이름(`courseName`) 대신 과목 코드(`courseCode`)를 저장하도록 수정.
*   **텍스트 파일 과목 형식**:
    *   텍스트 파일에서 각 과목은 다음과 같은 형식으로 구성됩니다:
        `[학기정보][과목코드][과목이름][학점정보][성적정보]`
    *   예시:
        *   `2022-2ECO2002경제학원론II3.0C+`
            *   학기정보: `2022-2`
            *   과목코드: `ECO2002`
            *   과목이름: `경제학원론II`
            *   학점정보: `3.0`
            *   성적정보: `C+`
        *   `2024-WMELRN001군이러닝취득교과목I3.0ST , S/U`
            *   학기정보: `2024-W`
            *   과목코드: `MELRN001`
            *   과목이름: `군이러닝취득교과목I`
            *   학점정보: `3.0`
            *   성적정보: `ST , S/U` (ST는 성적, S/U는 이수구분)
        *   `2025-1ECO2001경제학원론I3.0`
            *   학기정보: `2025-1`
            *   과목코드: `ECO2001`
            *   과목이름: `경제학원론I`
            *   학점정보: `3.0`
            *   성적정보: (없음)
        *   `2025-1ECO2007거시경제학I3.0E`
            *   학기정보: `2025-1`
            *   과목코드: `ECO2007`
            *   과목이름: `거시경제학I`
            *   학점정보: `3.0`
            *   성적정보: `E`
*   **과목 코드-이름 매핑 및 표시 개선**:
    *   `CourseMapping.java` 엔티티 및 `CourseMappingRepository.java` 리포지토리 추가: 과목 코드와 과목 이름 매핑을 저장하는 새로운 테이블(`COURSE_MAPPING`) 구현.
    *   `CourseService.java`: `analyzeFile` 메서드에서 텍스트 파일 파싱 시, 새로운 과목 코드-이름 매핑이 발견되면 `COURSE_MAPPING` 테이블에 저장.
    *   `CourseController.java`: `showResults` 메서드에서 `SemesterCourse`에 저장된 과목 코드(`courseName` 필드)를 사용하여 `COURSE_MAPPING` 테이블에서 실제 과목 이름을 조회하고, 이를 `/results` 페이지에 표시.
*   **최근 확정된 변경 사항**:
    *   **"다시 분석하기" 버튼 동작 변경**: `results.html`의 "다시 분석하기" 버튼이 로그인 페이지 대신 파일 업로드 페이지(`upload-file.html`)로 이동하도록 변경되었습니다. 이 때 `userId`가 파라미터로 전달됩니다.
    *   **파일 재업로드 시 수강 이력 업데이트 로직**: `CourseService.java`의 `saveCoursesToDatabase` 메소드가 수정되어, 파일 재업로드 시 기존 수강 이력과 비교하여 변경된 내용(새로운 과목 추가, 기존 과목 삭제 등)만 데이터베이스에 반영하도록 개선되었습니다.
    *   **"다음학기 구성하기" 기능 추가**: `results.html`에 "다음학기 구성하기" 버튼이 추가되었습니다. 이 버튼은 `/recommend` 경로로 `userId`와 함께 요청을 보내며, `recommend.html` 페이지로 이동합니다.
    *   **과목 추천 페이지 (`recommend.html`)**: `C:\banghak\next_sem.html`의 구조를 기반으로 한 새로운 Thymeleaf 템플릿 파일(`recommend.html`)이 생성되었습니다. 이 페이지는 `CourseService.recommendCourses` 메소드에서 제공하는 임시(하드코딩된) 추천 과목 목록을 표시합니다.
    *   **JPA `javax` to `jakarta` 마이그레이션**: Spring Boot 3.x 환경에 맞춰 `CourseMapping.java` 파일의 `javax.persistence` import 문이 `jakarta.persistence`로 변경되었습니다. 이는 빌드 시 `cannot find symbol` 오류를 해결하기 위함입니다.
    *   **`recommend.html` "처음으로" 버튼 변경**: `recommend.html`의 "처음으로" 버튼이 "이전으로"로 변경되었고, 클릭 시 `results` 페이지로 돌아가도록 수정되었습니다.
    *   **`CourseController.java` `double` 타입 오류 해결**: `CourseController.java`의 `showResults` 메소드에서 `sc.getSemester().toString()` 오류가 `String.valueOf(sc.getSemester())`로 수정되었습니다.
    *   **화면 표시 과목명 통일**: 분석 결과 페이지(`results.html`)와 다음 학기 구성 페이지(`recommend.html`)에서 과목 코드를 제외하고 과목 이름만 표시되도록 수정되었습니다.
