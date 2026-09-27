#!/usr/bin/env python3
"""
Generate Flyway SQL migrations for official MEDIATRON data from the two Excel files.
No external Python packages are required. It reads .xlsx files using the standard library.

Usage from backend/Lab_Management:
  python3 tools/generate_mediatron_import_sql.py

Expected files:
  import-data/liste d'inscription et pré-inscription 25-26-Médiatron.xlsx
  import-data/Liste_article_supcom_scopus-Mediatron.xlsx
"""
from __future__ import annotations

import re
import sys
import unicodedata
import xml.etree.ElementTree as ET
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path
from posixpath import normpath
from zipfile import ZipFile

XLSX_NS = {"a": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}

PHD_FILE_NAME = "liste d'inscription et pré-inscription 25-26-Médiatron.xlsx"
SCOPUS_FILE_NAME = "Liste_article_supcom_scopus-Mediatron.xlsx"


def clean(value) -> str:
    if value is None:
        return ""
    value = str(value).replace("\u00a0", " ").replace("\t", " ")
    value = re.sub(r"\s+", " ", value).strip()
    return value


def normalize_key(value) -> str:
    value = clean(value).lower()
    value = "".join(ch for ch in unicodedata.normalize("NFKD", value) if not unicodedata.combining(ch))
    value = re.sub(r"[^a-z0-9]+", "", value)
    return value


def sql(value) -> str:
    value = clean(value)
    if value == "":
        return "NULL"
    return "'" + value.replace("'", "''") + "'"


def sql_int(value) -> str:
    value = clean(value)
    if value == "":
        return "NULL"
    m = re.search(r"\d{4}", value)
    if m:
        return str(int(m.group(0)))
    if value.isdigit():
        return str(int(value))
    return "NULL"


def norm_doi(value) -> str:
    value = clean(value).lower()
    value = re.sub(r"^https?://(dx\.)?doi\.org/", "", value)
    value = value.strip().strip(".")
    return value


def doi_url(doi: str, fallback: str = "") -> str:
    doi = norm_doi(doi)
    if doi:
        return "https://doi.org/" + doi
    return clean(fallback)


def map_publication_type(document_type: str, source_title: str = "") -> str:
    doc = clean(document_type).lower()
    source = clean(source_title).lower()
    if "article" in doc and "conference" not in doc:
        return "JOURNAL"
    if "book" in doc and "chapter" in doc:
        return "BOOK_CHAPTER"
    if "conference" in doc:
        return "CONFERENCE_C"
    if "workshop" in source:
        return "WORKSHOP"
    return "OTHER"


def split_source_name(name: str) -> tuple[str, str]:
    """Most source names are 'LastName FirstName'. Return (first_name, last_name)."""
    name = clean(name)
    if not name:
        return "", ""
    tokens = name.split()
    if len(tokens) == 1:
        return tokens[0].title(), ""
    first = tokens[-1].title()
    last = " ".join(tokens[:-1]).title()
    return first, last


def split_supervisor_name(name: str) -> tuple[str, str]:
    """Supervisor names often look like 'Asma BEN LETAIFA'. Return (first_name, last_name)."""
    name = clean(name)
    if not name:
        return "", ""
    # Remove obvious separators/extra notes.
    name = re.split(r"[+/;]", name)[0].strip()
    tokens = name.split()
    if len(tokens) == 1:
        return tokens[0].title(), ""
    # In the PhD file, the first token is usually first name and the remaining tokens are last name.
    first = tokens[0].title()
    last = " ".join(tokens[1:]).title()
    return first, last


