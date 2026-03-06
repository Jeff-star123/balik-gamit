package com.bsit.lostandfound;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Controller
public class ItemController {

    @Autowired
    private ItemRepository repository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private JavaMailSender mailSender;

    

    // FIELDS FOR DEVELOPERS
    private static List<Developer> developerList = new ArrayList<>();
    private String bannerUrl = "/images/default-banner.png";
    private final String UPLOAD_DIR = System.getProperty("user.dir") + "/src/main/resources/static/uploads/";

    // SINGLE MERGED INIT METHOD
    @EventListener(ContextRefreshedEvent.class)
    public void init() {
        if (studentRepository.count() == 0) {
            studentRepository.save(new Student("2023-0001", "password123", "Test Student", "test@student.edu", false));
            studentRepository.save(new Student("ADMIN-01", "09062063128", "System Admin", "ssob.jeff13@gmail.com", true));
        }

        if (developerList.isEmpty()) {
            for (int i = 0; i < 5; i++) {
                developerList.add(new Developer("Dev Name " + (i+1), "Contributions", "Section", "/images/default-avatar.png"));
            }
        }
    }

    // --- HELPER METHODS (Fixed Duplicates) ---
    private String handleFileUpload(MultipartFile file) throws IOException {
        if (file.isEmpty()) return "default.jpg";
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path path = Paths.get(UPLOAD_DIR + fileName);
        Files.createDirectories(path.getParent());
        Files.write(path, file.getBytes());
        return fileName;
    }

    @GetMapping("/check-id")
    @ResponseBody
    public boolean checkId(@RequestParam String studentId) {
        return !studentRepository.existsById(studentId);
    }

    // --- LOGIN / LOGOUT ---
    @GetMapping("/login")
    public String loginPage() { return "login"; }

