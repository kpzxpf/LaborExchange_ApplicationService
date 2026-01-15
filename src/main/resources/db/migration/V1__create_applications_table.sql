CREATE TABLE application_statuses
(
    id          SERIAL PRIMARY KEY,
    code        VARCHAR(50)  NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    description TEXT
);

INSERT INTO application_statuses (code, name, description)
VALUES ('NEW', 'Новый', 'Соискатель только что отправил отклик'),
       ('REJECTED', 'Отказ', 'По той или иной причине в найме отказано'),
       ('WITHDRAWN', 'Отозван', 'Соискатель отозвал свой отклик');

CREATE TABLE applications
(
    id           BIGSERIAL PRIMARY KEY,
    employer_id  INTEGER NOT NULL, -- Кто нанимает
    vacancy_id   INTEGER NOT NULL, -- На какую вакансию
    candidate_id INTEGER NOT NULL, -- Кто откликнулся
    resume_id    INTEGER NOT NULL, -- Какое резюме прикрепил
    status_id    INTEGER NOT NULL         DEFAULT 1,
    created_at   TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_application_status
        FOREIGN KEY (status_id)
            REFERENCES application_statuses (id)
);

CREATE INDEX idx_apps_employer_id ON applications (employer_id);
CREATE INDEX idx_apps_candidate_id ON applications (candidate_id);
CREATE INDEX idx_apps_status_id ON applications (status_id);

CREATE UNIQUE INDEX idx_unique_candidate_vacancy ON applications (candidate_id, vacancy_id);