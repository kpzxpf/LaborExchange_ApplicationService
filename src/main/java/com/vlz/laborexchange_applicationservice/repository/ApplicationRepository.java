package com.vlz.laborexchange_applicationservice.repository;

import com.vlz.laborexchange_applicationservice.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    Optional<List<Application>> findByVacancyId(Long vacancyId);
    Optional<List<Application>> findByCandidateId(Long candidateId);
}
