package com.bsit.lostandfound;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

@Entity
public class Student {
    @Id
    private String studentId; 

    @Size(min = 10, message = "Password must be at least 10 characters long")
    @Pattern(
        regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).*$",
        message = "Password must contain at least one uppercase, one lowercase, one number, and one special character"
    )
    private String password;
    
    private String name;    
    private String email;
    private java.time.LocalDateTime lastActive;
    
    private boolean isAdmin = false;

    @OneToMany(mappedBy = "poster")
    private List<LostItem> postedItems;

    // 1. JPA MUST have a no-args constructor
    public Student() {}

    // 2. YOUR EXISTING CONSTRUCTOR
    public Student(String studentId, String password, String name, String email, boolean isAdmin) {
        this.studentId = studentId;
        this.password = password;
        this.name = name;
        this.email = email;
        this.isAdmin = isAdmin;
    }

    public java.time.LocalDateTime getLastActive() { return lastActive; }
    public void setLastActive(java.time.LocalDateTime lastActive) { this.lastActive = lastActive; }
    
    // 3. GETTERS
    public String getStudentId() { return studentId; }
    public String getPassword() { return password; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    
    public boolean isIsAdmin() { return isAdmin; }
    
    // 4. SETTERS
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setPassword(String password) { this.password = password; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setAdmin(boolean admin) { isAdmin = admin; }
}