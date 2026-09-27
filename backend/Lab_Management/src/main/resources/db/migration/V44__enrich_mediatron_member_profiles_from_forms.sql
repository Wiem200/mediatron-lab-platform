-- Enrich MEDIATRON member public profiles from the website information form.
-- This migration updates/creates scientific public profiles only. It does not create login accounts.
-- Free-text "Projets en cours" answers are intentionally kept out of the projects table because
-- projects must remain validated records in the project workflow.

-- Allow long self-declared research axes from the website form.
ALTER TABLE member_research_axes
ALTER COLUMN research_axes TYPE TEXT;

-- Legacy schemas may still store grade in a short VARCHAR column.
ALTER TABLE members
ALTER COLUMN grade TYPE VARCHAR(255);

-- Form contact strings can include both email and phone numbers.
ALTER TABLE members
ALTER COLUMN phone TYPE TEXT;

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


CREATE TEMP TABLE tmp_mediatron_member_profile_form (
    first_name TEXT,
    last_name TEXT,
    grade TEXT,
    contact_public TEXT,
    phone_only TEXT,
    linkedin_url TEXT,
    google_scholar_url TEXT,
    personal_site_url TEXT,
    research_axes TEXT,
    bio TEXT,
    projects_note TEXT
) ON COMMIT DROP;

