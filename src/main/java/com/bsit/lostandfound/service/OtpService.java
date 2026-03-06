package com.bsit.lostandfound.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OtpService {

    // This pulls your key from Railway's "Variables" tab
    @Value("${RESEND_API_KEY}")
    private String resendApiKey;

    public void sendOtp(String recipientEmail, String otpCode) {
        // Initialize Resend with the API Key
        Resend resend = new Resend(resendApiKey);

        CreateEmailOptions params = CreateEmailOptions.builder()
            .from("Balik Gamit <onboarding@resend.dev>") 
            .to(recipientEmail)
            .subject("Security Verification Code - BALIK GAMIT")
            .html("<div style='font-family: Arial, sans-serif; padding: 20px; border: 1px solid #eee; border-radius: 10px;'>" +
                  "<h2>Verification Code</h2>" +
                  "<p>You are receiving this email because a security action was requested.</p>" +
                  "<h1 style='color: #23a6d5; letter-spacing: 5px;'>" + otpCode + "</h1>" +
                  "<p>This code will expire shortly. If you did not request this, please ignore this email.</p>" +
                  "</div>")
            .build();

        try {
            resend.emails().send(params);
            System.out.println("Email sent successfully to: " + recipientEmail);
        } catch (ResendException e) {
            System.err.println("Resend failed to send email: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("General error in OtpService: " + e.getMessage());
            e.printStackTrace();
        }
    }
}