package com.leadmanagement.crm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadmanagement.crm.model.Lead;
import com.leadmanagement.crm.repository.LeadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@Service
public class LeadService {

    @Autowired
    private LeadRepository leadRepository;

    public Lead saveLead(Lead lead) {
        lead.setCreatedAt(LocalDateTime.now());
        lead.setStatus("New"); // Default status
        return leadRepository.save(lead);
    }

    public List<Lead> getAllLeads() {
        return leadRepository.findAll();
    }

    public Optional<Lead> getLeadById(String id) {
        return leadRepository.findById(id);
    }

    public Lead updateLead(String id, Lead updatedLead) {
        return leadRepository.findById(id).map(lead -> {
            lead.setName(updatedLead.getName());
            lead.setEmail(updatedLead.getEmail());
            lead.setPhone(updatedLead.getPhone());
            lead.setMessage(updatedLead.getMessage());
            lead.setStatus(updatedLead.getStatus());
            return leadRepository.save(lead);
        }).orElseThrow(() -> new RuntimeException("Lead not found"));
    }

    public void deleteLead(String id) {
        leadRepository.deleteById(id);
    }

    public void processHubSpotLeads(String responseBody) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(responseBody);
            JsonNode contacts = rootNode.get("contacts");

            Iterator<JsonNode> elements = contacts.elements();
            while (elements.hasNext()) {
                JsonNode contact = elements.next();

                Lead lead = new Lead();
                lead.setName(contact.path("properties").path("firstname").path("value").asText() + " " +
                        contact.path("properties").path("lastname").path("value").asText());
                lead.setEmail(contact.path("properties").path("email").path("value").asText());
                lead.setPhone(contact.path("properties").path("phone").path("value").asText());
                lead.setMessage("Imported from HubSpot");
                lead.setCreatedAt(LocalDateTime.now());
                lead.setStatus("New");

                saveLead(lead);
            }
        } catch (Exception e) {
            System.err.println("Error processing HubSpot leads: " + e.getMessage());
        }
    }
}