def worksheet_rows(path: Path, sheet_name: str) -> list[list[str]]:
    z = ZipFile(path)
    shared = []
    if "xl/sharedStrings.xml" in z.namelist():
        root = ET.fromstring(z.read("xl/sharedStrings.xml"))
        for si in root.findall("a:si", XLSX_NS):
            shared.append("".join(t.text or "" for t in si.iter("{http://schemas.openxmlformats.org/spreadsheetml/2006/main}t")))

    workbook = ET.fromstring(z.read("xl/workbook.xml"))
    rels = ET.fromstring(z.read("xl/_rels/workbook.xml.rels"))
    rel_targets = {}
    for rel in rels:
        target = rel.attrib["Target"]
        if target.startswith("/"):
            resolved = target.lstrip("/")
        else:
            resolved = normpath("xl/" + target)
        rel_targets[rel.attrib["Id"]] = resolved

    target = None
    for sheet in workbook.find("a:sheets", XLSX_NS):
        if sheet.attrib["name"] == sheet_name:
            rid = sheet.attrib["{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id"]
            target = rel_targets[rid]
            break

    if target is None:
        raise ValueError(f"Sheet {sheet_name!r} not found in {path}")

    root = ET.fromstring(z.read(target))

    def col_index(ref: str) -> int:
        m = re.match(r"([A-Z]+)", ref or "")
        if not m:
            return 0
        n = 0
        for ch in m.group(1):
            n = n * 26 + ord(ch) - 64
        return n - 1

    rows = []
    for row in root.findall(".//a:sheetData/a:row", XLSX_NS):
        values = []
        for cell in row.findall("a:c", XLSX_NS):
            idx = col_index(cell.attrib.get("r", ""))
            while len(values) <= idx:
                values.append("")
            ctype = cell.attrib.get("t")
            v = cell.find("a:v", XLSX_NS)
            value = ""
            if v is not None:
                value = v.text or ""
                if ctype == "s":
                    value = shared[int(value)]
            elif ctype == "inlineStr":
                inline = cell.find("a:is", XLSX_NS)
                if inline is not None:
                    value = "".join(t.text or "" for t in inline.iter("{http://schemas.openxmlformats.org/spreadsheetml/2006/main}t"))
            values[idx] = clean(value)
        rows.append(values)
    return rows


@dataclass
class MemberCandidate:
    first_name: str
    last_name: str
    grade: str
    email: str
    orcid_id: str
    bio: str
    scopus_ids: tuple[str, ...]
    research_axes: tuple[str, ...]


@dataclass
class PhDStudentCandidate:
    first_name: str
    last_name: str
    topic: str
    registration_year: str
    supervisor_first: str
    supervisor_last: str
    co_supervisor: str
    note: str


@dataclass
class PublicationCandidate:
    source_id: str
    title: str
    year: str
    authors_text: str
    source_title: str
    doi: str
    external_url: str
    document_type: str
    publication_type: str
    author_ids: tuple[str, ...]


def read_members(scopus_path: Path) -> tuple[list[MemberCandidate], dict[str, MemberCandidate]]:
    rows = worksheet_rows(scopus_path, "Data_LR_EDTIC")
    header = {clean(h): i for i, h in enumerate(rows[0])}

    def get(row, col):
        i = header.get(col)
        return clean(row[i]) if i is not None and i < len(row) else ""

    members = []
    scopus_to_member = {}
    seen_names = set()

    for row in rows[1:]:
        if get(row, "Laboratoire").upper() != "MEDIATRON":
            continue

        full_name = get(row, "Noms & Prénoms")
        grade = get(row, "Grade") or "Member"
        email = get(row, "email")
        # Active directory candidate heuristic: official grade OR email exists.
        # This keeps the public directory near the official human resources size.
        if not clean(get(row, "Grade")) and not email:
            continue

        first, last = split_source_name(full_name)
        if not first or not last:
            continue

        name_key = normalize_key(first + " " + last)
        if name_key in seen_names:
            continue
        seen_names.add(name_key)

        scopus_ids = []
        for col in ("Scopus Author ID", "Scopus Author ID_2", "Scopus Author ID_3"):
            value = get(row, col)
            if value:
                scopus_ids.append(value)

        themes = get(row, "Thématiques de recherche")
        axes = tuple(x.strip() for x in re.split(r"[;,]", themes) if x.strip())[:5]
        bio_parts = []
        if get(row, "Spécialité (recrutement)"):
            bio_parts.append("Speciality: " + get(row, "Spécialité (recrutement)"))
        if get(row, "Number of Documents"):
            bio_parts.append("Scopus documents: " + get(row, "Number of Documents"))
        bio = " | ".join(bio_parts)

        member = MemberCandidate(
            first_name=first,
            last_name=last,
            grade=grade,
            email=email,
            orcid_id=get(row, "ORCID ID"),
            bio=bio,
            scopus_ids=tuple(scopus_ids),
            research_axes=axes,
        )
        members.append(member)
        for sid in scopus_ids:
            scopus_to_member[sid] = member

    return members, scopus_to_member


