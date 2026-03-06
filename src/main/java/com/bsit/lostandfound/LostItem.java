package com.bsit.lostandfound;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.Duration;

@Entity 
public class LostItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    private Long id;
    
    private String name;
    private String description;
    private String imagePath;
    private String contactInfo;
    private String category;
    private String status; // NEW: Stores "LOST" or "FOUND"
    private boolean isReturned = false;
    
    private LocalDateTime createdAt = LocalDateTime.now(); 

    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student poster;

    public LostItem() {}

    // Updated Constructor to include status
    public LostItem(String name, String description, String imagePath, String contactInfo, String category, Student poster) {
        this.name = name;
        this.description = description;
        this.imagePath = imagePath;
        this.contactInfo = contactInfo;
        this.category = category;
        this.poster = poster; 
        this.isReturned = false;
        this.createdAt = LocalDateTime.now(); 
    }

    // Time Ago logic
    public String getFormattedDate() {
        if (this.createdAt == null) return "Just now";
        Duration duration = Duration.between(this.createdAt, LocalDateTime.now());
        long minutes = duration.toMinutes();
        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + "m ago";
        if (duration.toHours() < 24) return duration.toHours() + "h ago";
        return duration.toDays() + "d ago";
    }

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getImagePath() { return imagePath; }
    public String getContactInfo() { return contactInfo; }
    public boolean isReturned() { return isReturned; }
    public void setReturned(boolean returned) { this.isReturned = returned; }
    public String getCategory() { return category; }
    public Student getPoster() { return poster; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // NEW Getters and Setters for Status
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}