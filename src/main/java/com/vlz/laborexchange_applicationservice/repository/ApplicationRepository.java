package com.vlz.laborexchange_applicationservice.repository;

import com.vlz.laborexchange_applicationservice.entity.Application;
import com.vlz.laborexchange_applicationservice.entity.ApplicationStatusType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByVacancyId(Long vacancyId);
    List<Application> findByCandidateId(Long candidateId);
    boolean existsByVacancyIdAndCandidateIdAndResumeId(Long vacancyId, Long candidateId, Long resumeId);
    List<Application> findByStatus_Code(ApplicationStatusType statusType);
    List<Application> findByEmployerId(Long employerId);

    // Pageable variants to avoid unbounded List queries in high-volume scenarios
    Page<Application> findByVacancyId(Long vacancyId, Pageable pageable);
    Page<Application> findByCandidateId(Long candidateId, Pageable pageable);
    Page<Application> findByEmployerId(Long employerId, Pageable pageable);

    @Query("SELECT COUNT(a) FROM Application a WHERE a.status.code = :status")
    Long countByStatus(@Param("status") ApplicationStatusType status);

    @Query("SELECT COUNT(a) FROM Application a WHERE a.employerId = :employerId AND a.status.code = :status")
    Long countByEmployerIdAndStatus(@Param("employerId") Long employerId, @Param("status") ApplicationStatusType status);

    @Query("SELECT a.status.code, COUNT(a) FROM Application a GROUP BY a.status.code")
    List<Object[]> countByStatusGrouped();

    @Query("SELECT a.status.code, COUNT(a) FROM Application a WHERE a.employerId = :employerId GROUP BY a.status.code")
    List<Object[]> countByEmployerIdAndStatusGrouped(@Param("employerId") Long employerId);

    @Query("SELECT COUNT(a) FROM Application a WHERE a.status.code IN :statuses")
    Long countByStatusIn(@Param("statuses") List<ApplicationStatusType> statuses);

    @Query("SELECT COUNT(a) FROM Application a WHERE a.employerId = :employerId AND a.status.code IN :statuses")
    Long countByEmployerIdAndStatusIn(@Param("employerId") Long employerId, @Param("statuses") List<ApplicationStatusType> statuses);

    Long countByEmployerId(Long employerId);
}