def read_phd_students(phd_path: Path) -> list[PhDStudentCandidate]:
    rows = worksheet_rows(phd_path, "Sheet1")
    header_row_index = None
    for i, row in enumerate(rows):
        if row and clean(row[0]) == "N°":
            header_row_index = i
            break
    if header_row_index is None:
        raise ValueError("Could not find PhD header row")

    headers = {clean(h): idx for idx, h in enumerate(rows[header_row_index])}

    def get(row, col):
        i = headers.get(col)
        return clean(row[i]) if i is not None and i < len(row) else ""

    students = []
    for row in rows[header_row_index + 1:]:
        if not row or not get(row, "Doctorant"):
            continue
        first = clean(get(row, "Doctorant")).title()
        last = clean(get(row, "Nom")).title()
        topic = get(row, "Sujet de thèse")
        supervisor = get(row, "Directeur de thèse")
        supervisor_first, supervisor_last = split_supervisor_name(supervisor)
        co = get(row, "Cotutelle/Co-direction")
        # extra note is usually the first cell after Nationalité when present
        note = clean(row[12]) if len(row) > 12 else ""
        students.append(PhDStudentCandidate(
            first_name=first,
            last_name=last,
            topic=topic,
            registration_year=get(row, "Date de la 1ère inscription"),
            supervisor_first=supervisor_first,
            supervisor_last=supervisor_last,
            co_supervisor=co,
            note=note,
        ))
    return students


def read_publications(scopus_path: Path, scopus_to_member: dict[str, MemberCandidate]) -> tuple[list[PublicationCandidate], list[tuple[PublicationCandidate, MemberCandidate]]]:
    rows = worksheet_rows(scopus_path, "scopus_vf")
    headers = {clean(h): idx for idx, h in enumerate(rows[0])}

    def get(row, col):
        i = headers.get(col)
        return clean(row[i]) if i is not None and i < len(row) else ""

    publication_by_key = {}
    links = []

    for row in rows[1:]:
        raw_author_ids = get(row, "Author(s) ID")
        author_ids = tuple(x.strip() for x in raw_author_ids.split(";") if x.strip())
        matched_members = []
        seen_member_keys = set()
        for author_id in author_ids:
            member = scopus_to_member.get(author_id)
            if member is None:
                continue
            key = normalize_key(member.first_name + member.last_name)
            if key in seen_member_keys:
                continue
            seen_member_keys.add(key)
            matched_members.append(member)

        if not matched_members:
            continue

        title = get(row, "Title")
        if not title:
            continue
        year = get(row, "Year")
        doi = norm_doi(get(row, "DOI"))
        key = "doi:" + doi if doi else "title:" + normalize_key(title) + ":" + year

        if key not in publication_by_key:
            doc_type = get(row, "Document Type")
            pub = PublicationCandidate(
                source_id=get(row, "ID_publication"),
                title=title,
                year=year,
                authors_text=get(row, "Authors (Voir colonne V et plus,,,,)") or get(row, "Author full names"),
                source_title=get(row, "Source title"),
                doi=doi,
                external_url=doi_url(doi, get(row, "Link")),
                document_type=doc_type,
                publication_type=map_publication_type(doc_type, get(row, "Source title")),
                author_ids=author_ids,
            )
            publication_by_key[key] = pub
        else:
            pub = publication_by_key[key]

        for member in matched_members:
            links.append((pub, member))

    publications = list(publication_by_key.values())
    publications.sort(key=lambda p: (-(int(p.year) if p.year.isdigit() else 0), p.title.lower()))
    links.sort(key=lambda pair: (pair[0].title.lower(), pair[1].last_name.lower(), pair[1].first_name.lower()))
    return publications, links


def next_version(migration_dir: Path) -> int:
    versions = []
    for path in migration_dir.glob("V*__*.sql"):
        m = re.match(r"V(\d+)__", path.name)
        if m:
            versions.append(int(m.group(1)))
    return max(versions or [0]) + 1


def member_match_sql(first: str, last: str, alias="m") -> str:
    # Use regexp_replace so spacing/case differences do not create duplicates.
    return (
        f"LOWER(regexp_replace({alias}.first_name, '[[:space:]]+', '', 'g')) = {sql(normalize_key(first))} "
        f"AND LOWER(regexp_replace({alias}.last_name, '[[:space:]]+', '', 'g')) = {sql(normalize_key(last))}"
    )


