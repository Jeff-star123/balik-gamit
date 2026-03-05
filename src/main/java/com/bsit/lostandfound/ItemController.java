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
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Random;

@Controller
public class ItemController {

    @Autowired
    private ItemRepository repository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private JavaMailSender mailSender;

    private final String UPLOAD_DIR = System.getProperty("user.dir") + "/src/main/resources/static/uploads/";

    // --- INITIALIZATION (CREATING ADMIN) ---
    @EventListener(ContextRefreshedEvent.class)
    public void init() {
        if (studentRepository.count() == 0) {
            studentRepository.save(new Student("2023-0001", "password123", "Test Student", "test@student.edu", false));
            studentRepository.save(new Student("ADMIN-01", "admin123", "System Admin", "admin@balikgamit.com", true));
            System.out.println("ACCOUNTS CREATED: ADMIN-01 and 2023-0001 with emails.");
        }
    }

    // --- AUTHENTICATION ---

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

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

    // --- REGISTRATION WITH OTP ---

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/send-otp")
    @ResponseBody
    public String sendOtp(@RequestParam String email, HttpSession session) {
        String otp = String.valueOf(new Random().nextInt(900000) + 100000);
        session.setAttribute("generatedOtp", otp);
        session.setAttribute("tempEmail", email);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Your BALIK GAMIT Verification Code");
        message.setText("Your verification code is: " + otp);
        mailSender.send(message);

        return "OTP Sent!";
    }

    @PostMapping("/register")
    public String register(@RequestParam String studentId, @RequestParam String name, @RequestParam String email,
                           @RequestParam String password, @RequestParam String userOtp, HttpSession session, Model model) {
        
        if (studentRepository.findByEmail(email).isPresent()) {
            model.addAttribute("error", "This email is already registered!");
            return "register";
        }

        String sessionOtp = (String) session.getAttribute("generatedOtp");
        if (sessionOtp != null && sessionOtp.equals(userOtp)) {
            studentRepository.save(new Student(studentId, password, name, email, false));
            session.removeAttribute("generatedOtp");
            return "redirect:/login";
        } else {
            model.addAttribute("error", "Invalid OTP code.");
            return "register";
        }
    }

    // --- FEED & ITEM LOGIC ---

    @GetMapping("/")
    public String showFeed(Model model, HttpSession session, @RequestParam(required = false) String category) {
        Student loggedIn = (Student) session.getAttribute("loggedInStudent");
        if (loggedIn == null) return "redirect:/login";

        List<LostItem> activeItems = (category != null && !category.isEmpty()) 
            ? repository.findByIsReturnedFalseAndCategory(category) 
            : repository.findByIsReturnedFalse();

        model.addAttribute("items", activeItems);
        model.addAttribute("archivedItems", repository.findAll().stream().filter(LostItem::isReturned).toList());
        model.addAttribute("student", loggedIn);
        return "index";
    }

    @PostMapping("/report")
    public String reportItem(@RequestParam String itemName, @RequestParam String description, 
                             @RequestParam String contactInfo, @RequestParam String category,
                             @RequestParam("imageFile") MultipartFile file, HttpSession session) throws IOException {
        
        Student loggedIn = (Student) session.getAttribute("loggedInStudent");
        if (loggedIn == null) return "redirect:/login";

        String fileName = "default.jpg";
        if (!file.isEmpty()) {
            fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path path = Paths.get(UPLOAD_DIR + fileName);
            Files.createDirectories(path.getParent());
            Files.write(path, file.getBytes());
        }

        repository.save(new LostItem(itemName, description, "/uploads/" + fileName, contactInfo, category, loggedIn));
        return "redirect:/";
    }

    // --- FORGOT PASSWORD 3-STEP FLOW ---

    @GetMapping("/forgot-password")
    public String showForgotPassword() {
        return "forgot-password";
    }

    // Step 1: Send OTP and redirect to clean verify page
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

    // Step 2: Show the OTP-only verification page
    @GetMapping("/forgot-password/verify-otp")
    public String showVerifyOtpPage() {
        return "verify-otp"; 
    }

    // Step 2b: Handle OTP validation only
    @PostMapping("/forgot-password/confirm-otp")
    public String confirmOtp(@RequestParam String userOtp, HttpSession session, Model model) {
        String sessionOtp = (String) session.getAttribute("resetOtp");
        if (sessionOtp != null && sessionOtp.equals(userOtp)) {
            return "redirect:/forgot-password/set-new-password"; 
        }
        model.addAttribute("error", "Invalid OTP code. Please try again.");
        return "verify-otp";
    }

    // Step 3: Show the Password entry page
    @GetMapping("/forgot-password/set-new-password")
    public String showSetNewPasswordPage() {
        return "verify-reset"; 
    }

    // Step 3b: Save the new password to database
    @PostMapping("/forgot-password/verify")
    public String finishPasswordReset(@RequestParam String newPassword, HttpSession session, Model model) {
        String studentId = (String) session.getAttribute("resetStudentId");
        if (studentId != null) {
            studentRepository.findById(studentId).ifPresent(student -> {
                student.setPassword(newPassword);
                studentRepository.save(student);
            });
            session.removeAttribute("resetOtp");
            session.removeAttribute("resetStudentId");
            return "redirect:/login?resetSuccess"; // Redirect with success flag
        }
        return "redirect:/forgot-password";
    }

    // Helper: AJAX Resend for Forgot Password
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