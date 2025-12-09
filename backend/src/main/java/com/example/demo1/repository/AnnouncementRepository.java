package com.example.demo1.repository;

import com.example.demo1.common.enums.ReviewStatus;
import com.example.demo1.entity.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    Page<Announcement> findByStatusAndIsActiveTrue(ReviewStatus status, Pageable pageable);
    Page<Announcement> findByStatus(ReviewStatus status, Pageable pageable);
}
