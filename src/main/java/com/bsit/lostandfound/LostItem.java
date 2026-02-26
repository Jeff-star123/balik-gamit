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
    private boolean isReturned = false;
    
    // Only one createdAt variable is needed
    private LocalDateTime createdAt = LocalDateTime.now(); 

    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student poster;

    public LostItem() {}

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

    // This is the "Time Ago" logic for your BALIK GAMIT feed
    public String getFormattedDate() {
        if (this.createdAt == null) return "Just now";
        
        Duration duration = Duration.between(this.createdAt, LocalDateTime.now());
        long minutes = duration.toMinutes();
        long hours = duration.toHours();
        long days = duration.toDays();
        
        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + "m ago";
        if (hours < 24) return hours + "h ago";
        return days + "d ago";
    }

    // Getters and Setters
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
}