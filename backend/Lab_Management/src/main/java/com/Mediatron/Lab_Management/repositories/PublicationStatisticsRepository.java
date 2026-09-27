package com.Mediatron.Lab_Management.repositories;

import com.Mediatron.Lab_Management.data_dto.HomepageStatisticsSummaryDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PublicationStatisticsRepository {

    private static final String HOMEPAGE_STATISTICS_SQL = """
            SELECT
                COUNT(*) FILTER (WHERE status = 'APPROVED') AS publication_count,
                COUNT(*) FILTER (
                    WHERE status = 'APPROVED'
                    AND (
                        type IN ('JOURNAL', 'CONFERENCE_A', 'CONFERENCE_B', 'CONFERENCE_C', 'WORKSHOP')
                        OR NULLIF(TRIM(COALESCE(indexation, '')), '') IS NOT NULL
                    )
                ) AS journal_and_conference_count,
                COUNT(*) FILTER (
                    WHERE status = 'APPROVED'
                    AND (
                        type IN ('CONFERENCE_A', 'CONFERENCE_B', 'CONFERENCE_C')
                        OR (
                            type = 'WORKSHOP'
                            AND (
                                LOWER(COALESCE(indexation, '')) LIKE '%national%'
                                OR LOWER(COALESCE(source, '')) LIKE '%national%'
                            )
                        )
                    )
                ) AS classified_conference_count
            FROM publications
            """;

    private final JdbcTemplate jdbcTemplate;

    public PublicationStatisticsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public HomepageStatisticsSummaryDto getHomepageSummary() {
        return jdbcTemplate.queryForObject(
                HOMEPAGE_STATISTICS_SQL,
                (rs, rowNum) -> new HomepageStatisticsSummaryDto(
                        rs.getLong("publication_count"),
                        rs.getLong("journal_and_conference_count"),
                        rs.getLong("classified_conference_count")
                )
        );
    }
}
