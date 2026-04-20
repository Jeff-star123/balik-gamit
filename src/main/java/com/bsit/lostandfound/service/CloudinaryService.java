package com.bsit.lostandfound.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(@Value("${cloudinary.cloud_name}") String name,
                             @Value("${cloudinary.api_key}") String key,
                             @Value("${cloudinary.api_secret}") String secret) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
            "cloud_name", name,
            "api_key", key,
            "api_secret", secret));
    }

    public String uploadImage(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return "/images/default.jpg"; // Fallback if no file is uploaded
        }
        // Uploads the file and returns the secure HTTPS URL
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
        return uploadResult.get("secure_url").toString();
    }
}