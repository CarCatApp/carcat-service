package com.carland.carland_service.repository;

import com.carland.carland_service.entity.AutoService;
import com.carland.carland_service.entity.SuperAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * tr: AutoService entity'si için JPA repository; servis merkezlerini sorgular.
 * en: JPA repository for the AutoService entity; queries auto service centers.
 */
@Repository
public interface AutoServiceRepository extends JpaRepository<AutoService, Long> {

    /** tr: SuperAdmin'e bağlı servis merkezini bulur. / en: Finds the auto service center owned by the given superadmin. */
    AutoService findBySuperAdmin(SuperAdmin superAdmin);


}
