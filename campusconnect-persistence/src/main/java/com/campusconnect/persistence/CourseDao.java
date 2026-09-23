package com.campusconnect.persistence;

import java.util.List;

import com.campusconnect.domain.Course;

public interface CourseDao {
    Course findById(Long id);
    List findByTerm(String termCode);
    void save(Course course);
}
