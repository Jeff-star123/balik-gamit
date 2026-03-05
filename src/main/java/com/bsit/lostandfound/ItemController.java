package com.bsit.lostandfound;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.file.*;
import java.security.Principal;
import java.util.List;

@Controller
public class ItemController {

    @Autowired
    private ItemRepository repository;

    @Autowired
    private StudentRepository studentRepository;

    private final String UPLOAD_DIR = System.getProperty("user.dir") + "/src/main/resources/static/uploads/";

    // --- INITIALIZATION (CREATING ADMIN) ---
    @EventListener(ContextRefreshedEvent.class)
    public void init() {
        if (studentRepository.count() == 0) {
            // Regular Student (isAdmin = false)
            studentRepository.save(new Student("2023-0001", "password123", "Test Student", false));
            
            // ADMIN ACCOUNT (isAdmin = true)
            studentRepository.save(new Student("ADMIN-01", "admin123", "System Admin", true));
            
            System.out.println("ACCOUNTS CREATED: ADMIN-01 (Admin) and 2023-0001 (Student)");
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
                    model.addAttribute("error", true);
                    return "login";
                });
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String studentId, 
                           @RequestParam String name, 
                           @RequestParam String password, 
                           Model model) {
        if (studentRepository.existsById(studentId)) {
            model.addAttribute("error", true);
            return "register";
        }
        // FIXED: Added 'false' as the 4th parameter because new users aren't admins
        studentRepository.save(new Student(studentId, password, name, false));
        return "redirect:/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    // --- FEED LOGIC ---

    @GetMapping("/")
    public String showFeed(Model model, HttpSession session, @RequestParam(required = false) String category) {
        Student loggedIn = (Student) session.getAttribute("loggedInStudent");
        if (loggedIn == null) return "redirect:/login";

        List<LostItem> activeItems;
        if (category != null && !category.isEmpty()) {
            activeItems = repository.findByIsReturnedFalseAndCategory(category);
        } else {
            activeItems = repository.findByIsReturnedFalse();
        }

        model.addAttribute("items", activeItems);
        model.addAttribute("archivedItems", repository.findAll().stream().filter(LostItem::isReturned).toList());
        model.addAttribute("student", loggedIn);
        return "index";
    }

    @PostMapping("/report")
    public String reportItem(@RequestParam String itemName, 
                             @RequestParam String description, 
                             @RequestParam String contactInfo,
                             @RequestParam String category,
                             @RequestParam("imageFile") MultipartFile file,
                             HttpSession session) throws IOException {
        
        Student loggedIn = (Student) session.getAttribute("loggedInStudent");
        if (loggedIn == null) return "redirect:/login";

        String fileName = "default.jpg";
        if (!file.isEmpty()) {
            fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path path = Paths.get(UPLOAD_DIR + fileName);
            Files.createDirectories(path.getParent());
            Files.write(path, file.getBytes());
        }

        LostItem newItem = new LostItem(itemName, description, "/uploads/" + fileName, contactInfo, category, loggedIn);
        repository.save(newItem);
        
        return "redirect:/";
    }

    @PostMapping("/delete/{id}")
    public String deleteItem(@PathVariable Long id) {
        repository.deleteById(id);
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

    @PostMapping("/restore/{id}")
    public String restoreItem(@PathVariable Long id, Principal principal, HttpServletRequest request) {
        // 1. Find the item
        repository.findById(id).ifPresent(item -> {
            // 2. Get the current user's name
            String loggedInUser = principal.getName();
            
            // 3. Check if the user is an Admin
            boolean isAdmin = request.isUserInRole("ROLE_ADMIN");

            // 4. Security Check: Only allow if they are the owner OR an admin
            // Note: Change 'getPosterName()' to whatever field stores the username in your Item model
            if (item.getPosterName().equals(loggedInUser) || isAdmin) {
                item.setReturned(false);
                repository.save(item);
            }
        });

        return "redirect:/";
    }
}