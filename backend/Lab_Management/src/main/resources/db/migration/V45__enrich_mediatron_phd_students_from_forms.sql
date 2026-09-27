-- Enrich MEDIATRON PhD student records from the doctoral information form.
-- This migration updates existing PhD records, inserts missing ones if needed, and links
-- publications only when the form provides a DOI that already exists in the publications table.

CREATE OR REPLACE FUNCTION pg_temp.norm_mediatron_name(value TEXT)
RETURNS TEXT
LANGUAGE plpgsql
IMMUTABLE
AS $$
DECLARE
    v TEXT := lower(COALESCE(value, ''));
BEGIN
    v := replace(v, 'à', 'a');
    v := replace(v, 'á', 'a');
    v := replace(v, 'â', 'a');
    v := replace(v, 'ä', 'a');
    v := replace(v, 'ã', 'a');
    v := replace(v, 'å', 'a');
    v := replace(v, 'ç', 'c');
    v := replace(v, 'è', 'e');
    v := replace(v, 'é', 'e');
    v := replace(v, 'ê', 'e');
    v := replace(v, 'ë', 'e');
    v := replace(v, 'ì', 'i');
    v := replace(v, 'í', 'i');
    v := replace(v, 'î', 'i');
    v := replace(v, 'ï', 'i');
    v := replace(v, 'ñ', 'n');
    v := replace(v, 'ò', 'o');
    v := replace(v, 'ó', 'o');
    v := replace(v, 'ô', 'o');
    v := replace(v, 'ö', 'o');
    v := replace(v, 'õ', 'o');
    v := replace(v, 'ù', 'u');
    v := replace(v, 'ú', 'u');
    v := replace(v, 'û', 'u');
    v := replace(v, 'ü', 'u');
    v := replace(v, 'ý', 'y');
    v := replace(v, 'ÿ', 'y');
    v := replace(v, 'œ', 'oe');
    v := replace(v, 'æ', 'ae');
    RETURN regexp_replace(v, '[^a-z0-9]', '', 'g');
END;
$$;


CREATE TEMP TABLE tmp_mediatron_phd_student_form (
    first_name TEXT,
    last_name TEXT,
    thesis_topic TEXT,
    registration_year INTEGER,
    supervisor_first TEXT,
    supervisor_last TEXT,
    co_supervisor TEXT,
    publications_note TEXT
) ON COMMIT DROP;

