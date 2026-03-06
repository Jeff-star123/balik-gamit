package com.bsit.lostandfound;

public class Developer {
    private String name;
    private String contributions;
    private String section;
    private String photoUrl;

    // Must have this constructor for the Controller's init() to work
    public Developer(String name, String contributions, String section, String photoUrl) {
        this.name = name;
        this.contributions = contributions;
        this.section = section;
        this.photoUrl = photoUrl;
    }

    // GETTERS AND SETTERS ARE REQUIRED
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getContributions() { return contributions; }
    public void setContributions(String contributions) { this.contributions = contributions; }
    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
}