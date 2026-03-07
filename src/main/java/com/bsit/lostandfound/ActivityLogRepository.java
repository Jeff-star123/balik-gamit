package com.bsit.lostandfound;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    // Get the latest 5 activities for the sidebar
    List<ActivityLog> findTop5ByOrderByTimestampDesc();
}