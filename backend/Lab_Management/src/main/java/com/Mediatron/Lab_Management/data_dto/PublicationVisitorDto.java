package com.Mediatron.Lab_Management.data_dto;

import com.Mediatron.Lab_Management.data_models.PublicationType;

public class PublicationVisitorDto {

    private String title;
    private Integer publicationYear;
    private String authorsText;
    private String source;
    private String externalUrl;
    private String pdfUrl;
    private PublicationType type;

    public PublicationVisitorDto() {
    }

    public PublicationVisitorDto(
            String title,
            Integer publicationYear,
            String authorsText,
            String source,
            String externalUrl,
            String pdfUrl,
            PublicationType type
    ) {
        this.title = title;
        this.publicationYear = publicationYear;
        this.authorsText = authorsText;
        this.source = source;
        this.externalUrl = externalUrl;
        this.pdfUrl = pdfUrl;
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public PublicationType getType() {
        return type;
    }

    public void setType(PublicationType type) {
        this.type = type;
    }
}