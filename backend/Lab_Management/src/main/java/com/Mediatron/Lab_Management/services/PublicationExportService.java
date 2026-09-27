package com.Mediatron.Lab_Management.services;

import com.Mediatron.Lab_Management.data_models.Publication;
import com.Mediatron.Lab_Management.data_models.PublicationStatus;
import com.Mediatron.Lab_Management.data_models.PublicationType;
import com.Mediatron.Lab_Management.repositories.PublicationRepository;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class PublicationExportService {

    private static final int EXPORT_PAGE_SIZE = 1000;
    private static final Sort EXPORT_SORT = Sort.by(Sort.Direction.DESC, "publicationYear")
            .and(Sort.by(Sort.Direction.DESC, "createdAt"));

    private final PublicationRepository publicationRepository;

    public PublicationExportService(PublicationRepository publicationRepository) {
        this.publicationRepository = publicationRepository;
    }

    public byte[] exportPublicationsToCsv(
            String search,
            String title,
            String author,
            Integer year,
            PublicationType type
    ) {
        String effectiveSearch = normalizeSearch(search);
        if (effectiveSearch.isBlank()) {
            effectiveSearch = normalizeSearch(title);
        }

        List<Publication> publications = fetchAllApprovedPublications(
                effectiveSearch,
                normalizeSearch(author),
                year,
                type
        );

        StringBuilder csv = new StringBuilder();
        csv.append("Title,Publication Year,Authors,Source,External URL,PDF URL,Type\n");

        for (Publication publication : publications) {
            csv.append(escapeCsv(publication.getTitle()))
                    .append(',')
                    .append(escapeCsv(publication.getPublicationYear()))
                    .append(',')
                    .append(escapeCsv(publication.getAuthorsText()))
                    .append(',')
                    .append(escapeCsv(publication.getSource()))
                    .append(',')
                    .append(escapeCsv(publication.getExternalUrl()))
                    .append(',')
                    .append(escapeCsv(publication.getPdfUrl()))
                    .append(',')
                    .append(escapeCsv(publication.getType()))
                    .append('\n');
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] exportPublicationsToPdf(
            String search,
            String title,
            String author,
            Integer year,
            PublicationType type
    ) {
        String effectiveSearch = normalizeSearch(search);
        if (effectiveSearch.isBlank()) {
            effectiveSearch = normalizeSearch(title);
        }

        List<Publication> publications = fetchAllApprovedPublications(
                effectiveSearch,
                normalizeSearch(author),
                year,
                type
        );

        return buildPdf(
                "Publications Export",
                buildFiltersText(effectiveSearch, normalizeSearch(author), year, type, null),
                publications
        );
    }

    public byte[] exportMemberPublicationsToCsv(
            Integer memberId,
            String search,
            String author,
            Integer year,
            PublicationType type
    ) {
        List<Publication> publications = fetchAllApprovedMemberPublications(
                memberId,
                normalizeSearch(search),
                normalizeSearch(author),
                year,
                type
        );

        StringBuilder csv = new StringBuilder();
        csv.append("Title,Publication Year,Authors,Source,External URL,PDF URL,Type\n");

        for (Publication publication : publications) {
            csv.append(escapeCsv(publication.getTitle()))
                    .append(',')
                    .append(escapeCsv(publication.getPublicationYear()))
                    .append(',')
                    .append(escapeCsv(publication.getAuthorsText()))
                    .append(',')
                    .append(escapeCsv(publication.getSource()))
                    .append(',')
                    .append(escapeCsv(publication.getExternalUrl()))
                    .append(',')
                    .append(escapeCsv(publication.getPdfUrl()))
                    .append(',')
                    .append(escapeCsv(publication.getType()))
                    .append('\n');
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] exportMemberPublicationsToPdf(
            Integer memberId,
            String search,
            String author,
            Integer year,
            PublicationType type
    ) {
        List<Publication> publications = fetchAllApprovedMemberPublications(
                memberId,
                normalizeSearch(search),
                normalizeSearch(author),
                year,
                type
        );

        return buildPdf(
                "Member Publications Export",
                buildFiltersText(normalizeSearch(search), normalizeSearch(author), year, type, memberId),
                publications
        );
    }

    private List<Publication> fetchAllApprovedPublications(
            String search,
            String author,
            Integer year,
            PublicationType type
    ) {
        List<Publication> publications = new ArrayList<>();
        int pageNumber = 0;

        while (true) {
            Page<Publication> page = publicationRepository.searchPublications(
                    search,
                    author,
                    year,
                    type,
                    PublicationStatus.APPROVED,
                    PageRequest.of(pageNumber, EXPORT_PAGE_SIZE, EXPORT_SORT)
            );

            publications.addAll(page.getContent());

            if (page.isLast()) {
                break;
            }

            pageNumber++;
        }

        return publications;
    }

    private List<Publication> fetchAllApprovedMemberPublications(
            Integer memberId,
            String search,
            String author,
            Integer year,
            PublicationType type
    ) {
        List<Publication> publications = new ArrayList<>();
        int pageNumber = 0;

        while (true) {
            Page<Publication> page = publicationRepository.searchMemberPublications(
                    memberId,
                    search,
                    author,
                    year,
                    type,
                    PublicationStatus.APPROVED,
                    PageRequest.of(pageNumber, EXPORT_PAGE_SIZE, EXPORT_SORT)
            );

            publications.addAll(page.getContent());

            if (page.isLast()) {
                break;
            }

            pageNumber++;
        }

        return publications;
    }

    private byte[] buildPdf(String title, String filtersText, List<Publication> publications) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            com.lowagie.text.Document document =
                    new com.lowagie.text.Document(PageSize.A4.rotate(), 24, 24, 24, 24);
            PdfWriter.getInstance(document, outputStream);

            document.open();

            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL);
            Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD);

            Paragraph titleParagraph = new Paragraph(title, titleFont);
            titleParagraph.setSpacingAfter(10f);
            document.add(titleParagraph);

            Paragraph filtersParagraph = new Paragraph(filtersText, normalFont);
            filtersParagraph.setSpacingAfter(12f);
            document.add(filtersParagraph);

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setSpacingBefore(5f);
            table.setWidths(new float[]{4.4f, 1.2f, 3.2f, 2.3f, 3.2f, 1.6f});

            addHeaderCell(table, "Title", headerFont);
            addHeaderCell(table, "Year", headerFont);
            addHeaderCell(table, "Authors", headerFont);
            addHeaderCell(table, "Source", headerFont);
            addHeaderCell(table, "External URL", headerFont);
            addHeaderCell(table, "Type", headerFont);

            for (Publication publication : publications) {
                addBodyCell(table, safe(publication.getTitle()), normalFont);
                addBodyCell(table, safe(publication.getPublicationYear()), normalFont);
                addBodyCell(table, safe(publication.getAuthorsText()), normalFont);
                addBodyCell(table, safe(publication.getSource()), normalFont);
                addBodyCell(table, safe(publication.getExternalUrl()), normalFont);
                addBodyCell(table, safe(publication.getType()), normalFont);
            }

            document.add(table);
            document.close();

            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate publications PDF export: " + ex.getMessage(), ex);
        }
    }

    private void addHeaderCell(PdfPTable table, String value, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setPadding(6f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String value, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setPadding(5f);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private String buildFiltersText(
            String search,
            String author,
            Integer year,
            PublicationType type,
            Integer memberId
    ) {
        List<String> filters = new ArrayList<>();

        if (memberId != null) {
            filters.add("Member ID: " + memberId);
        }
        if (search != null && !search.isBlank()) {
            filters.add("Search: " + search);
        }
        if (author != null && !author.isBlank()) {
            filters.add("Author: " + author);
        }
        if (year != null) {
            filters.add("Year: " + year);
        }
        if (type != null) {
            filters.add("Type: " + type);
        }

        if (filters.isEmpty()) {
            return "Filters: none";
        }

        return "Filters: " + String.join(" | ", filters);
    }

    private String normalizeSearch(String value) {
        if (value == null) {
            return "";
        }

        String cleaned = value.trim();
        return cleaned.isBlank() ? "" : cleaned;
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String escapeCsv(Object value) {
        String text = safe(value);
        String escaped = text.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}