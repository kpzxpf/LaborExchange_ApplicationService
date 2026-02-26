INSERT INTO application_statuses (id, code, name, description)
VALUES (4, 'ACCEPTED', 'Принят', 'Работодатель принял отклик')
ON CONFLICT (id) DO NOTHING;
