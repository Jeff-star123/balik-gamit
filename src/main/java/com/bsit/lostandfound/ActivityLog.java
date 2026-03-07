package com.bsit.lostandfound;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.Duration;

@Entity
@Table(name = "activity_logs") // Best practice: use plural for table names
public class ActivityLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "performer_name") // This fixes the "user" reserved word error
    private String user;
    
    private String action;
    private String item;
    
    // Ensure this is initialized or set in the constructor
    private LocalDateTime timestamp = LocalDateTime.now();

    public String getTimeAgo() {
        if (timestamp == null) return "Just now";
        long minutes = Duration.between(timestamp, LocalDateTime.now()).toMinutes();
        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + "m ago";
        if (minutes < 1440) return (minutes / 60) + "h ago";
        return (minutes / 1440) + "d ago";
    }

    // Getters and Setters...
    public Long getId() { return id; }
    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getItem() { return item; }
    public void setItem(String item) { this.item = item; }
    
    // Add Getter/Setter for timestamp just in case
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}