def pub_match_sql(pub: PublicationCandidate, alias="p") -> str:
    if pub.doi:
        return f"LOWER({alias}.doi) = {sql(pub.doi)}"
    return f"LOWER({alias}.title) = LOWER({sql(pub.title)}) AND {alias}.publication_year = {sql_int(pub.year)}"


def write_members_sql(path: Path, members: list[MemberCandidate]) -> None:
    lines = []
    lines.append("-- Official MEDIATRON active/candidate members from Scopus Data_LR_EDTIC.")
    lines.append("-- This migration does not create login accounts. It creates scientific profiles only.")
    lines.append("")
    lines.append("CREATE TEMP TABLE tmp_official_mediatron_members (")
    lines.append("    first_name TEXT,")
    lines.append("    last_name TEXT,")
    lines.append("    grade TEXT,")
    lines.append("    email TEXT,")
    lines.append("    orcid_id TEXT,")
    lines.append("    bio TEXT")
    lines.append(") ON COMMIT DROP;")
    lines.append("")
    chunk = []
    for m in members:
        chunk.append(f"({sql(m.first_name)}, {sql(m.last_name)}, {sql(m.grade)}, {sql(m.email)}, {sql(m.orcid_id)}, {sql(m.bio)})")
    lines.append("INSERT INTO tmp_official_mediatron_members (first_name, last_name, grade, email, orcid_id, bio) VALUES")
    lines.append(",\n".join(chunk) + ";")
    lines.append("")
    lines.append("INSERT INTO members (first_name, last_name, grade, lab, orcid_id, bio, created_at, updated_at)")
    lines.append("SELECT t.first_name, t.last_name, COALESCE(NULLIF(t.grade, ''), 'Member'), 'MEDIATRON', NULLIF(t.orcid_id, ''), NULLIF(t.bio, ''), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP")
    lines.append("FROM tmp_official_mediatron_members t")
    lines.append("WHERE NOT EXISTS (")
    lines.append("    SELECT 1 FROM members m")
    lines.append("    WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = LOWER(regexp_replace(t.first_name, '[[:space:]]+', '', 'g'))")
    lines.append("      AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = LOWER(regexp_replace(t.last_name, '[[:space:]]+', '', 'g'))")
    lines.append(");")
    lines.append("")
    lines.append("-- Add research axes when the optional member_research_axes table exists.")
    lines.append("DO $$")
    lines.append("BEGIN")
    lines.append("    IF to_regclass('public.member_research_axes') IS NOT NULL THEN")
    for m in members:
        for axis in m.research_axes:
            lines.append("        INSERT INTO member_research_axes (member_id, research_axes)")
            lines.append(f"        SELECT id, {sql(axis)} FROM members m WHERE {member_match_sql(m.first_name, m.last_name, 'm')}")
            lines.append(f"        AND NOT EXISTS (SELECT 1 FROM member_research_axes ra WHERE ra.member_id = m.id AND LOWER(ra.research_axes) = LOWER({sql(axis)}));")
    lines.append("    END IF;")
    lines.append("END $$;")
    path.write_text("\n".join(lines), encoding="utf-8")


