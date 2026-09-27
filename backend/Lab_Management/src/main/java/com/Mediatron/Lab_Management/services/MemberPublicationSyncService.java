package com.Mediatron.Lab_Management.services;

import com.Mediatron.Lab_Management.data_dto.MemberPublicationSyncDTO;
import com.Mediatron.Lab_Management.data_models.Member;
import org.springframework.stereotype.Service;

@Service
public class MemberPublicationSyncService {

    public MemberPublicationSyncDTO buildSyncInfo(Member member) {
        MemberPublicationSyncDTO dto = new MemberPublicationSyncDTO();
        String googleScholarUrl = cleanNullable(member.getGoogleScholarUrl());
        String orcidId = cleanNullable(member.getOrcidId());

        dto.setSynchronizationAvailable(googleScholarUrl != null || orcidId != null);
        dto.setSource(googleScholarUrl != null ? "GOOGLE_SCHOLAR" : (orcidId != null ? "ORCID" : "UNCONFIGURED"));
        dto.setProfileUrl(googleScholarUrl);
        dto.setIdentifier(orcidId);

        if (dto.isSynchronizationAvailable()) {
            dto.setStatus("PENDING_IMPLEMENTATION");
            dto.setMessage("Publication synchronization is prepared at the service layer and can be connected to an external source later.");
        } else {
            dto.setStatus("NOT_CONFIGURED");
            dto.setMessage("Provide a Google Scholar URL or ORCID iD to enable automatic publication synchronization later.");
        }

        return dto;
    }

    private String cleanNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
