package com.carland.carland_service.repository;

import com.carland.carland_service.entity.Log;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * tr: Log entity'si için JPA repository; log kayıtlarına standart CRUD erişimi sağlar.
 * en: JPA repository for the Log entity; provides standard CRUD access to log records.
 */
@Repository
public interface LogRepository extends JpaRepository<Log, Long> {

}