def write_phd_sql(path: Path, students: list[PhDStudentCandidate]) -> None:
    lines = []
    lines.append("-- Official MEDIATRON PhD students from the 2025-2026 registration/pre-registration file.")
    lines.append("CREATE TEMP TABLE tmp_official_mediatron_phd_students (")
    lines.append("    first_name TEXT,")
    lines.append("    last_name TEXT,")
    lines.append("    thesis_topic TEXT,")
    lines.append("    registration_year INTEGER,")
    lines.append("    supervisor_first TEXT,")
    lines.append("    supervisor_last TEXT,")
    lines.append("    co_supervisor TEXT,")
    lines.append("    note TEXT")
    lines.append(") ON COMMIT DROP;")
    values = []
    for s in students:
        co_note = clean(" | ".join(x for x in [s.co_supervisor, s.note] if x))
        values.append(f"({sql(s.first_name)}, {sql(s.last_name)}, {sql(s.topic)}, {sql_int(s.registration_year)}, {sql(s.supervisor_first)}, {sql(s.supervisor_last)}, {sql(co_note)}, {sql(s.note)})")
    lines.append("INSERT INTO tmp_official_mediatron_phd_students (first_name, last_name, thesis_topic, registration_year, supervisor_first, supervisor_last, co_supervisor, note) VALUES")
    lines.append(",\n".join(values) + ";")
    lines.append("")
    lines.append("INSERT INTO phd_students (first_name, last_name, thesis_topic, registration_year, research_team, supervisor_id, co_supervisor)")
    lines.append("SELECT t.first_name, t.last_name, t.thesis_topic, t.registration_year, 'MEDIATRON',")
    lines.append("       (SELECT m.id FROM members m")
    lines.append("        WHERE LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = LOWER(regexp_replace(t.supervisor_first, '[[:space:]]+', '', 'g'))")
    lines.append("          AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = LOWER(regexp_replace(t.supervisor_last, '[[:space:]]+', '', 'g'))")
    lines.append("        ORDER BY m.id LIMIT 1),")
    lines.append("       NULLIF(t.co_supervisor, '')")
    lines.append("FROM tmp_official_mediatron_phd_students t")
    lines.append("WHERE NOT EXISTS (")
    lines.append("    SELECT 1 FROM phd_students p")
    lines.append("    WHERE LOWER(regexp_replace(p.first_name, '[[:space:]]+', '', 'g')) = LOWER(regexp_replace(t.first_name, '[[:space:]]+', '', 'g'))")
    lines.append("      AND LOWER(regexp_replace(p.last_name, '[[:space:]]+', '', 'g')) = LOWER(regexp_replace(t.last_name, '[[:space:]]+', '', 'g'))")
    lines.append("      AND LOWER(COALESCE(p.thesis_topic, '')) = LOWER(COALESCE(t.thesis_topic, ''))")
    lines.append(");")
    path.write_text("\n".join(lines), encoding="utf-8")


def write_publications_sql(path: Path, pubs: list[PublicationCandidate]) -> None:
    lines = []
    lines.append("-- Official MEDIATRON Scopus publication candidates linked to MEDIATRON active/candidate authors.")
    lines.append("-- Imported as APPROVED because the source is an official Scopus export.")
    lines.append("CREATE TEMP TABLE tmp_official_mediatron_publications (")
    lines.append("    title TEXT,")
    lines.append("    publication_year INTEGER,")
    lines.append("    authors_text TEXT,")
    lines.append("    source_title TEXT,")
    lines.append("    doi TEXT,")
    lines.append("    external_url TEXT,")
    lines.append("    document_type TEXT,")
    lines.append("    publication_type TEXT")
    lines.append(") ON COMMIT DROP;")
    values = []
    for p in pubs:
        values.append(f"({sql(p.title)}, {sql_int(p.year)}, {sql(p.authors_text)}, {sql(p.source_title)}, {sql(p.doi)}, {sql(p.external_url)}, {sql(p.document_type)}, {sql(p.publication_type)})")
    # Split into chunks to keep migration reasonably readable.
    chunk_size = 300
    for i in range(0, len(values), chunk_size):
        lines.append("")
        lines.append("INSERT INTO tmp_official_mediatron_publications (title, publication_year, authors_text, source_title, doi, external_url, document_type, publication_type) VALUES")
        lines.append(",\n".join(values[i:i+chunk_size]) + ";")
    lines.append("")
    lines.append("-- DOI-based import/update.")
    lines.append("INSERT INTO publications (title, doi, publication_year, authors_text, source, external_url, pdf_url, indexation, type, status, created_at, updated_at)")
    lines.append("SELECT t.title, NULLIF(t.doi, ''), t.publication_year, t.authors_text, 'Scopus', NULLIF(t.external_url, ''), NULL, NULLIF(t.source_title, ''), t.publication_type, 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP")
    lines.append("FROM tmp_official_mediatron_publications t")
    lines.append("WHERE NULLIF(t.doi, '') IS NOT NULL")
    lines.append("ON CONFLICT (doi) DO UPDATE SET")
    lines.append("    title = EXCLUDED.title,")
    lines.append("    publication_year = EXCLUDED.publication_year,")
    lines.append("    authors_text = EXCLUDED.authors_text,")
    lines.append("    source = EXCLUDED.source,")
    lines.append("    external_url = COALESCE(EXCLUDED.external_url, publications.external_url),")
    lines.append("    indexation = COALESCE(EXCLUDED.indexation, publications.indexation),")
    lines.append("    type = EXCLUDED.type,")
    lines.append("    status = 'APPROVED',")
    lines.append("    updated_at = CURRENT_TIMESTAMP;")
    lines.append("")
    lines.append("-- Title/year fallback for rows without DOI.")
    lines.append("INSERT INTO publications (title, doi, publication_year, authors_text, source, external_url, pdf_url, indexation, type, status, created_at, updated_at)")
    lines.append("SELECT t.title, NULL, t.publication_year, t.authors_text, 'Scopus', NULLIF(t.external_url, ''), NULL, NULLIF(t.source_title, ''), t.publication_type, 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP")
    lines.append("FROM tmp_official_mediatron_publications t")
    lines.append("WHERE NULLIF(t.doi, '') IS NULL")
    lines.append("  AND NOT EXISTS (")
    lines.append("      SELECT 1 FROM publications p")
    lines.append("      WHERE LOWER(p.title) = LOWER(t.title)")
    lines.append("        AND COALESCE(p.publication_year, -1) = COALESCE(t.publication_year, -1)")
    lines.append("  );")
    path.write_text("\n".join(lines), encoding="utf-8")


