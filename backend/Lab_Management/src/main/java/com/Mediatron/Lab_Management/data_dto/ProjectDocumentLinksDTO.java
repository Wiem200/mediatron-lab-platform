package com.Mediatron.Lab_Management.data_dto;

import lombok.Data;

@Data
public class ProjectDocumentLinksDTO {
    private LinkItem pdf;
    private LinkItem poster;
    private LinkItem github;
    private LinkItem video;

    @Data
    public static class LinkItem {
        private String label;
        private String url;
    }
}
