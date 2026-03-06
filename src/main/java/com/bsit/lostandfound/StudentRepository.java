package com.bsit.lostandfound;

import org.springframework.data.repository.CrudRepository;
import java.util.Optional; // This import is very important!

public interface StudentRepository extends CrudRepository<Student, String> {
    
    // Spring Boot will automatically create the SQL for this 
    // as long as your Student class has a variable named 'email'
    // In StudentRepository.java
    Optional<Student> findFirstByEmail(String email);
}
