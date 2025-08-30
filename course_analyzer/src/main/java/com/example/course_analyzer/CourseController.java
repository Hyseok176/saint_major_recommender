package com.example.course_analyzer;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;

@Controller
public class CourseController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SemesterCourseRepository semesterCourseRepository;

    @Autowired
    private CourseMappingRepository courseMappingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CrawlerService crawlerService;

    @GetMapping("/test-crawler")
    @ResponseBody
    public ResponseEntity<String> testCrawler(@RequestParam("year") String year, @RequestParam("semester") String semester) {
        try {
            String semesterInKorean;
            switch (semester) {
                case "1":
                    semesterInKorean = "1학기";
                    break;
                case "2":
                    semesterInKorean = "2학기";
                    break;
                case "summer":
                    semesterInKorean = "하계학기"; // 파이썬 스크립트의 기준에 맞춤
                    break;
                case "winter":
                    semesterInKorean = "동계학기"; // 파이썬 스크립트의 기준에 맞춤
                    break;
                default:
                    return ResponseEntity.badRequest().body("Invalid semester value: " + semester);
            }
            
            // 서비스에는 변환된 한글 학기를 전달
            crawlerService.runCrawlerImmediately(year, semesterInKorean);
            
            // 사용자 피드백은 영문/숫자 파라미터 기준
            return ResponseEntity.ok("Crawler execution started for " + year + " " + semester + ". Check server logs.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error during crawler execution: " + e.getMessage());
        }
    }

    @GetMapping("/crawler-admin")
    public String crawlerAdminPage() {
        return "crawler-admin";
    }

    @GetMapping("/api/crawled-files")
    @ResponseBody
    public ResponseEntity<List<String>> getCrawledFiles() {
        File dataDirectory = new File("course_analyzer/src/main/resources/data");
        if (!dataDirectory.exists() || !dataDirectory.isDirectory()) {
            return ResponseEntity.ok(new ArrayList<>());
        }
        
        String[] files = dataDirectory.list((dir, name) -> name.toLowerCase().endsWith(".csv"));
        
        List<String> fileList = (files != null) ? Arrays.asList(files) : new ArrayList<>();
        
        return ResponseEntity.ok(fileList);
    }

    @GetMapping("/")
    public String index(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            // If the user is already authenticated, redirect them to the results page.
            return "redirect:/results";
        }
        return "index";
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam("username") String username,
                             @RequestParam("password") String password,
                             @RequestParam(value = "nickname", required = false) String nickname,
                             @RequestParam(value = "email", required = false) String email,
                             RedirectAttributes redirectAttributes) {
        if (userRepository.findByUsername(username).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "이미 존재하는 사용자 ID입니다.");
            return "redirect:/";
        }
        User user = User.builder()
                .username(username)
                .nickname(nickname)
                .email(email)
                .password(passwordEncoder.encode(password))
                .build();
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("message", "회원가입이 완료되었습니다. 로그인해주세요.");
        return "redirect:/";
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file,
                             @RequestParam("major1") String major1,
                             @RequestParam("major2") String major2,
                             @RequestParam("major3") String major3,
                             Model model,
                             HttpServletRequest request,
                             RedirectAttributes redirectAttributes) { // Added RedirectAttributes
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = getUserFromAuthentication(authentication);
        String ipAddress = request.getRemoteAddr();
        try {
            courseService.updateUserTranscript(user, file, major1, major2, major3, ipAddress);
            return "redirect:/results";
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "파일 처리 중 오류가 발생했습니다. 파일 형식을 확인해주세요."); // Changed to RedirectAttributes
            return "redirect:/upload-form"; // Redirect to upload-form
        }
    }

    @GetMapping("/results")
    public String showResults(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = getUserFromAuthentication(authentication);

        // 1. Fetch and process completed courses
        List<SemesterCourse> takenCourses = semesterCourseRepository.findByUser(user).stream()
                .filter(c -> c.getSemester() > 0)
                .collect(Collectors.toList());

        Map<Double, List<Course>> coursesBySemesterNumber = takenCourses.stream()
                .collect(Collectors.groupingBy(SemesterCourse::getSemester,
                        java.util.TreeMap::new, // TreeMap automatically sorts by key (Double)
                        Collectors.mapping(sc -> {
                            String courseCode = sc.getCourseCode();
                            String actualCourseName = courseMappingRepository.findById(courseCode)
                                    .map(CourseMapping::getCourseName)
                                    .orElse(courseCode);
                            return new Course(String.valueOf(sc.getSemester()), courseCode, actualCourseName, sc.getGrade());
                        }, Collectors.toList())));

        // This map will hold the final, ordered list for the view
        Map<String, List<Course>> coursesForModel = new LinkedHashMap<>();

        // Add completed semesters to the final map, preserving their sorted order
        coursesBySemesterNumber.forEach((semester, courses) -> {
            String semesterKey = (semester % 1 == 0)
                ? String.format("%.0f학기", semester)
                : String.format("%.1f학기", semester);
            coursesForModel.put(semesterKey, courses);
        });

        // 2. Fetch and process planned courses (from cart)
        List<SavedCourse> savedCourses = courseService.getSavedCourses(user.getUsername());
        Map<String, List<SavedCourse>> savedCoursesBySemester = savedCourses.stream()
                .filter(sc -> sc.getTargetSemester() != null && !sc.getTargetSemester().isEmpty())
                .collect(Collectors.groupingBy(SavedCourse::getTargetSemester));

        // Sort the future semester keys alphabetically (e.g., "2025년 1학기", "2025년 2학기")
        List<String> sortedFutureSemesters = savedCoursesBySemester.keySet().stream().sorted().collect(Collectors.toList());

        // Add planned semesters to the final map in their sorted order
        for (String semester : sortedFutureSemesters) {
            String semesterKey = String.format("%s (계획)", semester);
            List<Course> courseViewModels = savedCoursesBySemester.get(semester).stream()
                    .map(sc -> new Course(null, sc.getCourseCode(), sc.getCourseName(), "담은 과목"))
                    .collect(Collectors.toList());
            coursesForModel.put(semesterKey, courseViewModels);
        }

        model.addAttribute("coursesBySemester", coursesForModel);
        model.addAttribute("savedCourses", savedCourses); // For the floating cart

        return "results";
    }

    @GetMapping("/upload-form")
    public String showUploadForm() {
        return "upload-file";
    }

    @GetMapping("/recommend")
    public String showRecommendPage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = getUserFromAuthentication(authentication);

        model.addAttribute("title", "과목 추천");
        model.addAttribute("recommendedCoursesMap", Map.of("major", new ArrayList<>(), "ge", new ArrayList<>()));
        model.addAllAttributes(generateFutureSemesterOptions(user));
        return "recommend";
    }

    @PostMapping("/recommend")
    public String recommend(@RequestBody RecommendationRequestDto requestDto, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = getUserFromAuthentication(authentication);
        
        List<String> cartCourseCodes = requestDto.getCartCourseCodes() == null ? new ArrayList<>() : requestDto.getCartCourseCodes();
        List<String> dismissedCourseCodes = requestDto.getDismissedCourseCodes() == null ? new ArrayList<>() : requestDto.getDismissedCourseCodes();
        Integer semester = requestDto.getSemester();

        Map<String, List<RecommendedCourseDto>> recommendedCoursesMap = courseService.recommendCourses(user, cartCourseCodes, dismissedCourseCodes, semester);
        
        model.addAttribute("title", "과목 추천");
        model.addAttribute("recommendedCoursesMap", recommendedCoursesMap);
        model.addAttribute("isEssentialTrackRecommendation", courseService.hasUncompletedTracks(user));
        
        return "recommend :: #recommendation-results";
    }

    @GetMapping("/all-courses")
    public String showAllCourses(@RequestParam(value = "major", required = false) String major,
                                 @RequestParam(value = "semester", required = false) Integer semester,
                                 Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = getUserFromAuthentication(authentication);

        List<String> userMajors = new ArrayList<>();
        if (user.getMajor1() != null && !user.getMajor1().isEmpty()) userMajors.add(user.getMajor1());
        if (user.getMajor2() != null && !user.getMajor2().isEmpty()) userMajors.add(user.getMajor2());
        if (user.getMajor3() != null && !user.getMajor3().isEmpty()) userMajors.add(user.getMajor3());
        model.addAttribute("userMajors", userMajors);

        if ("NonMajor".equals(major)) {
            List<CourseMapping> nonMajorCourseMappings = courseService.getNonMajorCourses(user, semester);
            List<CourseStatDto> courses = nonMajorCourseMappings.stream()
                    .map(course -> new CourseStatDto(
                            course.getCourseCode(),
                            course.getCourseName(),
                            semesterCourseRepository.countDistinctUsersByCourseCode(course.getCourseCode())
                    ))
                    .collect(Collectors.toList());
            model.addAttribute("courses", courses);
            model.addAttribute("selectedMajor", "NonMajor");
        } else if (major != null && !major.isEmpty() && !"All".equals(major)) {
            String majorPrefix = courseService.getCoursePrefixForMajor(major);
            List<CourseStatDto> courses = courseService.getCoursesByMajor(majorPrefix, semester);
            model.addAttribute("courses", courses);
            model.addAttribute("selectedMajor", major);
        } else {
            List<CourseMapping> allCourseMappings = courseService.getAllCourses(); // Note: getAllCourses is not filtered by semester yet. This might be a future improvement.
            List<CourseStatDto> courses = allCourseMappings.stream()
                    .map(course -> new CourseStatDto(
                            course.getCourseCode(),
                            course.getCourseName(),
                            semesterCourseRepository.countDistinctUsersByCourseCode(course.getCourseCode())
                    ))
                    .collect(Collectors.toList());
            model.addAttribute("courses", courses);
            model.addAttribute("selectedMajor", "All");
        }

        model.addAllAttributes(generateFutureSemesterOptions(user));
        model.addAttribute("selectedSemester", semester); // Pass the selected semester back to the view
        return "all-courses";
    }

    @GetMapping("/api/course-stats/{subjectCode}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCourseStats(@PathVariable String subjectCode) {
        try {
            Map<String, Object> stats = courseService.getCourseStats(subjectCode);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/extract-majors")
    @ResponseBody
    public ResponseEntity<List<String>> extractMajors(@RequestParam("file") MultipartFile file) {
        try {
            List<String> majors = courseService.extractMajorsFromFile(file.getInputStream());
            return ResponseEntity.ok(majors);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/api/saved-courses")
    @ResponseBody
    public ResponseEntity<List<SavedCourse>> getSavedCourses(Authentication authentication) {
        User user = getUserFromAuthentication(authentication);
        List<SavedCourse> savedCourses = courseService.getSavedCourses(user.getUsername());
        return ResponseEntity.ok(savedCourses);
    }

    @PostMapping("/api/saved-courses/{courseCode}")
    @ResponseBody
    public ResponseEntity<?> addSavedCourse(@PathVariable String courseCode, @RequestBody Map<String, String> payload, Authentication authentication) {
        User user = getUserFromAuthentication(authentication);
        String courseName = payload.get("courseName");
        String targetSemester = payload.get("targetSemester");
        try {
            SavedCourse savedCourse = courseService.addSavedCourse(user.getUsername(), courseCode, courseName, targetSemester);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedCourse);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @DeleteMapping("/api/saved-courses/{courseCode}")
    @ResponseBody
    public ResponseEntity<?> deleteSavedCourse(@PathVariable String courseCode, Authentication authentication) {
        User user = getUserFromAuthentication(authentication);
        try {
            courseService.deleteSavedCourse(user.getUsername(), courseCode);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    private User getUserFromAuthentication(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            String providerId = oauth2User.getName();
            return userRepository.findByProviderAndProviderId("kakao", providerId)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with providerId: " + providerId));
        } else {
            String username = authentication.getName();
            return userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
        }
    }

    private Map<String, Object> generateFutureSemesterOptions(User user) {
        int startYear;
        int startSemester;

        String lastSemester = user.getLastSemester(); // e.g., "2025-2"

        if (lastSemester != null && lastSemester.matches("\\d{4}-[12SW]")) {
            String[] parts = lastSemester.split("-");
            int lastYear = Integer.parseInt(parts[0]);
            String lastSem = parts[1];

            if (lastSem.equals("2") || lastSem.equals("W")) {
                startYear = lastYear + 1;
                startSemester = 1;
            } else { // "1" or "S"
                startYear = lastYear;
                startSemester = 2;
            }
        } else {
            // Fallback for new users or users without lastSemester data
            LocalDate today = LocalDate.now();
            startYear = today.getYear();
            int currentMonth = today.getMonthValue();
            startSemester = (currentMonth >= 3 && currentMonth <= 8) ? 2 : 1;
            if (startSemester == 1 && currentMonth > 8) {
                startYear++;
            }
        }

        List<Integer> years = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            years.add(startYear + i);
        }

        return Map.of("futureYears", years, "startSemester", startSemester);
    }
}