INSERT INTO tmp_mediatron_phd_student_form (
    first_name,
    last_name,
    thesis_topic,
    registration_year,
    supervisor_first,
    supervisor_last,
    co_supervisor,
    publications_note
) VALUES
('Khaoula', 'Kacem', 'Conception et amélioration des Réseaux Non Terrestres NTN pour l''accès Satellite-IoT dans l’écosystème de la 6G', 2024, 'Soumaya', 'Hamouda', 'Nawel Zangar', 'Satellite-IoT Access Technologies and Simulation Frameworks in 6G Non-Terrestrial Networks: Limitations and Modeling Challenges (MENACOMM 2026 Conference)'),
('Zied', 'Ben Amira', 'Vers un système intelligent et distribué de contrôle de drones autonome basé sur une connectivité hybride LoRaWAN et 5G/6Gs', 2025, 'Fatma', 'Abdelkefi', 'Dr.Nejib Boubaker', 'Pas encore'),
('Rihem', 'Taboubi', 'Informatique Quantique et IA pour une optimisation efficace du réseau 6G', 2025, 'Sonia', 'Ben Rejeb', NULL, 'Travaux de recherche en cours'),
('Youssef', 'Allagui', 'Design Approaches Leveraging Multi-Access Edge Computing (MEC), Machine Learning, and Datasets as Resources to Provide Security for Autonomous Vehicles', 2023, 'Nabil', 'Tabbane', NULL, '1'),
('Yosra', 'Abassi', 'Enabling Autonomous Systems Management Through Context-Aware Intent-Driven Networking and Artificial Intelligence.', 2025, 'Nabil', 'Tabbane', NULL, 'Context aware life cycle for intent-based networking systems.'),
('Oumaima', 'Ben Mohamed', 'Intent-Driven Autonomous AI Model Generation for AI-Native 6G Networks', 2026, 'Asma', 'Ben Letaifa', 'Abderrezak Rachedi', '0'),
('Lucas Justin Yirepoa', 'Kinda', 'Vers une compréhension fine des scènes complexes via la segmentation sémantique de petits objets.', 2024, 'Fatma', 'Abdelkefi', 'Dr CABANI Adnane, Dr TRABELSI Rim', 'A comparative review of state-of-the-art semantic segmentation architectures for high-resolution aerial imagery'),
('Rafik', 'Louati', 'Energy saving mechanisms for IoT', 2021, 'Nabil', 'Tabbane', 'Meriem Thaalbi', 'Enhancing LoRaWAN Class B: A Pseudo-Random Access Mechanism with Orthogonality Awareness. 10.1109/IWCMC58020.2023.10183191
An Adaptive Data Rate scheme for multi-hop forwarding in LoRaWAN networks. 10.1109/IWCMC61514.2024.10592512
A Secure Multi-Hop Communication Framework for Nomadic Devices in LoRaWAN Networks. 10.1109/ComNet64071.2024.10987351'),
('Kater Nada', 'Ayari', 'Secure and optimized small objects detection in images and videos using Generative AI', 2025, 'Asma', 'Ben Letaifa', NULL, 'Pas encore de publications'),
('Mehdi', 'Bayarassou', 'Modélisation de systèmes d’apprentissage machine sur les appareils IoT', 2022, 'Sami', 'Tabbane', NULL, NULL),
('Rima Hasna', 'Yamouni', 'Green deep learning for semantic segmentation in the wild', 2022, 'Fatma', 'Abdelkefi', 'Adnane Cabani et Rim Trabelsi', 'WildAnimal650+: A large-scale annotated dataset for generalized animal segmentation in unconstrained environments
Efficient Semantic Segmentation via Advanced Prompt Tuning Techniques
Towards Shallow Active Learning for Accurate Semantic Segmentation Datasets');

-- Insert missing PhD students only if the normalized name does not already exist.
INSERT INTO phd_students (
    first_name,
    last_name,
    thesis_topic,
    registration_year,
    research_team,
    supervisor_id,
    co_supervisor
)
SELECT
    t.first_name,
    t.last_name,
    NULLIF(t.thesis_topic, ''),
    t.registration_year,
    'MEDIATRON',
    (
        SELECT s.id
        FROM members s
        WHERE pg_temp.norm_mediatron_name(s.first_name) = pg_temp.norm_mediatron_name(t.supervisor_first)
          AND pg_temp.norm_mediatron_name(s.last_name) = pg_temp.norm_mediatron_name(t.supervisor_last)
        ORDER BY s.id
        LIMIT 1
    ),
    NULLIF(t.co_supervisor, '')
FROM tmp_mediatron_phd_student_form t
WHERE NOT EXISTS (
    SELECT 1
    FROM phd_students p
    WHERE pg_temp.norm_mediatron_name(p.first_name) = pg_temp.norm_mediatron_name(t.first_name)
      AND pg_temp.norm_mediatron_name(p.last_name) = pg_temp.norm_mediatron_name(t.last_name)
);

-- Update existing PhD students with the richer/latest form data.
UPDATE phd_students p
SET
    thesis_topic = COALESCE(NULLIF(t.thesis_topic, ''), p.thesis_topic),
    registration_year = COALESCE(t.registration_year, p.registration_year),
    research_team = COALESCE(NULLIF(p.research_team, ''), 'MEDIATRON'),
    supervisor_id = COALESCE((
        SELECT s.id
        FROM members s
        WHERE pg_temp.norm_mediatron_name(s.first_name) = pg_temp.norm_mediatron_name(t.supervisor_first)
          AND pg_temp.norm_mediatron_name(s.last_name) = pg_temp.norm_mediatron_name(t.supervisor_last)
        ORDER BY s.id
        LIMIT 1
    ), p.supervisor_id),
    co_supervisor = COALESCE(NULLIF(t.co_supervisor, ''), p.co_supervisor)
FROM tmp_mediatron_phd_student_form t
WHERE pg_temp.norm_mediatron_name(p.first_name) = pg_temp.norm_mediatron_name(t.first_name)
  AND pg_temp.norm_mediatron_name(p.last_name) = pg_temp.norm_mediatron_name(t.last_name);


CREATE TEMP TABLE tmp_mediatron_phd_publication_dois (
    first_name TEXT,
    last_name TEXT,
    doi TEXT
) ON COMMIT DROP;

INSERT INTO tmp_mediatron_phd_publication_dois (first_name, last_name, doi) VALUES
('Rafik', 'Louati', '10.1109/iwcmc58020.2023.10183191'),
('Rafik', 'Louati', '10.1109/iwcmc61514.2024.10592512'),
('Rafik', 'Louati', '10.1109/comnet64071.2024.10987351');

INSERT INTO phd_student_publications (phd_student_id, publication_id)
SELECT DISTINCT
    p.id,
    pub.id
FROM tmp_mediatron_phd_publication_dois t
JOIN phd_students p
  ON pg_temp.norm_mediatron_name(p.first_name) = pg_temp.norm_mediatron_name(t.first_name)
 AND pg_temp.norm_mediatron_name(p.last_name) = pg_temp.norm_mediatron_name(t.last_name)
JOIN publications pub
  ON lower(pub.doi) = lower(t.doi)
WHERE NOT EXISTS (
    SELECT 1
    FROM phd_student_publications existing
    WHERE existing.phd_student_id = p.id
      AND existing.publication_id = pub.id
);

