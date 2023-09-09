package com.example.demo.repository;

import com.example.demo.entity.Database;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DatabaseRepo extends JpaRepository<Database, Long> {
}
