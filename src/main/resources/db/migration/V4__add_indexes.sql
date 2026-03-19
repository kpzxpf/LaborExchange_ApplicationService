-- Application lookups by vacancy (employer viewing applicants)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_applications_vacancy_id ON applications(vacancy_id);

-- Duplicate check (existsByVacancyIdAndCandidateIdAndResumeId)
CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS idx_applications_unique_application
    ON applications(vacancy_id, candidate_id, resume_id);

-- Combined: employer + status (statistics queries)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_applications_employer_status ON applications(employer_id, status_id);
