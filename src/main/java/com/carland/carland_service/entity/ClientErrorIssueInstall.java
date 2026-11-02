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
 * tr: Bir qrupda eyni installId-ni bir dəfə saymaq üçün. Bu cihaz id-si deyil.
 * en: Counts an installId once per issue. This is not a device id.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(ClientErrorIssueInstall.Pk.class)
@Table(name = "client_error_issue_install")
public class ClientErrorIssueInstall {

    @Id
    @Column(name = "issue_id", nullable = false)
    private Long issueId;

    @Id
    @Column(name = "install_id", nullable = false, length = 64)
    private String installId;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pk implements Serializable {
        private Long issueId;
        private String installId;
    }
}
