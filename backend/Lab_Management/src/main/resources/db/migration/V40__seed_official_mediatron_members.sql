-- Official MEDIATRON active/candidate members from Scopus Data_LR_EDTIC.
-- This migration does not create login accounts. It creates scientific profiles only.

CREATE TEMP TABLE tmp_official_mediatron_members (
    first_name TEXT,
    last_name TEXT,
    grade TEXT,
    email TEXT,
    orcid_id TEXT,
    bio TEXT
) ON COMMIT DROP;

INSERT INTO tmp_official_mediatron_members (first_name, last_name, grade, email, orcid_id, bio) VALUES
('Mohamed', 'Siala', 'Professeur', 'mohamed.siala@supcom.tn', NULL, 'Speciality: Télécom | Scopus documents: 278'),
('Sami', 'Tabbane', 'Professeur', 'sami.tabbane@supcom.tn', NULL, 'Speciality: Télécom | Scopus documents: 270'),
('Fatma', 'Abdelkefi', 'Professeur', 'fatma.abdelkefi@supcom.tn', NULL, 'Speciality: Télécom | Scopus documents: 101'),
('Nabil', 'Tabbane', 'Professeur', 'nabil.tabbane@supcom.tn', NULL, 'Speciality: Télécom | Scopus documents: 87'),
('Belhassen', 'Zouari', 'Professeur', 'belhassen.zouari@supcom.tn', NULL, 'Speciality: Informatique | Scopus documents: 75'),
('Zied', 'Choukair', 'Professeur', 'z.choukair@supcom.tn', NULL, 'Speciality: Télécom | Scopus documents: 74'),
('Asma', 'Ben Letaifa', 'Maitre de Conférence', 'asma.benletaifa@supcom.tn', '0000-0002-1527-2557', 'Speciality: Télécom | Scopus documents: 73'),
('Soumaya', 'Hamouda', 'Professeur', 'soumaya.hamouda@supcom.tn', '0000-0003-0671-973X', 'Speciality: Télécom | Scopus documents: 57'),
('Sonia', 'Ben Rejeb', 'Maitre de Conférence', 'sonia.benrejeb@supcom.tn', '0000-0003-4331-0984', 'Speciality: Télécom | Scopus documents: 38'),
('Mohamed', 'Laaraiedh', 'Maitre-assistant', NULL, NULL, 'Speciality: Télécom | Scopus documents: 32'),
('Mouna', 'Zhioua Ghayet El', 'Maitre-assistant', 'ghayet.zhioua@supcom.tn', NULL, 'Speciality: Télécom | Scopus documents: 22'),
('Maha', 'Jebalia', 'Maitre-assistant', NULL, NULL, 'Speciality: Télécom | Scopus documents: 16'),
('Saloua', 'Hendaoui', 'Maitre-assistant', NULL, NULL, 'Speciality: Télécom | Scopus documents: 14'),
('Mariem', 'Thaalbi', 'Maitre-assistant', 'mariem.thaalbi@supcom.tn', NULL, 'Speciality: Informatique | Scopus documents: 13'),
('Hajer', 'Tounsi', 'Maitre-assistant', NULL, NULL, 'Speciality: Télécom | Scopus documents: 12'),
('Zeineb', 'Hraiech', 'Ingénieur', 'zeineb.hraiech@supcom.tn', NULL, 'Speciality: télécom | Scopus documents: 10'),
('Souhaila', 'Fki', 'Maitre-assistant', 'feki.souhaila@gmail.com', NULL, 'Speciality: télécom | Scopus documents: 10'),
('Raouia', 'Ayadi', 'Maitre-assistant', NULL, '0000-0003-1322-1825', 'Speciality: Télécom | Scopus documents: 9'),
('Amel', 'Haji', 'Maitre-assistant', 'amel.hajji@isgb.ucar.tn', NULL, 'Speciality: Télécom | Scopus documents: 9'),
('Olfa', 'Chabbouh', 'Maitre-assistant', NULL, NULL, 'Speciality: Télécom | Scopus documents: 9'),
('Jamila', 'Ben Slimane', 'Assistant', NULL, NULL, 'Scopus documents: 8'),
('Sassi', 'Maaloul', 'Maitre-assistant', NULL, NULL, 'Speciality: Télécom | Scopus documents: 8'),
('Nabil', 'Torjmen', 'Member', 'torjemen@topnet.tn', NULL, 'Scopus documents: 8'),
('Imen', 'Ben Chaabane', 'Maitre-assistant', NULL, NULL, 'Speciality: Télécom | Scopus documents: 8'),
('Amine', 'Besrour', 'Maitre-assistant', NULL, NULL, 'Speciality: Télécom | Scopus documents: 7'),
('Kaouther', 'Hedhli', 'Autres', NULL, NULL, 'Scopus documents: 7'),
('Soumaya', 'Dahi', 'Maitre-assistant', 'soumaya.dahi@supcom.tn', NULL, 'Speciality: télécom | Scopus documents: 7'),
('Bahria', 'El Asghar Nihed', 'Maitre-assistant', 'nihed.bahria@gmail.com', NULL, 'Speciality: télécom | Scopus documents: 6'),
('Wafa', 'Khrouf', 'Ingénieur', 'wafa.khrouf@supcom.tn', '0000-0002-8189-1954', 'Scopus documents: 6'),
('Tarek', 'Bchini', 'Maitre-assistant', NULL, NULL, 'Speciality: Télécom | Scopus documents: 6'),
('Mouna', 'Abdelmoumen', 'Maitre-Assistant', 'mouna.abdelmoumen@gmail.com', NULL, 'Speciality: télécom | Scopus documents: 5'),
('Karima', 'Smida', 'Technologue en informatqiue', 'karima.smida@supcom.tn', NULL, 'Speciality: télécom | Scopus documents: 5'),
('Emna', 'Daknou', 'Ingénieur', 'emna.daknou@supcom.tn', NULL, 'Speciality: Télécom | Scopus documents: 4'),
('Raïssa', 'Djouela Kamgang Ines', 'ASSISTANT', 'ines.djouela@supcom.tn', NULL, 'Speciality: télécom | Scopus documents: 4'),
('Chouaib', 'Farhati', 'Member', 'chouaib.farhati@supcom.tn', NULL, 'Speciality: télécom | Scopus documents: 4'),
('Maroua', 'Nouioua', 'Maitre-assistant', 'maroua.nouiouasupcom.tn', NULL, 'Speciality: Télécom | Scopus documents: 4'),
('Marwa', 'Salhi', 'Member', 'maroua.salhi@supcom.tn', NULL, 'Scopus documents: 3'),
('Mihia', 'Kassi', 'Member', 'mihia.kassi@supcom.tn', NULL, 'Speciality: télécom | Scopus documents: 3'),
('Walid', 'Messaoud', 'Member', 'walid.messaoud@supcom.tn', NULL, 'Scopus documents: 3'),
('Souhaila', 'Feki', 'Maitre-assistant', NULL, NULL, 'Speciality: Télécom | Scopus documents: 2'),
('Tarek', 'Bejaoui', 'Maitre-assistant', NULL, NULL, 'Speciality: Télécom'),
('Mohamed', 'Ayadi', 'Maitre de Conférence', 'm.ayadi@supcom.tn', NULL, 'Speciality: Télécom'),
('Mariem', 'Afif', 'Maitre-assistant', NULL, NULL, 'Speciality: Télécom'),
('Sinda', 'Boussen', 'Maitre-assistant', 'sinda_boussen@yahoo.fr', NULL, 'Speciality: Télécom'),
('Soumaya', 'Guedidi Dahi', 'Maitre-assistant', NULL, NULL, 'Speciality: Informatique'),
('Mariem', 'Ayadi', 'Maitre-assistant', NULL, NULL, 'Speciality: Télécom'),
('Olfa', 'Ben Ahmed', 'Maitre-assistant', 'olfa.benahmed@supcom.tn', NULL, 'Speciality: Télécom'),
('Wiem', 'Abderrahim', 'Maitre-assistant', 'wiem.abderrahim@supcom.tn', NULL, 'Speciality: télécom'),
('Amel', 'Ksentini', 'Member', 'amel.ksentini@supcom.tn', NULL, NULL),
('Mounir', 'Frikha', 'Professeur', 'M.Frikha@supcom.tn', NULL, 'Speciality: Télécom'),
('Chiheb', 'Abid', 'Maitre-assistant', 'chiheb.abid@fst.utm.tn', NULL, 'Speciality: Télécom'),
('Ngaffo', 'Armielle', 'Maitre-assistant', NULL, NULL, 'Speciality: Informatique'),
('Nihed', 'Bahria', 'Maitre-assistant', NULL, NULL, 'Speciality: Télécom'),
('Khaled', 'Barbaria', 'Maitre-assistant', NULL, NULL, 'Speciality: Informatique'),
('Souhaila', 'Ben Rehaim', 'Maitre-assistant', NULL, NULL, 'Speciality: Télécom'),
('Sajeh', 'Zairi', 'Maitre-assistant', 'sajeh.zairi@esen.tn', NULL, 'Speciality: Informatique'),
('Hafedh', 'Hrizi', 'Maitre-assistant', NULL, NULL, 'Speciality: Télécom'),
('Rihab', 'Khemiri', 'Maitre-assistant', NULL, NULL, 'Speciality: Informatique'),
('Neila', 'Krichene Mejdoub', 'Maitre-assistant', NULL, NULL, 'Speciality: Télécom'),
('Aymen', 'Massoudi', 'Maitre-assistant', NULL, NULL, 'Speciality: Télécom'),
('Imen', 'Allani', 'Ingénieur', NULL, NULL, 'Speciality: Informatique'),
('David', 'N''Dri Kouame', 'Member', 'kouame.ndri@supcom.tn', NULL, NULL),
('Désiré', 'Koné Kigninman', 'Member', 'desire.kone@esatic.edu.ci', NULL, NULL),
('Abar', 'Tasnim', 'Maitre-Assistant', 'tasnim.abar@supcom.tn', NULL, 'Speciality: télécom');

