package com.carland.carland_service.repository;

import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * tr: Car entity'si için JPA repository; araçları VIN, plaka, müşteri ve id'ye göre sorgular.
 * en: JPA repository for the Car entity; queries cars by VIN, plate number, customer and id.
 */
public interface CarRepository extends JpaRepository<Car, Long>, JpaSpecificationExecutor<Car> {

    /** tr: VIN'e göre aracı bulur. / en: Finds a car by VIN. */
    Car findByVin(String vin);

    /** tr: Plaka numarasına göre (büyük/küçük harf duyarsız) aracı bulur. / en: Finds a car by plate number (case-insensitive). */
    Optional<Car> findByPlateNumberIgnoreCase(String plateNumber);

    /** tr: Müşteriye ait tüm araçları listeler. / en: Lists all cars of a customer. */
    List<Car> findAllByCustomer(Customer customer);

    /** tr: Araçları sayfalı olarak listeler. / en: Lists cars with pagination. */
    Page<Car> findAll(Pageable pageable);

    /** tr: Araç id'sine göre aracı bulur. / en: Finds a car by car id. */
    Car findByCarId(Long carId);

    /** tr: Araç id'si ve müşteriye göre aracı bulur. / en: Finds a car by car id and customer. */
    Car findByCarIdAndCustomer(Long carId, Customer customer);

    /** tr: Tüm araçları müşterileriyle birlikte (join fetch) getirir. / en: Fetches all cars together with their customers (join fetch). */
    @Query("SELECT c FROM Car c JOIN FETCH c.customer")
    List<Car> findAllWithCustomer();

    /** tr: Müşterinin araçlarını oluşturulma tarihine göre azalan sırada listeler. / en: Lists a customer's cars ordered by creation date descending. */
    List<Car> findAllByCustomerOrderByCreatedAtDesc(Customer customer);

    /** tr: VIN ve müşteriye göre aracı bulur. / en: Finds a car by VIN and customer. */
    Car findByVinAndCustomer(String vin, Customer customer);

    /** tr: Müşteri kullanıcı id'sine göre araçları listeler. / en: Lists cars by customer user id. */
    List<Car> findByCustomer_UserId(Long userId);

    /** tr: Müşteri kullanıcı id'sine göre araçları sayfalı listeler. / en: Lists cars by customer user id with pagination. */
    Page<Car> findByCustomer_UserId(Long userId, Pageable pageable);

    /** tr: Kayıtlı araçlarda geçen benzersiz marka adları. / en: Distinct brand names present on stored cars. */
    @Query("SELECT DISTINCT c.brand FROM Car c WHERE c.brand IS NOT NULL AND c.brand <> '' ORDER BY c.brand")
    List<String> findDistinctBrands();
}