def write_links_sql(path: Path, links: list[tuple[PublicationCandidate, MemberCandidate]]) -> None:
    lines = []
    lines.append("-- Link official MEDIATRON Scopus publications to their MEDIATRON members.")
    lines.append("CREATE TEMP TABLE tmp_official_mediatron_publication_links (")
    lines.append("    publication_title TEXT,")
    lines.append("    publication_year INTEGER,")
    lines.append("    doi TEXT,")
    lines.append("    member_first_name TEXT,")
    lines.append("    member_last_name TEXT")
    lines.append(") ON COMMIT DROP;")
    values = []
    seen = set()
    for pub, member in links:
        key = (pub.doi, normalize_key(pub.title), pub.year, normalize_key(member.first_name + member.last_name))
        if key in seen:
            continue
        seen.add(key)
        values.append(f"({sql(pub.title)}, {sql_int(pub.year)}, {sql(pub.doi)}, {sql(member.first_name)}, {sql(member.last_name)})")
    chunk_size = 400
    for i in range(0, len(values), chunk_size):
        lines.append("")
        lines.append("INSERT INTO tmp_official_mediatron_publication_links (publication_title, publication_year, doi, member_first_name, member_last_name) VALUES")
        lines.append(",\n".join(values[i:i+chunk_size]) + ";")
    lines.append("")
    lines.append("DO $$")
    lines.append("DECLARE")
    lines.append("    has_status BOOLEAN;")
    lines.append("    has_created_at BOOLEAN;")
    lines.append("    has_updated_at BOOLEAN;")
    lines.append("    insert_sql TEXT;")
    lines.append("    columns_sql TEXT := 'member_id, publication_id';")
    lines.append("    select_sql TEXT := 'm.id, p.id';")
    lines.append("BEGIN")
    lines.append("    SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'members_publications' AND column_name = 'status') INTO has_status;")
    lines.append("    SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'members_publications' AND column_name = 'created_at') INTO has_created_at;")
    lines.append("    SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'members_publications' AND column_name = 'updated_at') INTO has_updated_at;")
    lines.append("    IF has_status THEN")
    lines.append("        columns_sql := columns_sql || ', status';")
    lines.append("        select_sql := select_sql || ', ''APPROVED''';")
    lines.append("    END IF;")
    lines.append("    IF has_created_at THEN")
    lines.append("        columns_sql := columns_sql || ', created_at';")
    lines.append("        select_sql := select_sql || ', CURRENT_TIMESTAMP';")
    lines.append("    END IF;")
    lines.append("    IF has_updated_at THEN")
    lines.append("        columns_sql := columns_sql || ', updated_at';")
    lines.append("        select_sql := select_sql || ', CURRENT_TIMESTAMP';")
    lines.append("    END IF;")
    lines.append("    insert_sql := 'INSERT INTO members_publications (' || columns_sql || ') '")
    lines.append("        || 'SELECT DISTINCT ' || select_sql || ' '")
    lines.append("        || 'FROM tmp_official_mediatron_publication_links l '")
    lines.append("        || 'JOIN members m ON LOWER(regexp_replace(m.first_name, ''[[:space:]]+'', '''', ''g'')) = LOWER(regexp_replace(l.member_first_name, ''[[:space:]]+'', '''', ''g'')) '")
    lines.append("        || 'AND LOWER(regexp_replace(m.last_name, ''[[:space:]]+'', '''', ''g'')) = LOWER(regexp_replace(l.member_last_name, ''[[:space:]]+'', '''', ''g'')) '")
    lines.append("        || 'JOIN publications p ON ((NULLIF(l.doi, '''') IS NOT NULL AND LOWER(p.doi) = LOWER(l.doi)) '")
    lines.append("        || 'OR (NULLIF(l.doi, '''') IS NULL AND LOWER(p.title) = LOWER(l.publication_title) AND COALESCE(p.publication_year, -1) = COALESCE(l.publication_year, -1))) '")
    lines.append("        || 'ON CONFLICT (member_id, publication_id) DO NOTHING';")
    lines.append("    EXECUTE insert_sql;")
    lines.append("    IF has_status THEN")
    lines.append("        UPDATE members_publications mp")
    lines.append("        SET status = 'APPROVED'")
    lines.append("        FROM tmp_official_mediatron_publication_links l")
    lines.append("        JOIN members m ON LOWER(regexp_replace(m.first_name, '[[:space:]]+', '', 'g')) = LOWER(regexp_replace(l.member_first_name, '[[:space:]]+', '', 'g'))")
    lines.append("            AND LOWER(regexp_replace(m.last_name, '[[:space:]]+', '', 'g')) = LOWER(regexp_replace(l.member_last_name, '[[:space:]]+', '', 'g'))")
    lines.append("        JOIN publications p ON ((NULLIF(l.doi, '') IS NOT NULL AND LOWER(p.doi) = LOWER(l.doi))")
    lines.append("            OR (NULLIF(l.doi, '') IS NULL AND LOWER(p.title) = LOWER(l.publication_title) AND COALESCE(p.publication_year, -1) = COALESCE(l.publication_year, -1)))")
    lines.append("        WHERE mp.member_id = m.id AND mp.publication_id = p.id;")
    lines.append("    END IF;")
    lines.append("END $$;")
    path.write_text("\n".join(lines), encoding="utf-8")


