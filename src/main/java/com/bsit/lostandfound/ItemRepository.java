package com.bsit.lostandfound;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<LostItem, Long> {
    
    // This handles the Search Bar (searching by name)
    List<LostItem> findByIsReturnedFalseAndNameContainingIgnoreCase(String name);
    
    // This handles the Active Feed (showing everything not returned)
    List<LostItem> findByIsReturnedFalse();

    // ADD THIS LINE BELOW - This handles your new Category Filter
    List<LostItem> findByIsReturnedFalseAndCategory(String category);
    
    long countByStatus(String status);
}