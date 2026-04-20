package com.bsit.lostandfound;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Developer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Unique ID for the database

    private String name;
    private String contributions;
    private String section;
    private String photoUrl;

    // 1. MANDATORY: JPA requires a no-argument constructor to fetch data from the database
    public Developer() {
    }

    // 2. Your existing constructor for the Controller's init()
    public Developer(String name, String contributions, String section, String photoUrl) {
        this.name = name;
        this.contributions = contributions;
        this.section = section;
        this.photoUrl = photoUrl;
    }

    // --- GETTERS AND SETTERS ---
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContributions() {
        return contributions;
    }

    public void setContributions(String contributions) {
        this.contributions = contributions;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}