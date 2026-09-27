CREATE TABLE homepage_content (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    hero_title VARCHAR(255) NOT NULL,
    hero_subtitle TEXT NOT NULL,
    history_label VARCHAR(120) NOT NULL,
    history_title VARCHAR(255) NOT NULL,
    history_content TEXT NOT NULL,
    research_areas_label VARCHAR(120) NOT NULL,
    research_areas_title VARCHAR(255) NOT NULL,
    research_areas_description TEXT NOT NULL,
    scientific_teams_label VARCHAR(120) NOT NULL,
    scientific_teams_title VARCHAR(255) NOT NULL,
    scientific_teams_description TEXT NOT NULL
);

CREATE TABLE research_areas (
    id SERIAL PRIMARY KEY,
    title VARCHAR(160) NOT NULL,
    description TEXT NOT NULL,
    display_order INTEGER NOT NULL
);

CREATE TABLE scientific_teams (
    id SERIAL PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    specialty VARCHAR(160) NOT NULL,
    description TEXT NOT NULL,
    display_order INTEGER NOT NULL
);

CREATE INDEX idx_research_areas_display_order ON research_areas (display_order);
CREATE INDEX idx_scientific_teams_display_order ON scientific_teams (display_order);

INSERT INTO homepage_content (
    id,
    hero_title,
    hero_subtitle,
    history_label,
    history_title,
    history_content,
    research_areas_label,
    research_areas_title,
    research_areas_description,
    scientific_teams_label,
    scientific_teams_title,
    scientific_teams_description
) VALUES (
    1,
    'Welcome to MEDIATRON',
    'A cutting-edge research laboratory dedicated to advancing media technology, digital innovation, and scientific excellence.',
    'Laboratory History',
    'A laboratory built at the intersection of media systems, communication science, and digital transformation.',
    'MEDIATRON was established to connect scientific research with the fast-evolving realities of media technology. Over the years, the laboratory has grown into a collaborative environment where faculty, researchers, doctoral students, and partners design, test, and evaluate new digital systems, communication models, and intelligent services. Its history is marked by interdisciplinary work, strong academic supervision, and an applied research culture focused on real institutional and societal needs.',
    'Research Areas',
    'Research axes that structure MEDIATRON''s scientific work.',
    'The laboratory organizes its agenda around complementary themes that combine theoretical depth, experimentation, and deployable innovation.',
    'Scientific Teams',
    'Scientific teams driving projects, publications, and technology transfer.',
    'Each team contributes a distinct expertise while collaborating across methods, datasets, and application domains.'
);

INSERT INTO research_areas (title, description, display_order) VALUES
    (
        'Media Technology and Digital Platforms',
        'Design and evaluation of digital media infrastructures, publishing workflows, platform architectures, and user-facing content delivery systems.',
        1
    ),
    (
        'Artificial Intelligence for Communication',
        'Research on intelligent content analysis, recommendation, multilingual processing, knowledge extraction, and decision support for communication environments.',
        2
    ),
    (
        'Immersive Interaction and User Experience',
        'Investigation of interactive interfaces, immersive storytelling, human-computer interaction, and inclusive digital experiences for professional and academic contexts.',
        3
    ),
    (
        'Data, Governance, and Digital Society',
        'Study of data governance, ethics, policy, security, and the societal impact of digital transformation in media and communication ecosystems.',
        4
    );

INSERT INTO scientific_teams (name, specialty, description, display_order) VALUES
    (
        'Digital Media Systems Team',
        'Infrastructure and publishing systems',
        'Focuses on the engineering of robust media platforms, audiovisual processing pipelines, digital repositories, and tools that support creation, preservation, and dissemination.',
        1
    ),
    (
        'AI and Knowledge Engineering Team',
        'Machine intelligence and analytics',
        'Develops methods for semantic enrichment, machine learning, language technologies, and analytical services that turn complex media data into actionable scientific knowledge.',
        2
    ),
    (
        'Interaction, Design, and Society Team',
        'Experience design and impact studies',
        'Examines how users engage with digital systems through usability research, interface design, participatory methods, and critical analysis of technological adoption.',
        3
    );