INSERT INTO tmp_mediatron_member_profile_form (
    first_name,
    last_name,
    grade,
    contact_public,
    phone_only,
    linkedin_url,
    google_scholar_url,
    personal_site_url,
    research_axes,
    bio,
    projects_note
) VALUES
      ('Soumaya', 'Hamouda', 'Professeur', 'soumaya.hamouda@supcom.tn', NULL, 'https://www.linkedin.com/in/soumaya-hamouda-832831368/', NULL, NULL, '5G/6G; O-RAN; Virtualization; Network Slicing; Digital Twin', 'Soumaya Hamouda received the Engineering degree in electrical engineering and the M.Sc. degree in communication systems from ENIT, Tunisia, and the Ph.D. and Habilitation degrees in Information and Communication Technologies from SUPâ€™COM, Tunisia. She is currently Professor in Telecommunications at the Faculty of Sciences of Bizerte (University of Carthage). She is a member of the MEDIATRON research laboratory at SUPâ€™COM, the college board of the Tunisian regulator INT, and an IEEE Senior Member. Her research focuses on radio resource allocation, multiple access techniques, CRAN/VRAN/ORAN, network virtualization, Digital Twin and Network Slicing for 5G/B5G/6G networks.', 'O-RAN and DT for B5G/6G; NTN for 6G'),
      ('Asma', 'Ben Letaifa', 'Maitre de ConfÃ©rences', 'asma.benletaifa@supcom.tn, +216 98 265090', '+216 98 265090', 'https://www.linkedin.com/in/asma-ben-letaifa-390bb218/', NULL, NULL, 'SystÃ¨mes connectÃ©s intelligents (IoT, IoV, UAV);Intelligence distribuÃ©e pour architectures cloudâ€“edge;IA pour systÃ¨mes autonomes, e-santÃ© et infrastructures critiques', 'Asma BEN LETAIFA est MaÃ®tre de confÃ©rences Ã  SUPâ€™COM (Higher School of Communication, Tunisie) et chercheuse senior au laboratoire MEDIATRON.', NULL),
      ('Fatma', 'Abdelkefi', 'Professeur', 'fatma.abdelkefi@supcom.tn', NULL, 'https://www.linkedin.com/in/fatma-abdelkefi-02752787/', NULL, NULL, 'Communications sans fil avancÃ©es (5G/B5G/6G, multi-bandes, MIMO, NOMA);Intelligence artificielle pour les rÃ©seaux et le traitement du signal;Traitement du signal pour systÃ¨mes multicarriers (OFDM, FBMC);Vision par ordinateur et deep learning (dÃ©tection, segmentation, GANs);Optimisation et apprentissage pour systÃ¨mes complexes', 'Fatma Abdelkefi (Senior Member, IEEE) a obtenu le diplÃ´me dâ€™ingÃ©nieur en gÃ©nie Ã©lectrique Ã  lâ€™Ã‰cole Nationale dâ€™Enseignement de Tunis (ENIT)... Depuis 2009, elle est membre du corps professoral de lâ€™Ã‰cole SupÃ©rieure des Communications de Tunis (SUPâ€™COM). Elle dirige, depuis 2024, le laboratoire RÃ©seaux Radio-Mobiles MultimÃ©dia (MEDIATRON) Ã  SUPâ€™COM.', 'Projet du laboratoire Mediatron "Optimisation et analyse des donnÃ©es"'),
      ('Nabil', 'Tabbane', 'Professeur', 'nabil.tabbane@supcom.tn', NULL, 'https://www.linkedin.com/in/nabil-tabbane-81539964/?originalSubdomain=tn', NULL, NULL, 'QoS in ad hoc and sensor networks, Cloud Computing and Virtualization, Information Systems Security, Internet of Things and Cloud Security, QoS Aware Orchestration Mechanisms for NFV, Mobility Management and Security of Wireless Networks.', 'Nabil Tabbane is full professor in telecommunications, specializing in networking, information security, and cloud computing. He is currently a faculty member and researcher at the Higher School of Communications of Tunis (SUPâ€™COM), University of Carthage.', 'Ensuring the security and reliability of health information systems (HIS) through the generation of realistic data, analysis, and detection of anomalies and cyberattacks.'),
      ('Hajer', 'Tounsi', 'MaÃ®tre assistante', 'Hajer.Tounsi@supcom.tn', NULL, NULL, NULL, NULL, 'Gestion de ressources, QualitÃ© de service et sÃ©curitÃ© dans les rÃ©seaux IoT, Gestion de la QoS dans les RÃ©seaux Ã©mergents (SDN, Digital Twin, Intent Based Networks, etc.)', 'Docteur Hajer Tounsi, enseignante chercheure Ã  lâ€™Ecole SupÃ©rieure des Communications de Tunis (SUPâ€™COM), UniversitÃ© de Carthage. Elle a obtenu son Habilitation universitaire en Informatique en 2024 de lâ€™Ecole Nationale des Sciences de lâ€™Informatique (ENSI).', 'Projet Erasmus+ Health-Tech, https://health-tech.rnu.tn/'),
      ('Sonia', 'Ben Rejeb', 'Maitre de ConfÃ©rences', 'sonia.benrejeb@supcom.tn', NULL, NULL, 'https://scholar.google.com/citations?hl=fr&user=11IR_fcAAAAJ&view_op=list_works&sortby=pubdate', NULL, 'Performance analysis, design and optimization of wireless mobile 5/6/7G and IOT networks, resources allocation, mobility and QoS management, edge networking and cloud native, network virtualization, softwarization and slicing, machine learning and artificial intelligence (ML/IA), intelligent autonomous systems, smart cities, quantum communications, blockchain, digital twin and cybersecurity.', 'Dr. Sonia BEN REJEB Associate Professor of Telecommunications at ISI/University of Tunis El Manar (UTM). Member in the research laboratory MEDIATRON at SUPCOM since 2000.', NULL),
      ('Mariem', 'Thaalbi', 'Maitre assistant', 'mariem.thaalbi@supcom.tn, 99935146', '99935146', 'https://tn.linkedin.com/in/mariem-thaalbi-2964121b', NULL, NULL, 'Routing, 5G and beyonds, Blockchain, IoT', 'Dr. Mariem Thaalbi is an IT Assistant Professor with over 14 years of experience. She holds a PhD in Communications and Information Technologies from SUPâ€™COM.', 'Dr. Thaalbi is contributing in supervising a Phd thesis on IoT environment'),
      ('Karima', 'Smida', 'Docteur', 'Karima.smida@supcom.tn', NULL, NULL, NULL, NULL, 'Network engineering', 'Enseignate technologue en TIC Ã  ISET charguia', 'Recherches au sein du Labo Mediatron'),
      ('Mouna', 'Abdelmoumen', 'MaÃ®tre Assistant', 'mouna.abdelmoumen@supcom.tn, mouna.abdelmoumen@fsb.ucar.tn', NULL, NULL, NULL, NULL, 'RÃ©seaux sans fil pour IoT', 'Mouna Abdelmoumen a obtenu le diplÃ´me dâ€™ingÃ©nieur suivi dâ€™un mastÃ¨re en TÃ©lÃ©communications suivi dâ€™une thÃ¨se de doctorat en TIC de lâ€™Ã©cole supÃ©rieure de tÃ©lÃ©communications de Tunis (SUPâ€™COM).', NULL),
      ('Mohamed', 'Siala', 'Professeur', 'mohamed.siala@supcom.tn, +216 98 64 73 98', '+216 98 64 73 98', 'https://www.linkedin.com/in/mohamed-siala-a636263b/', NULL, NULL, 'Waveform design for FBMC and OTFS, MS-PRS and NSM modulations for 6G and beyond, Localization using non-linear Kalman filtering', 'Mohamed Siala received his general engineering degree from Ã‰cole Polytechnique, Palaiseau, France, in 1988... Since 2001, he has been with SUPâ€™COM, Tunis, Tunisia, where he currently serves as a Full Professor.', 'Waveform design for OTFS and FBMC in a multiple access framework using the POPS algorithm, Optimization of NSM and MS-PRS modulations for 6G and beyond, Localization based on non-linear Kalman filtering'),
      ('Sami', 'Tabbane', 'Professeur', 'sami.tabbane@supcom.tn', NULL, NULL, NULL, NULL, 'RÃ©seaux mobiles, IA pour les rÃ©seaux de tÃ©lÃ©communications', 'Sami Tabbane est ingÃ©nieur de lâ€™Ecole des Mines de Paris, Docteur TÃ©lÃ©com Paris Tech et Professeur de lâ€™enseignement supÃ©rieur.', NULL),
      ('Nihed', 'Bahria', 'Maitre Assistant', 'nihed.bahria@gmail.com', NULL, NULL, NULL, NULL, 'Cloud, security, IA, automation', 'Maitre assistant en Informatique Ã  Issat de Sousse. Laboratoire de recherche MEDIATRON SupCom.', NULL),
      ('Chouaib', 'Farhati', 'Professeur', 'chouaib.farhati@supcom.tn', NULL, NULL, NULL, NULL, 'communication sans fil, ia et traitement de signal', 'docteur en tic', 'ia pour traitement d''image'),
      ('MÃ¨riem', 'Afif', 'Maitre Assistant', 'meriem.afif@insat.ucar.tn , 98646288', '98646288', NULL, NULL, NULL, 'Gestion des ressources et de mobilitÃ© dans les rÃ©seaux radio-mobiles , optimisation de la QoS et de la QoE dans ces rÃ©seaux', 'MÃ©riem Afif, Ph.D. Associate Professor | National Institute of Applied Sciences and Technology (INSAT). She is a permanent researcher at the Mediatron laboratory.', 'PrÃ©paration de HDR'),
      ('Nawres', 'Abdelwahed', 'Assistante contractuelle', 'nawres.abdelwahed@supcom.tn', NULL, 'https://www.linkedin.com/in/nawres-abdelwahed-67aa24320?utm_source=share_via&utm_content=profile&utm_medium=member_android', NULL, NULL, 'IA, analyse des sentiments, NLP', 'I obtained my engineering degree from Enicarthage in 2014 and my PhD degree from SUPCom in 2017.', 'Ã‰crire des scientifiques en IA, NLP, Machine Learning'),
      ('Souhaila', 'Fki', 'Maitre assistante', 'feki.souhaila@gmail.com, 94472606', '94472606', 'https://www.linkedin.com/in/souhaila-fki-59a50235', NULL, NULL, 'Transmission numÃ©rique, traitement de signal, Ã©galisation de canaux de transmission', 'SOUHAILA FKI received the engineering diploma in digital communication systems from Institut Polytechnique de Bordeaux, France.', NULL),
      ('Amel', 'Haji', 'MaÃ®tre Assistante', 'amel.hajji@isgb.ucar.tn', NULL, 'https://www.linkedin.com/in/amel-haji-540a431b/', NULL, NULL, 'Cloud Computing', 'Dr. Amel Haji est MaÃ®tre-Assistante en informatique Ã  lâ€™Institut SupÃ©rieur de Gestion de Bizerte (ISG Bizerte), relevant de lâ€™UniversitÃ© de Carthage.', NULL),
      ('Maroua', 'Nouioua', 'Maitre assistant', 'maroua.nouioua@supcom.tn', NULL, NULL, NULL, NULL, 'IA, Cloud, Security', 'Maitre assistant et encadrante professionnelle associÃ©e au Laboratoire MEDIATRON.', NULL);

