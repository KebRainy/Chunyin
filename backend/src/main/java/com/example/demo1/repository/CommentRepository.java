package com.example.demo1.repository;

import com.example.demo1.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findByBeverageIdAndIsDeletedFalse(Long beverageId, Pageable pageable);
    Page<Comment> findByBeverageIdAndParentIsNullAndIsDeletedFalse(Long beverageId, Pageable pageable);
}