    @PostMapping("/login")
    public String login(@RequestParam String studentId, @RequestParam String password, HttpSession session, Model model) {
        return studentRepository.findById(studentId)
                .filter(s -> s.getPassword().equals(password))
                .map(s -> {
                    session.setAttribute("loggedInStudent", s);
                    return "redirect:/";
                })
                .orElseGet(() -> {
                    model.addAttribute("error", "Invalid Student ID or Password"); 
                    return "login";
                });
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    // --- DASHBOARD & PROFILE ---
    @GetMapping({"/", "/dashboard"})
    public String showFeed(Model model, HttpSession session) {
        Student loggedIn = (Student) session.getAttribute("loggedInStudent");
        if (loggedIn == null) return "redirect:/login";

        List<LostItem> activeItems = repository.findByIsReturnedFalse();
        model.addAttribute("items", activeItems);
        model.addAttribute("student", loggedIn);
        return "index";
    }

    @GetMapping("/profile")
    public String showProfile(HttpSession session, Model model) {
        Student loggedIn = (Student) session.getAttribute("loggedInStudent");
        if (loggedIn == null) return "redirect:/login";

        List<LostItem> myFinishedItems = repository.findAll().stream()
                .filter(item -> item.isReturned() && 
                        item.getPoster().getStudentId().equals(loggedIn.getStudentId()))
                .collect(Collectors.toList());

        model.addAttribute("myFinishedItems", myFinishedItems);
        model.addAttribute("student", loggedIn);
        return "profile";
    }

    // --- DEVELOPERS PAGE ---
    @GetMapping("/developers")
    public String showDevelopers(Model model, HttpSession session) {
        Student user = (Student) session.getAttribute("loggedInStudent");
        boolean isAdmin = (user != null && user.isIsAdmin()); 
        
        model.addAttribute("developers", developerList);
        model.addAttribute("bannerUrl", bannerUrl);
        model.addAttribute("isAdmin", isAdmin);
        return "developers";
    }

    @PostMapping("/developers/update")
    public String updateDeveloper(@RequestParam("devIndex") int index,
                                @RequestParam(value="name", required=false) String name,
                                @RequestParam(value="contributions", required=false) String contributions,
                                @RequestParam(value="section", required=false) String section,
                                @RequestParam("photo") MultipartFile photo,
                                HttpSession session) throws IOException {
        
        Student user = (Student) session.getAttribute("loggedInStudent");
        if (user == null || !user.isIsAdmin()) {
            return "redirect:/developers?error=unauthorized";
        }

        String fileName = photo.isEmpty() ? null : handleFileUpload(photo);

        if (index == -1) { 
            if (fileName != null) bannerUrl = "/uploads/" + fileName;
        } else if (index >= 0 && index < developerList.size()) { 
            Developer dev = developerList.get(index);
            if (name != null && !name.isEmpty()) dev.setName(name);
            if (contributions != null && !contributions.isEmpty()) dev.setContributions(contributions);
            if (section != null && !section.isEmpty()) dev.setSection(section);
            if (fileName != null) dev.setPhotoUrl("/uploads/" + fileName);
        }
        return "redirect:/developers?success=true";
    }

    // --- SETTINGS LOGIC ---
    @GetMapping("/settings")
    public String showSettings(HttpSession session, Model model) {
        Student loggedIn = (Student) session.getAttribute("loggedInStudent");
        if (loggedIn == null) return "redirect:/login";
        model.addAttribute("student", loggedIn);
        return "settings";
    }

    @PostMapping("/settings/request-otp")
    @ResponseBody
    public String requestSettingsOtp(@RequestParam String action, 
                                    @RequestParam String verifyValue, // Password
                                    @RequestParam(required = false) String emailCheck, 
                                    HttpSession session) {
        Student loggedIn = (Student) session.getAttribute("loggedInStudent");
        if (loggedIn == null) return "Unauthorized";

        // Standardized Security Check: Verify BOTH Password and Email for any major change
        if (action.equals("DELETE_ACCOUNT") || action.equals("CHANGE_EMAIL") || action.equals("CHANGE_PASSWORD")) {
            if (!loggedIn.getEmail().equalsIgnoreCase(emailCheck)) {
                return "The email provided does not match your account.";
            }
            if (!loggedIn.getPassword().equals(verifyValue)) {
                return "Incorrect password.";
            }
        }

        // If checks pass, generate and send OTP
        String otp = String.valueOf(new Random().nextInt(900000) + 100000);
        session.setAttribute("settingsOtp", otp);
        session.setAttribute("pendingAction", action);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(loggedIn.getEmail());
        message.setSubject("Security Verification Code - BALIK GAMIT");
        message.setText("You requested a " + action.replace("_", " ") + ". Your verification code is: " + otp);
        mailSender.send(message);

        return "OTP Sent to " + loggedIn.getEmail();
    }

    @PostMapping("/settings/verify-and-update")
    public String verifySettingsAction(@RequestParam String userOtp, 
                                       @RequestParam(required = false) String newDetail, 
                                       HttpSession session, Model model) {
        String sessionOtp = (String) session.getAttribute("settingsOtp");
        String action = (String) session.getAttribute("pendingAction");
        Student loggedIn = (Student) session.getAttribute("loggedInStudent");

        if (sessionOtp == null || !sessionOtp.equals(userOtp)) {
            model.addAttribute("error", "Invalid OTP code.");
            model.addAttribute("student", loggedIn);
            return "settings";
        }

        if ("CHANGE_PASSWORD".equals(action)) {
            studentRepository.findById(loggedIn.getStudentId()).ifPresent(s -> {
                s.setPassword(newDetail);
                studentRepository.save(s);
            });
        } else if ("CHANGE_EMAIL".equals(action)) {
            studentRepository.findById(loggedIn.getStudentId()).ifPresent(s -> {
                s.setEmail(newDetail);
                studentRepository.save(s);
                session.setAttribute("loggedInStudent", s);
            });
        } else if ("DELETE_ACCOUNT".equals(action)) {
            List<LostItem> userItems = repository.findAll().stream()
                    .filter(i -> i.getPoster().getStudentId().equals(loggedIn.getStudentId()))
                    .collect(Collectors.toList());
            repository.deleteAll(userItems);
            studentRepository.deleteById(loggedIn.getStudentId());
            session.invalidate();
            return "redirect:/login?deleted";
        }

        session.removeAttribute("settingsOtp");
        session.removeAttribute("pendingAction");
        return "redirect:/settings?success";
    }

    // --- REGISTRATION LOGIC ---
    @GetMapping("/register")
    public String showRegisterPage() { return "register"; }

    @PostMapping("/register/send-otp")
    public String completeRegistration(@RequestParam String studentId, @RequestParam String name, 
                                    @RequestParam String email, @RequestParam String password,
                                    @RequestParam String userOtp, HttpSession session, Model model) {
        String sessionOtp = (String) session.getAttribute("regOtp");
        if (sessionOtp == null || !sessionOtp.equals(userOtp)) {
            model.addAttribute("error", "Invalid or expired OTP code.");
            return "register";
        }
        if (studentRepository.existsById(studentId)) {
            model.addAttribute("error", "Student ID already exists!");
            return "register";
        }
        studentRepository.save(new Student(studentId, password, name, email, false));
        session.removeAttribute("regOtp");
        return "redirect:/login?registered=true"; 
    }

    @PostMapping("/send-otp")
    @ResponseBody
    public String sendSimpleOtp(@RequestParam String email, HttpSession session) {
        String otp = String.valueOf(new Random().nextInt(900000) + 100000);
        session.setAttribute("regOtp", otp);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Registration OTP");
        message.setText("Your code is: " + otp);
        mailSender.send(message);
        return "OK";
    }

    @PostMapping("/register/verify")
    public String verifyRegistration(@RequestParam String userOtp, HttpSession session, Model model) {
        String sessionOtp = (String) session.getAttribute("regOtp");
        Student pendingStudent = (Student) session.getAttribute("regData");
        if (sessionOtp != null && sessionOtp.equals(userOtp)) {
            studentRepository.save(pendingStudent);
            session.removeAttribute("regOtp");
            session.removeAttribute("regData");
            return "redirect:/login?registered=true";
        }
        model.addAttribute("error", "Invalid OTP. Please try again.");
        return "verify-registration";
    }

    // --- ITEM REPORTING & ACTIONS ---
    @PostMapping("/report")
    public String reportFoundItem(@RequestParam String itemName, @RequestParam String description, 
                                  @RequestParam String contactInfo, @RequestParam String category,
                                  @RequestParam("imageFile") MultipartFile file, HttpSession session) throws IOException {
        Student loggedIn = (Student) session.getAttribute("loggedInStudent");
        if (loggedIn == null) return "redirect:/login";
        String fileName = handleFileUpload(file);
        LostItem item = new LostItem(itemName, description, "/uploads/" + fileName, contactInfo, category, loggedIn);
        item.setStatus("FOUND"); 
        repository.save(item);
        return "redirect:/";
    }

    @PostMapping("/report-lost")
    public String reportLostItem(@RequestParam String itemName, @RequestParam String description, 
                                 @RequestParam String contactInfo, @RequestParam String category,
                                 @RequestParam("imageFile") MultipartFile file, HttpSession session) throws IOException {
        Student loggedIn = (Student) session.getAttribute("loggedInStudent");
        if (loggedIn == null) return "redirect:/login";
        String fileName = handleFileUpload(file);
        LostItem item = new LostItem(itemName, description, "/uploads/" + fileName, contactInfo, category, loggedIn);
        item.setStatus("LOST"); 
        repository.save(item);
        return "redirect:/";
    }

    @PostMapping("/finish/{id}")
    public String finishItem(@PathVariable Long id) {
        repository.findById(id).ifPresent(item -> {
            item.setReturned(true);
            repository.save(item);
        });
        return "redirect:/";
    }

    @PostMapping("/delete/{id}")
    public String deleteItem(@PathVariable Long id, HttpServletRequest request) {
        repository.deleteById(id);
        String referer = request.getHeader("Referer");
        if (referer != null && referer.contains("/profile")) return "redirect:/profile";
        return "redirect:/";
    }

    @PostMapping("/restore/{id}")
    public String restoreItem(@PathVariable Long id) {
        repository.findById(id).ifPresent(item -> {
            item.setReturned(false);
            repository.save(item);
        });
        return "redirect:/profile";
    }

    // --- FORGOT PASSWORD ---
    @GetMapping("/forgot-password")
    public String showForgotPassword() { return "forgot-password"; }

    @PostMapping("/forgot-password/send-otp")
    public String sendResetOtp(@RequestParam String email, HttpSession session, Model model) {
        return studentRepository.findByEmail(email).map(student -> {
            String otp = String.valueOf(new Random().nextInt(900000) + 100000);
            session.setAttribute("resetOtp", otp);
            session.setAttribute("resetStudentId", student.getStudentId());
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(student.getEmail());
            message.setSubject("Password Reset - BALIK GAMIT");
            message.setText("Your reset code is: " + otp);
            mailSender.send(message);
            return "redirect:/forgot-password/verify-otp"; 
        }).orElseGet(() -> {
            model.addAttribute("error", "No account found with that email address!");
            return "forgot-password";
        });
    }

    @GetMapping("/forgot-password/verify-otp")
    public String showVerifyOtpPage() { return "verify-otp"; }

    @PostMapping("/forgot-password/confirm-otp")
    public String confirmOtp(@RequestParam String userOtp, HttpSession session, Model model) {
        String sessionOtp = (String) session.getAttribute("resetOtp");
        if (sessionOtp != null && sessionOtp.equals(userOtp)) return "redirect:/forgot-password/set-new-password"; 
        model.addAttribute("error", "Invalid OTP code. Please try again.");
        return "verify-otp";
    }

    @GetMapping("/forgot-password/set-new-password")
    public String showSetNewPasswordPage() { return "verify-reset"; }

    @PostMapping("/forgot-password/verify")
    public String finishPasswordReset(@RequestParam String newPassword, HttpSession session) {
        String studentId = (String) session.getAttribute("resetStudentId");
        if (studentId != null) {
            studentRepository.findById(studentId).ifPresent(student -> {
                student.setPassword(newPassword);
                studentRepository.save(student);
            });
            session.invalidate(); 
            return "redirect:/login?resetSuccess";
        }
        return "redirect:/forgot-password";
    }

    @PostMapping("/forgot-password/send-otp-resend")
    @ResponseBody
    public String resendResetOtp(HttpSession session) {
        String studentId = (String) session.getAttribute("resetStudentId");
        if (studentId != null) {
            return studentRepository.findById(studentId).map(student -> {
                String otp = String.valueOf(new Random().nextInt(900000) + 100000);
                session.setAttribute("resetOtp", otp);
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(student.getEmail());
                message.setSubject("Resend: Password Reset - BALIK GAMIT");
                message.setText("Your new reset code is: " + otp);
                mailSender.send(message);
                return "OTP Sent!";
            }).orElse("Error");
        }
        return "Session Expired";
    }
}