-- Insert missing scientific profiles without creating user/login accounts.
INSERT INTO members (
    first_name,
    last_name,
    grade,
    lab,
    bio,
    phone,
    linkedin_url,
    google_scholar_url,
    personal_site_url,
    created_at,
    updated_at
)
SELECT
    t.first_name,
    t.last_name,
    COALESCE(NULLIF(t.grade, ''), 'Member'),
    'MEDIATRON',
    NULLIF(t.bio, ''),
    NULLIF(t.contact_public, ''),
    NULLIF(t.linkedin_url, ''),
    NULLIF(t.google_scholar_url, ''),
    NULLIF(t.personal_site_url, ''),
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM tmp_mediatron_member_profile_form t
WHERE NOT EXISTS (
    SELECT 1
    FROM members m
    WHERE (
              (
                  pg_temp.norm_mediatron_name(m.first_name) = pg_temp.norm_mediatron_name(t.first_name)
                      AND pg_temp.norm_mediatron_name(m.last_name) = pg_temp.norm_mediatron_name(t.last_name)
                  )
                  OR (
                  pg_temp.norm_mediatron_name(m.first_name) = pg_temp.norm_mediatron_name(t.last_name)
                      AND pg_temp.norm_mediatron_name(m.last_name) = pg_temp.norm_mediatron_name(t.first_name)
                  )
              )
);

