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
    *   **"다음학기 구성하기" 기능 개선**: `/recommend` 페이지로 이동하며, 사용자의 마지막 이수 학기를 기준으로 다음 학기 제목을 동적으로 표시합니다.
    *   **과목 추천 로직 구현**: `CourseService`의 `recommendCourses` 메서드에 수학과 전공 과목(MAT로 시작) 추천 로직을 구현했습니다. 이 로직은 사용자가 아직 이수하지 않은 MAT 과목 중, 모든 학생들의 수강 기록을 분석하여 해당 과목이 가장 많이 이수된 학기를 기준으로 추천합니다. 또한, 각 과목별로 해당 학기에 수강한 학생의 비율(%)을 함께 표시합니다.
    *   **추천 과목 필터링**: `/recommend` 페이지에서는 사용자가 현재 계획하고 있는 학기(예: 7학기)에 해당하는 과목만 추천하며, 이전 학기에 대한 추천은 표시하지 않습니다.
    *   **데이터베이스 스키마 및 연동 변경**: `SemesterCourse` 엔티티의 `courseName` 필드를 `courseCode`로 변경하고, 파일 업로드 시 `courseCode`를 저장하도록 수정했습니다. `/results` 페이지에서는 `courseCode`를 기반으로 `CourseMapping`에서 실제 과목 이름을 조회하여 표시합니다.
    *   **`Course` 엔티티 확장**: `Course` 클래스에 `percentage` 필드를 추가하여 추천 과목의 비율을 담을 수 있도록 했습니다.
    *   **관련 Repository 메서드 추가 및 임포트 수정**: `SemesterCourseRepository`에 `findByCourseCode` 메서드를, `CourseMappingRepository`에 `findByCourseCodeStartingWith` 메서드를 추가하고, 필요한 임포트 구문을 수정하여 컴파일 오류를 해결했습니다.
*   **인코딩 및 파싱 문제 해결 (2025-07-26)**:
    *   **파일 파싱 인코딩 문제**: `복소수함수론II`와 같이 한글과 로마 숫자가 혼합된 과목 이름이 깨지는 현상 발생.
        *   `CourseService.java`의 `analyzeFile` 메서드에서 파일 입력 스트림을 읽을 때, 인코딩을 `EUC-KR`에서 `UTF-8`로 변경하여 문제를 해결.
    *   **유연한 과목 정보 파싱**: 탭(tab)으로 구분되지 않고 모든 정보가 붙어있는 형식(예: `2024-2MAT4220복소수함수론II1.0A+`)의 과목 정보를 처리하지 못하는 문제 발생.
        *   `CourseService.java`의 `analyzeFile` 메서드에 있는 정규 표현식을 수정하여, 탭 없이 이어붙여진 형식과 다양한 과목 코드(예: `MAT4220`, `MELRN001`)를 모두 처리할 수 있도록 개선.
    *   **서버 응답 인코딩 오류**: 파일 업로드 후 결과를 표시하는 과정에서 `java.lang.IllegalArgumentException` (Unicode character cannot be encoded) 오류 발생.
        *   `application.properties` 파일에 `server.servlet.encoding.charset=UTF-8`, `server.servlet.encoding.enabled=true`, `server.servlet.encoding.force=true` 설정을 추가하여 서버의 요청/응답 인코딩을 UTF-8로 강제하여 문제를 해결.