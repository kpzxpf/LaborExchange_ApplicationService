package com.vlz.laborexchange_applicationservice.repository;

import com.vlz.laborexchange_applicationservice.entity.ApplicationStatus;
import com.vlz.laborexchange_applicationservice.entity.ApplicationStatusType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplicationStatusRepository extends JpaRepository<ApplicationStatus, Integer> {
    Optional<ApplicationStatus> findByCode(ApplicationStatusType code);
}