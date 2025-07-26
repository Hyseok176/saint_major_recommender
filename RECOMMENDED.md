# Current Course Recommendation Logic

This document describes the current implementation of the course recommendation logic within the `CourseService.java` file.

## `recommendCourses` Method

**Location:** `C:\banghak\course_analyzer\src\main\java\com\example\course_analyzer\CourseService.java`

**Description:**

The `recommendCourses` method is currently implemented as a **placeholder** for future, more sophisticated recommendation algorithms. It does not perform any dynamic analysis of the user's past courses or other data to generate recommendations.

Instead, it returns a **hardcoded list of courses** categorized into three priority groups. This implementation serves as a structural foundation for the recommendation feature, allowing the front-end (e.g., `recommend.html`) to display recommendations in the desired format.

**Hardcoded Recommendation Structure:**

The method returns a `Map<String, List<Course>>` where the keys are descriptive strings for each priority group, and the values are lists of `Course` objects.

*   **1순위 추천과목 (7학기 빈도수 가장 높은 과목):**
    *   계산수학
    *   푸리에
    *   알바트로스세미나

*   **2순위 추천과목 (7학기 빈도수가 2위인 과목):**
    *   선형대수학 (여름학기 수강)

*   **3순위 추천과목 (6,8 학기에 들은 과목):**
    *   영어글로벌의사소통I
    *   자연계글쓰기

**Future Development:**

This placeholder logic is intended to be replaced with a more advanced recommendation algorithm that considers factors such as:

*   User's past course history and grades.
*   Course prerequisites and dependencies.
*   Popularity of courses among similar users.
*   User preferences or declared interests.
*   Academic progress and graduation requirements.