-- Enrich existing profiles. For login-linked members, keep phone as phone-only when possible
-- because their email already comes from the users table. For scientific-only profiles,
-- contact_public is stored in phone so it can be displayed by the existing public DTO.
UPDATE members m
SET
    grade = COALESCE(NULLIF(t.grade, ''), m.grade),
    bio = COALESCE(NULLIF(t.bio, ''), m.bio),
    phone = CASE
                WHEN m.user_id IS NULL THEN COALESCE(NULLIF(t.contact_public, ''), m.phone)
                ELSE COALESCE(NULLIF(t.phone_only, ''), m.phone)
        END,
    linkedin_url = COALESCE(NULLIF(t.linkedin_url, ''), m.linkedin_url),
    google_scholar_url = COALESCE(NULLIF(t.google_scholar_url, ''), m.google_scholar_url),
    personal_site_url = COALESCE(NULLIF(t.personal_site_url, ''), m.personal_site_url),
    updated_at = CURRENT_TIMESTAMP
    FROM tmp_mediatron_member_profile_form t
WHERE (
    (
    pg_temp.norm_mediatron_name(m.first_name) = pg_temp.norm_mediatron_name(t.first_name)
  AND pg_temp.norm_mediatron_name(m.last_name) = pg_temp.norm_mediatron_name(t.last_name)
    )
   OR (
    pg_temp.norm_mediatron_name(m.first_name) = pg_temp.norm_mediatron_name(t.last_name)
  AND pg_temp.norm_mediatron_name(m.last_name) = pg_temp.norm_mediatron_name(t.first_name)
    )
    );

-- Replace old/generated research-axis rows for these official form respondents with the cleaner
-- self-declared axes from the website form.
DELETE FROM member_research_axes ra
    USING members m, tmp_mediatron_member_profile_form t
WHERE ra.member_id = m.id
  AND t.research_axes IS NOT NULL
  AND (
    (
    pg_temp.norm_mediatron_name(m.first_name) = pg_temp.norm_mediatron_name(t.first_name)
  AND pg_temp.norm_mediatron_name(m.last_name) = pg_temp.norm_mediatron_name(t.last_name)
    )
   OR (
    pg_temp.norm_mediatron_name(m.first_name) = pg_temp.norm_mediatron_name(t.last_name)
  AND pg_temp.norm_mediatron_name(m.last_name) = pg_temp.norm_mediatron_name(t.first_name)
    )
    );

INSERT INTO member_research_axes (member_id, research_axes)
SELECT DISTINCT
    m.id,
    trim(regexp_replace(axis.value, '^[-–—• ]+', '', 'g')) AS research_axes
FROM tmp_mediatron_member_profile_form t
         JOIN members m
              ON (
                  (
                      pg_temp.norm_mediatron_name(m.first_name) = pg_temp.norm_mediatron_name(t.first_name)
                          AND pg_temp.norm_mediatron_name(m.last_name) = pg_temp.norm_mediatron_name(t.last_name)
                      )
                      OR (
                      pg_temp.norm_mediatron_name(m.first_name) = pg_temp.norm_mediatron_name(t.last_name)
                          AND pg_temp.norm_mediatron_name(m.last_name) = pg_temp.norm_mediatron_name(t.first_name)
                      )
                  )
         CROSS JOIN LATERAL regexp_split_to_table(t.research_axes, E'\\n|;') AS axis(value)
WHERE t.research_axes IS NOT NULL
  AND trim(regexp_replace(axis.value, '^[-–—• ]+', '', 'g')) <> ''
  AND NOT EXISTS (
    SELECT 1
    FROM member_research_axes existing
    WHERE existing.member_id = m.id
  AND lower(existing.research_axes) = lower(trim(regexp_replace(axis.value, '^[-–—• ]+', '', 'g')))
    );
