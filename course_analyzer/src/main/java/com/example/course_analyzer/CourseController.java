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

import java.io.IOException;
import java.util.ArrayList;
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

        // 1. Fetch ALL courses from the database for the user
        List<SemesterCourse> allUserCourses = semesterCourseRepository.findByUser(user);

        // 2. Separate taken courses from cart items
        List<SemesterCourse> takenCourses = allUserCourses.stream()
                .filter(c -> c.getSemester() > 0)
                .collect(Collectors.toList());
        
        List<SemesterCourse> cartItems = allUserCourses.stream()
                .filter(c -> c.getSemester() == 0)
                .collect(Collectors.toList());

        // 3. Prepare taken courses for display (grouped by semester)
        List<String> takenCourseCodes = takenCourses.stream()
                .map(SemesterCourse::getCourseCode)
                .collect(Collectors.toList());
        model.addAttribute("takenCourseCodes", takenCourseCodes);

        Map<Double, List<Course>> coursesBySemesterNumber = takenCourses.stream()
                .collect(Collectors.groupingBy(SemesterCourse::getSemester,
                        java.util.TreeMap::new,
                        Collectors.mapping(sc -> {
                            String courseCode = sc.getCourseCode();
                            String actualCourseName = courseMappingRepository.findById(courseCode)
                                    .map(CourseMapping::getCourseName)
                                    .orElse(courseCode);
                            return new Course(String.valueOf(sc.getSemester()), courseCode, actualCourseName, sc.getGrade());
                        }, Collectors.toList())));

        Map<String, List<Course>> coursesForModel = new java.util.LinkedHashMap<>();
        coursesBySemesterNumber.forEach((semester, courses) -> {
            String semesterKey = (semester % 1 == 0)
                ? String.format("%.0f학기", semester)
                : String.format("%.1f학기", semester);
            coursesForModel.put(semesterKey, courses);
        });

        // 4. Prepare cart items for display
        double lastSemester = coursesBySemesterNumber.isEmpty() ? 0.0 : ((java.util.TreeMap<Double, List<Course>>) coursesBySemesterNumber).lastKey();
        double nextSemesterNum = Math.floor(lastSemester) + 1;
        String cartSemesterName = String.format("%.0f학기 (장바구니)", nextSemesterNum);

        List<Course> cartCoursesForModel = cartItems.stream()
                .map(sc -> {
                    String courseName = courseMappingRepository.findById(sc.getCourseCode())
                            .map(CourseMapping::getCourseName)
                            .orElse(sc.getCourseCode());
                    return new Course(null, sc.getCourseCode(), courseName, "담은 과목");
                })
                .collect(Collectors.toList());

        coursesForModel.put(cartSemesterName, cartCoursesForModel);

        model.addAttribute("coursesBySemester", coursesForModel);
        return "results";
    }

    @GetMapping("/upload-form")
    public String showUploadForm() {
        return "upload-file";
    }

    @GetMapping("/recommend")
    public String showRecommendPage(Model model) {
        // Return the initial page structure. The content will be loaded dynamically.
        model.addAttribute("title", "과목 추천");
        // Provide empty maps to prevent errors on initial render
        model.addAttribute("recommendedCoursesMap", Map.of("major", new ArrayList<>(), "ge", new ArrayList<>()));
        return "recommend";
    }

    @PostMapping("/recommend")
    public String recommend(@RequestBody RecommendationRequestDto requestDto, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = getUserFromAuthentication(authentication);
        
        List<String> cartCourseCodes = requestDto.getCartCourseCodes() == null ? new ArrayList<>() : requestDto.getCartCourseCodes();
        List<String> dismissedCourseCodes = requestDto.getDismissedCourseCodes() == null ? new ArrayList<>() : requestDto.getDismissedCourseCodes();

        Map<String, List<RecommendedCourseDto>> recommendedCoursesMap = courseService.recommendCourses(user, cartCourseCodes, dismissedCourseCodes);
        
        model.addAttribute("title", "과목 추천");
        model.addAttribute("recommendedCoursesMap", recommendedCoursesMap);
        
        return "recommend :: #recommendation-results";
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

    @GetMapping("/api/cart-courses")
    @ResponseBody
    public ResponseEntity<List<CartCourseDto>> getCartCourses(Authentication authentication) {
        User user = getUserFromAuthentication(authentication);
        List<CartCourseDto> cartCourses = courseService.getCartCourses(user.getUsername());
        return ResponseEntity.ok(cartCourses);
    }

    @PostMapping("/api/cart-courses/{courseCode}")
    @ResponseBody
    public ResponseEntity<?> addCourseToCart(@PathVariable String courseCode, @RequestBody Map<String, String> payload, Authentication authentication) {
        User user = getUserFromAuthentication(authentication);
        String courseName = payload.get("courseName");
        try {
            SemesterCourse cartItem = courseService.addCourseToCart(user.getUsername(), courseCode, courseName);
            // To avoid circular references, we return a DTO
            courseMappingRepository.findById(cartItem.getCourseCode()).ifPresent(courseMapping -> {
                CartCourseDto cartCourseDto = new CartCourseDto(cartItem.getCourseCode(), courseMapping.getCourseName());
            });
            return ResponseEntity.status(HttpStatus.CREATED).body(new CartCourseDto(cartItem.getCourseCode(), courseName));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @DeleteMapping("/api/cart-courses/{courseCode}")
    @ResponseBody
    public ResponseEntity<?> removeCourseFromCart(@PathVariable String courseCode, Authentication authentication) {
        User user = getUserFromAuthentication(authentication);
        try {
            courseService.removeCourseFromCart(user.getUsername(), courseCode);
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
}
