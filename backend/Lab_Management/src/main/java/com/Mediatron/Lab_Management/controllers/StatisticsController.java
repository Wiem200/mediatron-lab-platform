package com.Mediatron.Lab_Management.controllers;

import com.Mediatron.Lab_Management.data_dto.HomepageStatisticsSummaryDto;
import com.Mediatron.Lab_Management.repositories.PublicationStatisticsRepository;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
@CrossOrigin(origins = "*")
public class StatisticsController {

    private final PublicationStatisticsRepository publicationStatisticsRepository;

    public StatisticsController(PublicationStatisticsRepository publicationStatisticsRepository) {
        this.publicationStatisticsRepository = publicationStatisticsRepository;
    }

    @GetMapping("/summary")
    public HomepageStatisticsSummaryDto getSummary() {
        return publicationStatisticsRepository.getHomepageSummary();
    }
}
