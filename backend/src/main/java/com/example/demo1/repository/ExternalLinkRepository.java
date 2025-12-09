package com.example.demo1.repository;

import com.example.demo1.common.enums.ReviewStatus;
import com.example.demo1.entity.ExternalLink;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExternalLinkRepository extends JpaRepository<ExternalLink, Long> {
    Page<ExternalLink> findByBeverageIdAndStatusAndIsActiveTrue(Long beverageId, ReviewStatus status, Pageable pageable);
    Page<ExternalLink> findByStatus(ReviewStatus status, Pageable pageable);
}
