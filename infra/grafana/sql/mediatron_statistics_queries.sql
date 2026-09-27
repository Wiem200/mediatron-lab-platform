-- MEDIATRON Statistical Dashboards - optimized PostgreSQL queries
-- These queries assume the Flyway-managed schema used by the MEDIATRON platform.
-- Existing supporting indexes:
--   publications(publication_year)
--   publications(type)
--   publications(status)
-- Additional dashboard indexes are created in V24__statistics_indexes.sql.

-- 1. Publication count per year
SELECT
  make_date(p.publication_year, 1, 1) AS "time",
  COUNT(*)::double precision AS publications
FROM publications p
WHERE p.publication_year IS NOT NULL
  AND p.status = 'APPROVED'
GROUP BY 1
ORDER BY 1;

-- 2. Journal vs Conference distribution
SELECT
  category,
  COUNT(*)::double precision AS publications
FROM (
  SELECT CASE
    WHEN p.type = 'JOURNAL' THEN 'Journal'
    WHEN p.type IN ('CONFERENCE_A', 'CONFERENCE_B', 'CONFERENCE_C')
      OR COALESCE(p.indexation, '') ILIKE '%national%'
      OR COALESCE(p.source, '') ILIKE '%national%'
      OR p.type = 'WORKSHOP'
    THEN 'Conference'
    ELSE NULL
  END AS category
  FROM publications p
  WHERE p.status = 'APPROVED'
) distribution
WHERE category IS NOT NULL
GROUP BY category
ORDER BY category;

-- 3. Conference classification breakdown (A/B/C/National)
SELECT
  classification,
  COUNT(*)::double precision AS publications
FROM (
  SELECT CASE
    WHEN p.type = 'CONFERENCE_A' THEN 'A'
    WHEN p.type = 'CONFERENCE_B' THEN 'B'
    WHEN p.type = 'CONFERENCE_C' THEN 'C'
    WHEN COALESCE(p.indexation, '') ILIKE '%national%'
      OR COALESCE(p.source, '') ILIKE '%national%'
      OR p.type = 'WORKSHOP'
    THEN 'National'
    ELSE NULL
  END AS classification
  FROM publications p
  WHERE p.status = 'APPROVED'
) conference_breakdown
WHERE classification IS NOT NULL
GROUP BY classification
ORDER BY CASE classification WHEN 'A' THEN 1 WHEN 'B' THEN 2 WHEN 'C' THEN 3 ELSE 4 END;

-- 4. Evolution by member
SELECT
  make_date(p.publication_year, 1, 1) AS "time",
  TRIM(CONCAT(m.first_name, ' ', m.last_name)) AS member_name,
  COUNT(*)::double precision AS publications
FROM publications p
JOIN members_publications mp ON mp.publication_id = p.id
JOIN members m ON m.id = mp.member_id
WHERE p.publication_year IS NOT NULL
  AND p.status = 'APPROVED'
GROUP BY 1, 2
ORDER BY 1, 2;

-- 5. Heatmap matrix by member and year
SELECT
  TRIM(CONCAT(m.first_name, ' ', m.last_name)) AS member_name,
  p.publication_year,
  COUNT(*)::double precision AS publications
FROM publications p
JOIN members_publications mp ON mp.publication_id = p.id
JOIN members m ON m.id = mp.member_id
WHERE p.publication_year IS NOT NULL
  AND p.status = 'APPROVED'
GROUP BY 1, 2
ORDER BY 1, 2;
