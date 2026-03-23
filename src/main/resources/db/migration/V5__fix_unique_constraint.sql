-- Fix conflicting unique constraints:
-- V1 created: UNIQUE (candidate_id, vacancy_id)     — one application per candidate per vacancy regardless of resume
-- V4 created: UNIQUE (vacancy_id, candidate_id, resume_id) — one application per candidate+resume per vacancy
-- The V4 semantics are correct (code uses includeResumeId check), so drop the V1 constraint.
DROP INDEX IF EXISTS idx_unique_candidate_vacancy;
