package com.carland.carland_service.repository;

import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.CustomerServiceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * tr: CustomerServiceRecord entity'si için JPA repository; müşterinin girdiği araç servis kayıtlarını sorgular.
 * en: JPA repository for the CustomerServiceRecord entity; queries car service records entered by the customer.
 */
@Repository
public interface CustomerServiceRecordRepository extends JpaRepository<CustomerServiceRecord,Long> {
    /** tr: Servis adı ve araca göre kaydı bulur. / en: Finds a record by service name and car. */
    CustomerServiceRecord findByServiceNameAndCar(String serviceName, Car car);

    /** tr: Servis id'si ve araca göre kaydı bulur. / en: Finds a record by service id and car. */
    CustomerServiceRecord findByServiceIdAndCar(Long serviceId, Car car);

    /** tr: Kayıt id'si ve araca göre kaydı bulur. / en: Finds a record by record id and car. */
    CustomerServiceRecord findByIdAndCar(Long recordId, Car car);

    /** tr: Servis adı, işlem tipi ve araca göre kaydı bulur. / en: Finds a record by service name, action type and car. */
    CustomerServiceRecord findByServiceNameAndActionTypeAndCar(String serviceName, String actionType, Car car);


    /** tr: Araca ait tüm servis kayıtlarını listeler. / en: Lists all service records of a car. */
    List<CustomerServiceRecord> findAllByCar(Car car);


}
