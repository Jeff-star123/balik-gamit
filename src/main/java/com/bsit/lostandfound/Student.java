package com.bsit.lostandfound;

import jakarta.persistence.*;
import java.util.List;

@Entity
public class Student {
    @Id
    private String studentId; // e.g., "2023-0001"
    private String password;
    private String name;
    private String email;
    
    private boolean isAdmin = false;

    @OneToMany(mappedBy = "poster")
    private List<LostItem> postedItems;

    // 1. ADD THIS: JPA MUST have a no-args constructor
    public Student() {}

    // 2. YOUR EXISTING CONSTRUCTOR
    public Student(String studentId, String password, String name, String email, boolean isAdmin) {
        this.studentId = studentId;
        this.password = password;
        this.name = name;
        this.email = email; // Now the computer knows what 'email' refers to
        this.isAdmin = isAdmin;
    }
    
    // 3. GETTERS
    public String getStudentId() { return studentId; }
    public String getPassword() { return password; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    
    // Use this specific naming for booleans
    public boolean isIsAdmin() { return isAdmin; }
    
    // Add these setters just in case your login logic needs them
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setPassword(String password) { this.password = password; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setAdmin(boolean admin) { isAdmin = admin; }
}