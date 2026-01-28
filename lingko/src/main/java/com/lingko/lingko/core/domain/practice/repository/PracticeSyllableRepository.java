package com.lingko.lingko.core.domain.practice.repository;

import com.lingko.lingko.core.domain.practice.entity.PracticeSyllable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PracticeSyllableRepository extends JpaRepository<PracticeSyllable, Long> {
}
