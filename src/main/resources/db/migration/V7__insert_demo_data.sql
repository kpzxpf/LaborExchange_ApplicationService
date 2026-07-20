-- Demo applications in all statuses, with cover letters and realistic dates.

UPDATE applications
SET cover_letter = COALESCE(cover_letter,
    CASE status_id
        WHEN 1 THEN 'Здравствуйте! Откликаюсь на вакансию: опыт и навыки совпадают с требованиями. Готов обсудить детали на интервью.'
        WHEN 2 THEN 'Отклик был рассмотрен, но по итогам отбора компания выбрала другого кандидата.'
        WHEN 3 THEN 'Кандидат отозвал отклик после изменения карьерных планов.'
        WHEN 4 THEN 'Кандидат успешно прошел этапы отбора и получил положительное решение.'
        ELSE 'Демо-сопроводительное письмо.'
    END),
    updated_at = NOW()
WHERE id BETWEEN 1 AND 40;

INSERT INTO applications (id, employer_id, vacancy_id, candidate_id, resume_id, status_id, cover_letter, created_at, updated_at)
VALUES
    (100, 121, 100, 101, 100, 1, 'Здравствуйте! У меня 6 лет Java/Spring, production Kafka и Redis. Интересна ваша платформа найма и возможность усилить backend-команду.', NOW() - INTERVAL '9 days', NOW() - INTERVAL '9 days'),
    (101, 121, 101, 102, 102, 4, 'Мария: хочу развивать интерфейс личного кабинета. Есть опыт Next.js, дизайн-систем и e2e-тестов.', NOW() - INTERVAL '8 days', NOW() - INTERVAL '2 days'),
    (102, 121, 102, 105, 105, 1, 'Готов обсудить SRE-задачи: Kubernetes, Terraform, GitLab CI и мониторинг SLO.', NOW() - INTERVAL '7 days', NOW() - INTERVAL '7 days'),
    (103, 122, 103, 103, 103, 1, 'Работал с продуктовой аналитикой, SQL, Python и ClickHouse. Интересен финтех-домен.', NOW() - INTERVAL '7 days', NOW() - INTERVAL '7 days'),
    (104, 122, 104, 103, 103, 4, 'Есть опыт Airflow, витрин данных и контроля качества данных. Готов рассказать о проектах.', NOW() - INTERVAL '6 days', NOW() - INTERVAL '1 day'),
    (105, 123, 105, 104, 104, 1, 'Автоматизирую UI/API проверки на Playwright и Selenium, умею стабилизировать flaky-тесты.', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
    (106, 123, 106, 106, 106, 2, 'Junior Python профиль, хочу развиваться в ML-сервисах. Готова выполнить тестовое задание.', NOW() - INTERVAL '5 days', NOW() - INTERVAL '3 days'),
    (107, 124, 108, 102, 108, 1, 'Есть опыт UX research и дизайн-систем, могу помочь улучшить onboarding и игровые метрики.', NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days'),
    (108, 125, 109, 101, 101, 3, 'Отклик на Kotlin-стажировку был отозван: кандидат выбрал backend-позицию уровня middle.', NOW() - INTERVAL '4 days', NOW() - INTERVAL '2 days'),
    (109, 125, 110, 107, 107, 2, 'Кандидат хорошо подходит по support-навыкам, но аккаунт сейчас деактивирован для демонстрации админ-флоу.', NOW() - INTERVAL '3 days', NOW() - INTERVAL '2 days'),
    (110, 122, 112, 101, 100, 1, 'Интересен аудит backend-инфраструктуры и secure SDLC. Есть опыт production-сервисов.', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
    (111, 125, 113, 102, 108, 4, 'Опыт UX и продуктовых процессов поможет на HR Tech проектах. Готова подключиться к discovery.', NOW() - INTERVAL '2 days', NOW() - INTERVAL '12 hours'),
    (112, 123, 114, 103, 103, 1, 'Могу закрывать SQL/Python аналитику и строить продуктовые дашборды.', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day')
ON CONFLICT (vacancy_id, candidate_id, resume_id) DO UPDATE SET
    employer_id = EXCLUDED.employer_id,
    status_id = EXCLUDED.status_id,
    cover_letter = EXCLUDED.cover_letter,
    updated_at = EXCLUDED.updated_at;

SELECT setval(pg_get_serial_sequence('applications', 'id'), COALESCE((SELECT MAX(id) FROM applications), 1), TRUE);
SELECT setval(pg_get_serial_sequence('application_statuses', 'id'), COALESCE((SELECT MAX(id) FROM application_statuses), 1), TRUE);
