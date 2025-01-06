
package com.leadmanagement.crm.controller;

import com.leadmanagement.crm.model.Lead;
import com.leadmanagement.crm.service.EmailService;
import com.leadmanagement.crm.service.LeadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leads")
public class LeadController {

    @Autowired
    private LeadService leadService;

    @Autowired
    private EmailService emailService;
    @Value("${hubspot.api.token}")
    private String hubSpotAccessToken;

    @GetMapping
    public List<Lead> getAllLeads() {
        return leadService.getAllLeads();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Lead> getLeadById(@PathVariable String id) {
        return leadService.getLeadById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createLead(@RequestBody Lead lead) {
        // Step 1: Save the lead to MongoDB
        Lead savedLead = leadService.saveLead(lead);

        // Step 2: Sync with HubSpot
        String url = "https://api.hubapi.com/crm/v3/objects/contacts";
        RestTemplate restTemplate = new RestTemplate();

        try {
            // Prepare the HubSpot request payload
            Map<String, Object> hubSpotRequestBody = Map.of(
                    "properties", Map.of(
                            "firstname", lead.getName(),  // Use single name field
                                "email", lead.getEmail(),
                            "phone", lead.getPhone(),
                            "message", lead.getMessage()
                    )
            );

            // Prepare headers for the HubSpot API
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(hubSpotAccessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(hubSpotRequestBody, headers);

            // Make the request to HubSpot
            ResponseEntity<String> hubspotResponse = restTemplate.postForEntity(url, entity, String.class);
            // Print the HubSpot API response
            System.out.println("HubSpot Response: " + hubspotResponse.getBody());

            // Check response and return success
            if (hubspotResponse.getStatusCode() == HttpStatus.CREATED || hubspotResponse.getStatusCode() == HttpStatus.OK) {
                return ResponseEntity.status(HttpStatus.CREATED).body(savedLead);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Lead saved in MongoDB, but failed to sync with HubSpot: " + hubspotResponse.getBody());
            }

        } catch (Exception e) {
            // Handle API exceptions
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lead saved in MongoDB, but failed to sync with HubSpot: " + e.getMessage());
        }
    }


    @PutMapping("/{id}")
    public ResponseEntity<Lead> updateLead(@PathVariable String id, @RequestBody Lead lead) {
        try {
            return ResponseEntity.ok(leadService.updateLead(id, lead));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLead(@PathVariable String id) {
        leadService.deleteLead(id);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/subscribe")
    public ResponseEntity<String> subscribe(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");

        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body("Email is required");
        }
        try {
            emailService.sendSubscriptionEmail(
                    email,
                    "Welcome to ServCyber!",
                    "<h1>Thank you for subscribing to our newsletter!</h1><p>Stay tuned for updates.</p>"
            );
            return ResponseEntity.ok("Subscription email sent successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to send subscription email: " + e.getMessage());
        }
    }
}
