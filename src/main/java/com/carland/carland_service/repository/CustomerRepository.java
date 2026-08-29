package com.carland.carland_service.repository;

import com.carland.carland_service.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * tr: Customer entity'si için JPA repository; müşterileri id, telefon ve duruma göre sorgular.
 * en: JPA repository for the Customer entity; queries customers by id, phone and status.
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    /** tr: Kullanıcı id'sine göre müşteriyi bulur. / en: Finds a customer by user id. */
    Customer findByUserId(Long userId);

    /** tr: Telefon numarasına göre müşteriyi bulur. / en: Finds a customer by phone number. */
    Customer findByPhoneNumber(String phoneNumber);

    /** tr: Kullanıcı id, telefon ve duruma göre müşteriyi bulur. / en: Finds a customer by user id, phone number and status. */
    Customer findByUserIdAndPhoneNumberAndStatus(Long userId, String phoneNumber,  String status);

    /** tr: Kullanıcı id ve telefona göre müşteriyi bulur. / en: Finds a customer by user id and phone number. */
    Customer findByUserIdAndPhoneNumber(Long aLong, String phoneNumber);

    /**
     * tr: Aynı FIN'e sahip tüm müşteriler (verified olmayanlarda duplicate olabilir).
     * en: All customers with this FIN (duplicates allowed when not SIMA-verified).
     */
    List<Customer> findAllByPinIgnoreCase(String pin);

    /** tr: E-posta adresine göre müşteriyi bulur. / en: Finds a customer by e-mail. */
    Customer findByMailIgnoreCase(String mail);
}
