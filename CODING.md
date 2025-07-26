# Gemini's Coding Process

This document outlines the thought process and steps I, Gemini, take when modifying the codebase.

## General Approach

1.  **Understand the Goal:** I first analyze the user's request to fully understand the desired outcome. This includes identifying the core feature to be added or modified, the expected user interaction, and any provided examples or constraints.

2.  **Analyze Existing Code:** Before writing any code, I examine the existing codebase to understand its structure, conventions, and patterns. This involves:
    *   **Reading relevant files:** I use `read_file` to inspect the contents of files that are likely to be affected by the changes.
    *   **Searching for keywords:** I use `search_file_content` to find relevant code snippets, such as function definitions, class declarations, or HTML elements.
    *   **Listing directories:** I use `list_directory` to understand the project's file structure.

3.  **Formulate a Plan:** Based on my understanding of the goal and the existing code, I create a step-by-step plan for implementing the changes. This plan typically involves:
    *   Modifying existing files (e.g., adding new methods, updating HTML).
    *   Creating new files (e.g., new HTML templates, new Java classes).
    *   Deleting files if necessary.

4.  **Execute the Plan:** I execute the plan by using the available tools, such as `write_file`, `replace`, and `run_shell_command`.

5.  **Verify the Changes:** After making changes, I encourage the user to verify them by running the application and testing the new functionality. If any issues arise, I will analyze the problem and attempt to fix it.

## My Thought Process for Handling User Requests

When I receive a request from a user, I follow a structured thinking process to ensure I provide an accurate and efficient solution. Here's a breakdown of my internal monologue and decision-making process:

1.  **Deconstruct the Request:** I first break down the user's request into smaller, manageable parts. I identify the key actions required (e.g., "change button behavior," "update database logic"), the specific files or components involved, and any constraints or preferences mentioned.

2.  **Information Gathering:** I then gather all the necessary information to understand the current state of the application. This involves a series of questions I ask myself:
    *   "Which files are relevant to this request?"
    *   "What is the current implementation of the feature I need to modify?"
    *   "Are there any existing conventions or patterns I need to follow?"
    *   "What are the potential side effects of the changes I'm about to make?"

3.  **Planning and Strategy:** Once I have a clear understanding of the request and the codebase, I formulate a plan of action. I think about the most efficient and robust way to implement the changes. This includes:
    *   **Choosing the right tools:** I decide which of my available tools (`read_file`, `write_file`, `replace`, etc.) are best suited for each step of the plan.
    *   **Considering alternatives:** I think about different ways to achieve the desired outcome and choose the one that is most consistent with the existing code and best practices.
    *   **Anticipating potential issues:** I try to foresee any potential problems that might arise from my changes and plan accordingly.

4.  **Execution and Self-Correction:** As I execute the plan, I constantly monitor the results of my actions. If a tool fails or produces an unexpected outcome, I analyze the error and adjust my plan. For example, if the `replace` tool fails, I will try to use `read_file` and `write_file` as a more robust alternative.

5.  **Communication and Verification:** Throughout the process, I keep the user informed of my progress. I explain what I'm doing and why, and I ask for clarification if anything is unclear. After I've implemented the changes, I ask the user to verify that everything is working as expected.

By following this structured approach, I can ensure that I provide high-quality, reliable, and maintainable solutions to the user's requests.

## Recent Confirmed Changes

This section summarizes the changes that have been recently implemented and confirmed.

### "다시 분석하기" Button Behavior and File Re-upload Logic

1.  **Goal:** Modify the "다시 분석하기" (Analyze Again) button on the results page to redirect to the file upload page (`upload-file.html`) instead of the login page. Additionally, implement logic to update existing course history when a file is re-uploaded, reflecting only the changed content.

2.  **Analysis:**
    *   Identified `results.html` as the file to modify for the button's `href` attribute.
    *   Identified `CourseController.java` to add a new GET mapping for `/upload-form` to serve `upload-file.html` with the `userId`.
    *   Identified `CourseService.java` and its `saveCoursesToDatabase` method as the place to implement the differential update logic for course history.

3.  **Plan:**
    *   Modify the `href` of the "다시 분석하기" button in `results.html` to `@{/upload-form(userId=${userId})}`.
    *   Add a new `@GetMapping("/upload-form")` method in `CourseController.java` that takes `userId` as a parameter and returns `upload-file`.
    *   Modify `CourseService.saveCoursesToDatabase` to:
        *   Fetch existing `SemesterCourse` entries for the user.
        *   Compare the new `coursesBySemester` with existing ones.
        *   Update existing entries or add new ones if a course is modified or new.
        *   Delete `SemesterCourse` entries that are no longer present in the new upload.

4.  **Execution:**
    *   Used `write_file` to update `results.html`.
    *   Used `write_file` to update `CourseController.java`.
    *   Used `write_file` to update `CourseService.java`.

### "다음학기 구성하기" Feature (Placeholder)