INSERT INTO members (first_name, last_name, grade, lab, orcid_id, bio, created_at, updated_at)
SELECT t.first_name, t.last_name, COALESCE(NULLIF(t.grade, ''), 'Member'), 'MEDIATRON', NULLIF(t.orcid_id, ''), NULLIF(t.bio, ''), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM tmp_official_mediatron_members t
WHERE NOT EXISTS (
    SELECT 1 FROM members m
    WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = LOWER(regexp_replace(t.first_name, '[[:space:]]+', '', 'g'))
      AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = LOWER(regexp_replace(t.last_name, '[[:space:]]+', '', 'g'))
);

-- Add research axes when the optional member_research_axes table exists.
DO $$
BEGIN
    IF to_regclass('public.member_research_axes') IS NOT NULL THEN
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Computer ScienceEngineeringMathematicsSocial SciencesPhysics and AstronomyMaterials ScienceDecision SciencesEnergyBusiness' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'mohamed' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'siala'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Computer ScienceEngineeringMathematicsSocial SciencesPhysics and AstronomyMaterials ScienceDecision SciencesEnergyBusiness'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Management and AccountingChemistry' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'mohamed' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'siala'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Management and AccountingChemistry'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Computer Science Engineering Mathematics Social Sciences Decision Sciences Business' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'sami' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'tabbane'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Computer Science Engineering Mathematics Social Sciences Decision Sciences Business'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Management and Accounting Multidisciplinary Biochemistry' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'sami' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'tabbane'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Management and Accounting Multidisciplinary Biochemistry'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Genetics and Molecular Biology' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'sami' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'tabbane'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Genetics and Molecular Biology'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Computer Science Engineering Mathematics Physics and Astronomy Materials Science Social Sciences Energy Business' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'fatma' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'abdelkefi'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Computer Science Engineering Mathematics Physics and Astronomy Materials Science Social Sciences Energy Business'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Management and Accounting Environmental Science Chemistry' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'fatma' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'abdelkefi'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Management and Accounting Environmental Science Chemistry'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Computer Science Engineering Mathematics Social Sciences Decision Sciences' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'nabil' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'tabbane'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Computer Science Engineering Mathematics Social Sciences Decision Sciences'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Computer Science Engineering Mathematics Business' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'belhassen' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'zouari'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Computer Science Engineering Mathematics Business'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Management and Accounting Decision Sciences Energy Earth and Planetary Sciences Chemical Engineering Social Sciences' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'belhassen' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'zouari'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Management and Accounting Decision Sciences Energy Earth and Planetary Sciences Chemical Engineering Social Sciences'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Computer Science Engineering Mathematics Decision Sciences Materials Science Economics' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'zied' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'choukair'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Computer Science Engineering Mathematics Decision Sciences Materials Science Economics'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Econometrics and Finance Social Sciences' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'zied' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'choukair'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Econometrics and Finance Social Sciences'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Computer Science Engineering Decision Sciences Mathematics Social Sciences Medicine Materials Science Energy Business' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'asma' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'benletaifa'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Computer Science Engineering Decision Sciences Mathematics Social Sciences Medicine Materials Science Energy Business'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Management and Accounting Multidisciplinary' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'asma' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'benletaifa'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Management and Accounting Multidisciplinary'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Computer Science Engineering Mathematics Social Sciences Decision Sciences Materials Science Medicine Physics and Astronomy Energy Business' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'soumaya' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'hamouda'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Computer Science Engineering Mathematics Social Sciences Decision Sciences Materials Science Medicine Physics and Astronomy Energy Business'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Management and Accounting' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'soumaya' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'hamouda'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Management and Accounting'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Computer Science Engineering Mathematics Decision Sciences Materials Science Social Sciences Biochemistry' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'sonia' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'benrejeb'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Computer Science Engineering Mathematics Decision Sciences Materials Science Social Sciences Biochemistry'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Genetics and Molecular Biology' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'sonia' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'benrejeb'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Genetics and Molecular Biology'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Computer Science Engineering Mathematics Social Sciences Physics and Astronomy Energy' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'mohamed' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'laaraiedh'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Computer Science Engineering Mathematics Social Sciences Physics and Astronomy Energy'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Computer Science Engineering Mathematics Social Sciences Decision Sciences' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'mouna' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'zhiouaghayetel'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Computer Science Engineering Mathematics Social Sciences Decision Sciences'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Computer Science Engineering Mathematics Business' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'maha' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'jebalia'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Computer Science Engineering Mathematics Business'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Management and Accounting' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'maha' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'jebalia'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Management and Accounting'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Computer Science Engineering Decision Sciences Physics and Astronomy Mathematics Business' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'saloua' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'hendaoui'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Computer Science Engineering Decision Sciences Physics and Astronomy Mathematics Business'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Management and Accounting Social Sciences' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'saloua' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'hendaoui'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Management and Accounting Social Sciences'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Computer Science Engineering Social Sciences Mathematics' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'mariem' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'thaalbi'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Computer Science Engineering Social Sciences Mathematics'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Computer Science Engineering Mathematics Social Sciences Decision Sciences' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'hajer' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'tounsi'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Computer Science Engineering Mathematics Social Sciences Decision Sciences'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Engineering Computer Science Mathematics Physics and Astronomy' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'zeineb' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'hraiech'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Engineering Computer Science Mathematics Physics and Astronomy'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Computer Science Engineering Mathematics Physics and Astronomy' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'souhaila' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'fki'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Computer Science Engineering Mathematics Physics and Astronomy'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Computer Science Engineering Social Sciences' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'raouia' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'ayadi'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Computer Science Engineering Social Sciences'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Computer Science Decision Sciences Engineering Mathematics' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'amel' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'haji'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Computer Science Decision Sciences Engineering Mathematics'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Computer Science Engineering Mathematics Materials Science Decision Sciences' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'olfa' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'chabbouh'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Computer Science Engineering Mathematics Materials Science Decision Sciences'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Computer Science Engineering Social Sciences Mathematics Medicine' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'jamila' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'benslimane'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Computer Science Engineering Social Sciences Mathematics Medicine'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Computer Science Engineering Social Sciences Mathematics' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'sassi' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'maaloul'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Computer Science Engineering Social Sciences Mathematics'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Computer Science Engineering Mathematics Social Sciences Decision Sciences' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'nabil' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'torjmen'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Computer Science Engineering Mathematics Social Sciences Decision Sciences'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Computer Science Engineering Mathematics' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'imen' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'benchaabane'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Computer Science Engineering Mathematics'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Computer Science Engineering Mathematics Physics and Astronomy Materials Science' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'amine' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'besrour'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Computer Science Engineering Mathematics Physics and Astronomy Materials Science'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Computer Science Mathematics Energy' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'kaouther' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'hedhli'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Computer Science Mathematics Energy'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Computer Science Engineering' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'soumaya' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'dahi'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Computer Science Engineering'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Computer Science Engineering Social Sciences Mathematics Decision Sciences' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'bahria' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'elasgharnihed'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Computer Science Engineering Social Sciences Mathematics Decision Sciences'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Computer Science Engineering Mathematics Physics and Astronomy Materials Science' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'wafa' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'khrouf'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Computer Science Engineering Mathematics Physics and Astronomy Materials Science'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Computer Science Engineering' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'tarek' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'bchini'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Computer Science Engineering'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Computer Science Engineering' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'mouna' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'abdelmoumen'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Computer Science Engineering'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Computer Science Engineering Mathematics' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'karima' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'smida'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Computer Science Engineering Mathematics'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Computer Science Engineering' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'emna' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'daknou'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Computer Science Engineering'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Computer Science Engineering Mathematics' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'raissa' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'djouelakamgangines'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Computer Science Engineering Mathematics'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Computer Science Engineering Physics and Astronomy Materials Science Mathematics' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'chouaib' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'farhati'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Computer Science Engineering Physics and Astronomy Materials Science Mathematics'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Computer Science Engineering Business' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'maroua' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'nouioua'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Computer Science Engineering Business'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Management and Accounting' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'maroua' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'nouioua'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Management and Accounting'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Computer Science Mathematics' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'marwa' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'salhi'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Computer Science Mathematics'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Computer Science Mathematics Engineering' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'mihia' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'kassi'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Computer Science Mathematics Engineering'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Computer Science Engineering Mathematics' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'walid' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'messaoud'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Computer Science Engineering Mathematics'));
        INSERT INTO member_research_axes (member_id, research_axes)
        SELECT id, 'Engineering Materials Science Computer Science' FROM members m WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = 'souhaila' AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = 'feki'
        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER('Engineering Materials Science Computer Science'));
    END IF;
END $$;