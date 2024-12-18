package com.leadmanagement.crm.repository;


import com.leadmanagement.crm.model.Lead;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface LeadRepository extends MongoRepository<Lead, String> {
}
