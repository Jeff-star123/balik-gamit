package com.bsit.lostandfound;

import org.springframework.data.repository.CrudRepository;
import java.util.Optional; // This import is very important!
import java.time.LocalDateTime;
import java.util.List;

public interface StudentRepository extends CrudRepository<Student, String> {
    Optional<Student> findByEmail(String email);
    
    // Finds students active in the last 5 minutes
    List<Student> findByLastActiveAfter(LocalDateTime timestamp);
}