def main() -> int:
    base_dir = Path.cwd()
    migration_dir = base_dir / "src" / "main" / "resources" / "db" / "migration"
    data_dir = base_dir / "import-data"

    if not migration_dir.exists():
        print("ERROR: run this script from backend/Lab_Management or create src/main/resources/db/migration first.", file=sys.stderr)
        return 1

    phd_path = data_dir / PHD_FILE_NAME
    scopus_path = data_dir / SCOPUS_FILE_NAME
    if not phd_path.exists() or not scopus_path.exists():
        print("ERROR: missing Excel files.", file=sys.stderr)
        print(f"Expected: {phd_path}", file=sys.stderr)
        print(f"Expected: {scopus_path}", file=sys.stderr)
        return 1

    members, scopus_to_member = read_members(scopus_path)
    students = read_phd_students(phd_path)
    publications, links = read_publications(scopus_path, scopus_to_member)

    version = next_version(migration_dir)
    files = [
        (migration_dir / f"V{version}__seed_official_mediatron_members.sql", lambda p: write_members_sql(p, members)),
        (migration_dir / f"V{version + 1}__seed_official_mediatron_phd_students.sql", lambda p: write_phd_sql(p, students)),
        (migration_dir / f"V{version + 2}__seed_official_mediatron_scopus_publications.sql", lambda p: write_publications_sql(p, publications)),
        (migration_dir / f"V{version + 3}__seed_official_mediatron_publication_member_links.sql", lambda p: write_links_sql(p, links)),
    ]

    for path, writer in files:
        if path.exists():
            print(f"ERROR: refusing to overwrite existing migration: {path}", file=sys.stderr)
            return 1
        writer(path)

    print("Generated MEDIATRON import migrations:")
    for path, _ in files:
        print(f"- {path.relative_to(base_dir)}")
    print("")
    print(f"Members prepared: {len(members)}")
    print(f"PhD students prepared: {len(students)}")
    print(f"Publications prepared: {len(publications)}")
    print(f"Publication-member links prepared: {len(set((p.doi, normalize_key(p.title), p.year, normalize_key(m.first_name + m.last_name)) for p, m in links))}")
    print("")
    print("Next: restart Spring Boot. Flyway will run these migrations automatically.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
