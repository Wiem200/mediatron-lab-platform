package com.Mediatron.Lab_Management.data_models;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberPublicationId implements Serializable {

    @Column(name = "member_id")
    private Integer memberId;

    @Column(name = "publication_id")
    private Integer publicationId;
}
