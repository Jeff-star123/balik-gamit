package com.bsit.lostandfound;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.bsit.lostandfound.service.CloudinaryService;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.Optional;

@Controller
public class ItemController {

    @Autowired
    private ItemRepository repository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private DeveloperRepository devRepo;

    @Autowired 
    private CloudinaryService cloudinaryService;

    // Use the Resend service instead of JavaMailSender
    @Autowired
    private com.bsit.lostandfound.service.OtpService otpService;

    // TOGGLE FEATURE: Read from application.properties
    @Value("${app.feature.email-verification:true}")
    private boolean emailEnabled;

    // FIELDS FOR DEVELOPERS
    private static List<Developer> developerList = new ArrayList<>();
    private String bannerUrl = "/images/default-banner.png";


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
        if (devRepo.count() == 0) {
            // Standard 5 developers
            for (int i = 1; i <= 5; i++) {
                devRepo.save(new Developer("Dev Name " + i, "Developer Role", "BSIT-3A", "/images/default-avatar.png"));
            }
            // The Lead/Main Developer (The bigger one)
            devRepo.save(new Developer("Lead Developer Name", "Project Lead & System Architect", "LEAD", "/images/lead-avatar.png"));
        }
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
        
        // 1. Get the global thanks from the servlet context
        String savedThanks = (String) session.getServletContext().getAttribute("globalSpecialThanks");
        
        // 2. Add everything to the model
        model.addAttribute("developers", devRepo.findAll());
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("bannerUrl", this.bannerUrl); // Use the variable from the controller
        model.addAttribute("specialThanks", savedThanks); // This makes it show up in the HTML
        
        return "developers";
    }

    @PostMapping("/developers/update")
    public String updateDeveloper(
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "contributions", required = false) String contributions,
            @RequestParam(value = "section", required = false) String section,
            @RequestParam(value = "specialThanks", required = false) String specialThanks,
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            @RequestParam(value = "devIndex", required = false) Integer devIndex,
            HttpSession session) throws IOException {

        // 1. Check Admin Permission
        Student user = (Student) session.getAttribute("loggedInStudent");
        if (user == null || !user.isIsAdmin()) return "redirect:/login";

        // 2. Handle Banner Update
        if (devIndex != null && devIndex == -1 && photo != null && !photo.isEmpty()) {
            this.bannerUrl = cloudinaryService.uploadImage(photo); // NEW: Full URL
        }

        // 3. Handle Special Thanks Update (devIndex -2 is your Thanks)
        if (specialThanks != null && !specialThanks.trim().isEmpty()) {
            // Since you don't have a Settings table, we store it in the session 
            // so it persists during this session.
            session.getServletContext().setAttribute("globalSpecialThanks", specialThanks);
        }

        // 4. Handle Developer Updates
        if (id != null) {
            Developer dev = devRepo.findById(id).orElse(null);
            if (dev != null) {
                // ... (setting name, etc)
                
                if (photo != null && !photo.isEmpty()) {
                    String imageUrl = cloudinaryService.uploadImage(photo); // NEW: Full URL
                    dev.setPhotoUrl(imageUrl); // NEW: Save full URL
                }
                devRepo.save(dev); 
            }
        }

        return "redirect:/developers";
    }

    @PostMapping("/developers/edit")
    public String editDeveloper(@ModelAttribute Developer dev) {
        // This saves the changes (including the new photo URL) to the database
        devRepo.save(dev); 
        return "redirect:/developers";
    }

    // --- FB SHARE VIEW ---
    @GetMapping("/item/{id}")
    public String viewItem(@PathVariable("id") Long id, Model model) {
        // Fixed: changed 'itemRepo' to 'repository' to match your @Autowired field
        return repository.findById(id).map(item -> {
            model.addAttribute("item", item);
            return "view-item";
        }).orElse("redirect:/"); 
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

        if (action.equals("DELETE_ACCOUNT") || action.equals("CHANGE_EMAIL") || action.equals("CHANGE_PASSWORD")) {
            if (!loggedIn.getEmail().equalsIgnoreCase(emailCheck)) {
                return "The email provided does not match your account.";
            }
            if (!loggedIn.getPassword().equals(verifyValue)) {
                return "Incorrect password.";
            }
        }

        String otp = String.valueOf(new Random().nextInt(900000) + 100000);
        session.setAttribute("settingsOtp", otp);
        session.setAttribute("pendingAction", action);

        // Updated to use Resend
        otpService.sendOtp(loggedIn.getEmail(), otp);

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
        if (!emailEnabled) {
            if (studentRepository.existsById(studentId)) {
                model.addAttribute("error", "Student ID already exists!");
                return "register";
            }
            studentRepository.save(new Student(studentId, password, name, email, false));
            return "redirect:/login?registered=true";
        }

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
        if (!emailEnabled) return "SKIP"; // Stop here if disabled

        String otp = String.valueOf(new Random().nextInt(900000) + 100000);
        session.setAttribute("regOtp", otp);
        otpService.sendOtp(email, otp);
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
        String imageUrl = cloudinaryService.uploadImage(file); // Uses Cloudinary
        LostItem item = new LostItem(itemName, description, imageUrl, contactInfo, category, loggedIn);
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
        String imageUrl = cloudinaryService.uploadImage(file); // Uses Cloudinary
        LostItem item = new LostItem(itemName, description, imageUrl, contactInfo, category, loggedIn);
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
            
            // Updated to use Resend exclusively
            otpService.sendOtp(student.getEmail(), otp);
            
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
                
                // Updated to use Resend
                otpService.sendOtp(student.getEmail(), otp);
                
                return "OTP Sent!";
            }).orElse("Error");
        }
        return "Session Expired";
    }
}