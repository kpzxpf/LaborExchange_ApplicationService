package com.vlz.laborexchange_applicationservice.repository;

import com.vlz.laborexchange_applicationservice.entity.Application;
import com.vlz.laborexchange_applicationservice.entity.ApplicationStatusType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    Optional<List<Application>> findByVacancyId(Long vacancyId);
    Optional<List<Application>> findByCandidateId(Long candidateId);
    boolean existsByVacancyIdAndCandidateIdAndResumeId(Long vacancyId, Long candidateId, Long resumeId);
    Optional<List<Application>> findByStatus_Code(ApplicationStatusType statusType);
    Optional<List<Application>> findByEmployerId(Long employerId);

    @Query("SELECT COUNT(a) FROM Application a WHERE a.status.code = :status")
    Long countByStatus(@Param("status") ApplicationStatusType status);

    @Query("SELECT a.status.code, COUNT(a) FROM Application a GROUP BY a.status.code")
    List<Object[]> countByStatusGrouped();

    @Query("SELECT COUNT(a) FROM Application a WHERE a.status.code IN :statuses")
    Long countByStatusIn(@Param("statuses") List<ApplicationStatusType> statuses);
}
