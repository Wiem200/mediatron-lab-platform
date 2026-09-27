-- Official MEDIATRON PhD students from the 2025-2026 registration/pre-registration file.
CREATE TEMP TABLE tmp_official_mediatron_phd_students (
    first_name TEXT,
    last_name TEXT,
    thesis_topic TEXT,
    registration_year INTEGER,
    supervisor_first TEXT,
    supervisor_last TEXT,
    co_supervisor TEXT,
    note TEXT
) ON COMMIT DROP;
INSERT INTO tmp_official_mediatron_phd_students (first_name, last_name, thesis_topic, registration_year, supervisor_first, supervisor_last, co_supervisor, note) VALUES
('Rihem', 'Taboubi', 'Informatique quantique et IA pour une optimisation sécurisée et efficace du réseau 6G', 2025, 'Sonia', 'Ben Rejeb', NULL, NULL),
('Lucia Randzavola', 'Raholinarivo', 'Real-Time Recognition of Isolated Sign Language Gestures Using Deep Learning', 2025, 'Nabil', 'Tabbane', 'défaillante', 'défaillante'),
('Balsem', 'Majdoub', 'Gestion Cognitive des Réseaux 6G IA-Natifs Pilotée par les Large Language Models LLMs', 2025, 'Sonia', 'Ben Rejeb', NULL, NULL),
('Zied', 'Ben Amira', 'Using LoRaWAN 4G/5G Connectivity For Distributed Control System for Autonomous Drones', 2025, 'Fatma', 'Abdelkefi', NULL, NULL),
('Yosra', 'Abassi', 'Enabling Autonomous Systems Management Through Context-Aware Intent-Driven Networking and Artificial Intelligence', 2025, 'Nabil', 'Tabbane', NULL, NULL),
('Oumaima', 'Ben Mohamed', 'Service-Driven Multi-Agent Architectures for Private 6G Network Automation', 2025, 'Asma', 'Ben Letaifa', NULL, NULL),
('Ala Eddine', 'Hsairi', 'Enhancing Real Time Video Object Detection and Tracking', 2024, 'Asma', 'Ben Letaifa', NULL, NULL),
('Kater Nada', 'Ayari', 'Secure and optimized small objects detection in images and videos using Generative AI', 2024, 'Asma', 'Ben Letaifa', NULL, NULL),
('Khaoula', 'Kacem', 'Conception et amélioration des Réseaux Non Terrestres NTN pour l''accès Satellite-IoT dans l’écosystème de la 6G', 2024, 'Soumaya', 'Hamouda', NULL, NULL),
('Lucas Justin Yirepoa', 'Kinda', 'Vers une compréhension fine des scènes complexes via la segmentation sémantique des petits objets', 2024, 'Fatma', 'Abdelkefi', 'ESIGELEC/Adnane CABANI', NULL),
('Hela', 'Alaya', 'Resource Management in FL/Blockchain-Based Secure and Collaborative Decision Making for UAV Context', 2023, 'Asma', 'Ben Letaifa', NULL, NULL),
('Youssef', 'Allagui', 'Design Approaches Leveraging Multi-Access Edge Computing MEC Machine Learning and Datasets as Resources to Provide Security for Autonomous Vehicles', 2023, 'Sami', 'Tabbane', NULL, NULL),
('Mohamed Amine', 'Hechmi', 'Gestion dynamique et flexible des ressources pour les services émergents 6G avec SDN et Intelligence artificielle', 2023, 'Sami', 'Tabbane', NULL, NULL),
('Nesrine', 'Maatouk', 'Solving data offloading problem with IA solution in UAV context', 2023, 'Asma', 'Ben Letaifa', NULL, NULL),
('Mehdi', 'Bayarassou', 'Modélisation de systèmes d’apprentissage machine sur les appareils IoT', 2022, 'Sami', 'Tabbane', NULL, NULL),
('Karidja', 'Dominique Christelle', 'Open data dans la smart city de la théorie à la pratique', 2022, 'Asma', 'Ben Letaifa', 'Oussama HABACHI/Université de Clermont Auvergne-France', NULL),
('Rima Hasna', 'Yamouni', 'Green Deep Learning for Semantic Segmentation in the Wild', 2022, 'Fatma', 'Abdelkefi', 'ESIGELEC/Adnane CABANI', NULL),
('Rafik', 'Louati', 'Energy Saving Mechanisms for the Internet of Things (IoT)', 2021, 'Sami', 'Tabbane', NULL, NULL),
('Hela', 'Makina', 'Monitoring and securing healthcare applications in AI/IoT/Cloud context', 2021, 'Asma', 'Ben Letaifa', 'soutenance le 14/02/2026', 'soutenance le 14/02/2026'),
('Insaf', 'Rzig', 'On the optimization of aerial platforms deployment for enhanced communication and computation services in Vertical Heterogeneous Networks (VHetNets)', 2021, 'Sami', 'Tabbane', 'L''ETS/Wael JAAFER-Canada', NULL);

INSERT INTO phd_students (first_name, last_name, thesis_topic, registration_year, research_team, supervisor_id, co_supervisor)
SELECT t.first_name, t.last_name, t.thesis_topic, t.registration_year, 'MEDIATRON',
       (SELECT m.id FROM members m
        WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = LOWER(regexp_replace(t.supervisor_first, '[[:space:]]+', '', 'g'))
          AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = LOWER(regexp_replace(t.supervisor_last, '[[:space:]]+', '', 'g'))
        ORDER BY m.id LIMIT 1),
       NULLIF(t.co_supervisor, '')
FROM tmp_official_mediatron_phd_students t
WHERE NOT EXISTS (
    SELECT 1 FROM phd_students p
    WHERE LOWER(regexp_replace(p.first_name, '[[:space:]]+', '', 'g')) = LOWER(regexp_replace(t.first_name, '[[:space:]]+', '', 'g'))
      AND LOWER(regexp_replace(p.last_name, '[[:space:]]+', '', 'g')) = LOWER(regexp_replace(t.last_name, '[[:space:]]+', '', 'g'))
      AND LOWER(COALESCE(p.thesis_topic, '')) = LOWER(COALESCE(t.thesis_topic, ''))
);