package com.carland.carland_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * tr: Bir qrupda eyni userId-ni bir dəfə saymaq üçün. Hesab silinəndə sətir silinir.
 * en: Counts a userId once per issue. The row is removed when the account is deleted.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(ClientErrorIssueUser.Pk.class)
@Table(name = "client_error_issue_user")
public class ClientErrorIssueUser {

    @Id
    @Column(name = "issue_id", nullable = false)
    private Long issueId;

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pk implements Serializable {
        private Long issueId;
        private Long userId;
    }
}