1.  **Goal:** Add a "다음학기 구성하기" (Configure Next Semester) button to the results page, which, when clicked, navigates to a new page (`recommend.html`) displaying a placeholder list of recommended courses. The recommendation algorithm itself is to be implemented later.

2.  **Analysis:**
    *   Identified `results.html` as the file to modify for adding the new button.
    *   Identified `CourseController.java` to add a new GET mapping for `/recommend` to serve `recommend.html`.
    *   Identified `CourseService.java` to add a placeholder `recommendCourses` method.
    *   Identified `C:\banghak\next_sem.html` as the template for the new `recommend.html`.

3.  **Plan:**
    *   Add a new button to `results.html` with `th:href="@{/recommend(userId=${userId})}"`.
    *   Add a new `@GetMapping("/recommend")` method in `CourseController.java` that takes `userId` and returns `recommend`.
    *   Add a `public Map<String, List<Course>> recommendCourses(User user)` method to `CourseService.java` that returns hardcoded course data.
    *   Create `C:\banghak\course_analyzer\src\main\resources\templates\recommend.html` based on `C:\banghak\next_sem.html`.

4.  **Execution:**
    *   Used `write_file` to update `results.html`.
    *   Used `write_file` to update `CourseController.java`.
    *   Used `write_file` to update `CourseService.java`.
    *   Used `write_file` to create `recommend.html`.

### JPA `javax` to `jakarta` Migration

1.  **Goal:** Resolve compilation errors related to `javax.persistence` not being found, which occurred after a `git reset --hard` operation.

2.  **Analysis:**
    *   The error logs indicated `cannot find symbol` for `javax.persistence` annotations in `CourseMapping.java`.
    *   Recognized that Spring Boot 3.x uses `jakarta.persistence` instead of `javax.persistence`.
    *   Confirmed that `CourseMapping.java` and `CourseMappingRepository.java` were missing or incorrect after the `git reset --hard`.

3.  **Plan:**
    *   Recreate `CourseMapping.java` with `jakarta.persistence` imports.
    *   Recreate `CourseMappingRepository.java`.

4.  **Execution:**
    *   Used `write_file` to create `CourseMapping.java` with the correct `jakarta` imports.
    *   Used `write_file` to create `CourseMappingRepository.java`.

### `recommend.html` Navigation Update

1.  **Goal:** Change the "처음으로" button on the `recommend.html` page to "이전으로" and make it navigate back to the previous page (`results.html`) instead of the login page.

2.  **Analysis:**
    *   Identified `recommend.html` as the file to modify.
    *   Noted that the current `th:href` was `@{/}` (login page).

3.  **Plan:**
    *   Change the button text from "처음으로" to "이전으로".
    *   Update the `th:href` attribute to `@{/results(userId=${userId})}` to navigate back to the results page with the user ID.

4.  **Execution:**
    *   Used `write_file` to update `recommend.html`.

### `CourseController.java` `double` Type Error Resolution

1.  **Goal:** Resolve the `double cannot be dereferenced` compilation error in `CourseController.java`.

2.  **Analysis:**
    *   The error occurred at `sc.getSemester().toString()` because `sc.getSemester()` returns a primitive `double`, which cannot have methods called on it directly.

3.  **Plan:**
    *   Replace `sc.getSemester().toString()` with `String.valueOf(sc.getSemester())`.

4.  **Execution:**
    *   Used `write_file` to update `CourseController.java`.

### Display Course Names Only on UI

1.  **Goal:** Ensure that only course names (not codes) are displayed on the analysis results page (`results.html`) and the next semester recommendation page (`recommend.html`), while keeping course codes stored in the `SEMESTER_COURSE` table in the database.

2.  **Analysis:**
    *   Confirmed that `SemesterCourse.java` already uses `courseCode` for storage.
    *   Confirmed that `CourseService.java`'s `saveCoursesToDatabase` correctly stores `courseCode`.
    *   Confirmed that `CourseController.java`'s `showResults` already retrieves `courseName` from `CourseMappingRepository` using `courseCode`.
    *   Confirmed that `recommend.html` already displays `courseName`.
    *   Identified that `results.html` was still displaying both `courseCode` and `courseName`.
    *   Identified that `CourseService.java`'s `recommendCourses` was creating `Course` objects with `courseCode` in the `courseCode` field and `courseName` in the `courseName` field, which is correct for the `Course` object structure, but the `results.html` was explicitly showing `courseCode`.

3.  **Plan:**
    *   Modify `results.html` to display only `th:text="${course.courseName}"`.
    *   Modify `CourseService.java`'s `recommendCourses` to pass an empty string for `courseCode` when creating `Course` objects, as the `Course` constructor expects `semester`, `courseCode`, `courseName`, `remark`.

4.  **Execution:**
    *   Used `write_file` to update `results.html`.
    *   Used `write_file` to update `CourseService.java`.