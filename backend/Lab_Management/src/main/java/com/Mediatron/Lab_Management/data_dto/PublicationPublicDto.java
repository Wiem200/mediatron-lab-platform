package com.Mediatron.Lab_Management.data_dto;

import com.Mediatron.Lab_Management.data_models.PublicationStatus;
import com.Mediatron.Lab_Management.data_models.PublicationType;

public class PublicationPublicDto {

    private Integer id;
    private String title;
    private String doi;
    private Integer publicationYear;
    private String authorsText;
    private String source;
    private String externalUrl;
    private String pdfUrl;
    private String indexation;
    private PublicationType type;
    private PublicationStatus status;

    public PublicationPublicDto() {
    }

    public PublicationPublicDto(
            Integer id,
            String title,
            String doi,
            Integer publicationYear,
            String authorsText,
            String source,
            String externalUrl,
            String pdfUrl,
            String indexation,
            PublicationType type,
            PublicationStatus status
    ) {
        this.id = id;
        this.title = title;
        this.doi = doi;
        this.publicationYear = publicationYear;
        this.authorsText = authorsText;
        this.source = source;
        this.externalUrl = externalUrl;
        this.pdfUrl = pdfUrl;
        this.indexation = indexation;
        this.type = type;
        this.status = status;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public Integer getPublicationYear() {
        return publicationYear;
    }

    public void setPublicationYear(Integer publicationYear) {
        this.publicationYear = publicationYear;
    }

    public String getAuthorsText() {
        return authorsText;
    }

    public void setAuthorsText(String authorsText) {
        this.authorsText = authorsText;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    public void setExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    public String getIndexation() {
        return indexation;
    }

    public void setIndexation(String indexation) {
        this.indexation = indexation;
    }

    public PublicationType getType() {
        return type;
    }

    public void setType(PublicationType type) {
        this.type = type;
    }

    public PublicationStatus getStatus() {
        return status;
    }

    public void setStatus(PublicationStatus status) {
        this.status = status;
    }
}