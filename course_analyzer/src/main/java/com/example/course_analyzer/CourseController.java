package com.example.course_analyzer;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @GetMapping("/")
    public String index() {
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

        List<SemesterCourse> savedCourses = semesterCourseRepository.findByUser(user);

        if (savedCourses.isEmpty()) {
            return "redirect:/upload-form";
        }

        Map<Double, List<Course>> coursesBySemester = savedCourses.stream()
                .collect(Collectors.groupingBy(SemesterCourse::getSemester,
                        LinkedHashMap::new,
                        Collectors.mapping(sc -> {
                            String courseCode = sc.getCourseCode();
                            String actualCourseName = courseMappingRepository.findById(courseCode)
                                    .map(CourseMapping::getCourseName)
                                    .orElse(courseCode);
                            return new Course(String.valueOf(sc.getSemester()), courseCode, actualCourseName, sc.getGrade());
                        }, Collectors.toList())));

        model.addAttribute("coursesBySemester", coursesBySemester);
        return "results";
    }

    @GetMapping("/upload-form")
    public String showUploadForm() {
        return "upload-file";
    }

    @GetMapping("/recommend")
    public String recommend(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = getUserFromAuthentication(authentication);
        System.out.println("Generating recommendations for user: " + user.getUsername());
        Map<String, List<CourseStatDto>> recommendedCoursesMap = courseService.recommendCourses(user);
        System.out.println("Recommended courses map: " + recommendedCoursesMap);
        List<String> plannedCourseCodes = courseService.getPlanCourseCodes(user.getUsername());
        System.out.println("Planned course codes: " + plannedCourseCodes);
        model.addAttribute("title", "과목 추천");
        model.addAttribute("recommendedCoursesMap", recommendedCoursesMap);
        model.addAttribute("plannedCourseCodes", plannedCourseCodes);
        return "recommend";
    }

    @GetMapping("/all-courses")
    public String showAllCourses(@RequestParam(value = "major", required = false) String major, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = getUserFromAuthentication(authentication);

        List<String> userMajors = new ArrayList<>();
        if (user.getMajor1() != null && !user.getMajor1().isEmpty()) userMajors.add(user.getMajor1());
        if (user.getMajor2() != null && !user.getMajor2().isEmpty()) userMajors.add(user.getMajor2());
        if (user.getMajor3() != null && !user.getMajor3().isEmpty()) userMajors.add(user.getMajor3());
        model.addAttribute("userMajors", userMajors);

        List<String> plannedCourseCodes = courseService.getPlanCourseCodes(user.getUsername());
        model.addAttribute("plannedCourseCodes", plannedCourseCodes);

        if ("NonMajor".equals(major)) { // New condition for "비전공"
            List<CourseMapping> nonMajorCourseMappings = courseService.getNonMajorCourses(user);
            List<CourseStatDto> courses = nonMajorCourseMappings.stream()
                    .map(course -> new CourseStatDto(
                            course.getCourseCode(),
                            course.getCourseName(),
                            semesterCourseRepository.countDistinctUsersByCourseCode(course.getCourseCode())
                    ))
                    .collect(Collectors.toList());
            model.addAttribute("courses", courses);
            model.addAttribute("selectedMajor", "NonMajor");
        } else if (major != null && !major.isEmpty() && !"All".equals(major)) { // Existing major filtering logic
            String majorPrefix = courseService.getCoursePrefixForMajor(major);
            List<CourseStatDto> courses = courseService.getCoursesByMajor(majorPrefix);
            model.addAttribute("courses", courses);
            model.addAttribute("selectedMajor", major);
        } else { // Default case when major is "All" or null/empty
            List<CourseMapping> allCourseMappings = courseService.getAllCourses();
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

    @GetMapping("/semester-plan")
    public String showSemesterPlan(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = getUserFromAuthentication(authentication);
        List<CourseMapping> plannedCourses = courseService.getSemesterPlan(user.getUsername());
        model.addAttribute("plannedCourses", plannedCourses);
        return "semester-plan";
    }

    @PostMapping("/api/plan/add/{courseCode}")
    public ResponseEntity<String> addCourseToPlan(@PathVariable String courseCode) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = getUserFromAuthentication(authentication);
        try {
            courseService.addCourseToPlan(user.getUsername(), courseCode);
            return ResponseEntity.ok("Course added to plan successfully!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error adding course to plan.");
        }
    }

    @PostMapping("/api/plan/remove/{courseCode}")
    public ResponseEntity<String> removeCourseFromPlan(@PathVariable String courseCode) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = getUserFromAuthentication(authentication);
        try {
            courseService.removeCourseFromPlan(user.getUsername(), courseCode);
            return ResponseEntity.ok("Course removed from plan successfully!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error removing course from plan.");
        }
    }
}
