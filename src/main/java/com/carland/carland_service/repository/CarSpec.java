package com.carland.carland_service.repository;

import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.idda.IddaPlateKey;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.hibernate.query.criteria.JpaRoot;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * tr: Admin araç listesi filtreleri (Criteria; JPQL LIKE + page count Hibernate 6 hatasından kaçınır).
 *     Plaka araması boşluk, tire ve noktayı atar: 10-BB-110, 10BB110 ve 10 BB 110 aynıdır.
 * en: Admin car list filters (Criteria; avoids Hibernate 6 JPQL LIKE + page count bind bug).
 *     Plate search drops spaces, dashes and dots: 10-BB-110, 10BB110 and 10 BB 110 match.
 */
public final class CarSpec {

    public static final String OWNER_ALL = "all";
    public static final String OWNER_WITH = "sahipli";
    public static final String OWNER_NONE = "sahipsiz";
    public static final String PATH_ALL = "all";

    private CarSpec() {
    }

    /**
     * @param owner {@code sahipli}, {@code sahipsiz}, or null/{@code all}
     * @param path  {@code vin}, {@code plate}, or null/{@code all}. Blank path counts as vin.
     */
    public static Specification<Car> filters(Long userId, String vin, String plate, String owner, String path) {
        String plateKey = plateNeedle(plate);
        String ownerKey = ownerKey(owner);
        String pathKey = pathKey(path);
        return (root, query, cb) -> {
            boolean countQuery = query.getResultType() == Long.class || query.getResultType() == long.class;
            boolean needCustomer = userId != null || ownerKey != null;
            Join<Car, Customer> customerJoin = !countQuery
                    ? customerFetch(root)
                    : (needCustomer ? root.join("customer", JoinType.LEFT) : null);
            List<Predicate> parts = new ArrayList<>();
            if (userId != null) {
                parts.add(cb.equal(customerJoin.get("userId"), userId));
            }
            if (OWNER_WITH.equals(ownerKey)) {
                parts.add(cb.isNotNull(customerJoin.get("userId")));
            } else if (OWNER_NONE.equals(ownerKey)) {
                parts.add(cb.isNull(customerJoin.get("userId")));
            }
            if (vin != null && !vin.isBlank()) {
                String needle = "%" + vin.trim().toLowerCase() + "%";
                parts.add(cb.like(cb.lower(cb.coalesce(root.get("vin"), "")), needle));
            }
            if (plateKey != null) {
                parts.add(cb.like(normalizedPlate(cb, cb.coalesce(root.get("plateNumber"), "")), "%" + plateKey + "%"));
            }
            if (Car.PATH_VIN.equals(pathKey)) {
                Expression<String> stored = cb.lower(cb.trim(cb.coalesce(root.get("path"), "")));
                parts.add(cb.or(cb.equal(stored, ""), cb.equal(stored, Car.PATH_VIN)));
            } else if (Car.PATH_PLATE.equals(pathKey)) {
                parts.add(cb.equal(cb.lower(cb.trim(root.get("path"))), Car.PATH_PLATE));
            }
            if (parts.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(parts.toArray(Predicate[]::new));
        };
    }

    @SuppressWarnings("unchecked")
    private static Join<Car, Customer> customerFetch(Root<Car> root) {
        JpaRoot<Car> jpaRoot = (JpaRoot<Car>) root;
        return (Join<Car, Customer>) (Object) jpaRoot.fetch("customer", JoinType.LEFT);
    }

    /** tr: all ve bilinmeyen değer filtre değildir. / en: all and unknown values are not a filter. */
    public static String ownerKey(String owner) {
        if (owner == null) {
            return null;
        }
        String value = owner.trim().toLowerCase(Locale.ROOT);
        if (OWNER_WITH.equals(value) || OWNER_NONE.equals(value)) {
            return value;
        }
        return null;
    }

    /** tr: all ve bilinmeyen değer filtre değildir. Boş path vin sayılır. / en: all and unknown values are not a filter. A blank path counts as vin. */
    public static String pathKey(String path) {
        if (path == null) {
            return null;
        }
        String value = path.trim().toLowerCase(Locale.ROOT);
        if (Car.PATH_VIN.equals(value) || Car.PATH_PLATE.equals(value)) {
            return value;
        }
        return null;
    }

    /**
     * tr: Admin plaka kutusunun arama anahtarı. Boşsa filtre yok.
     * en: Search key for the admin plate box. Blank means no filter.
     */
    public static String plateNeedle(String plate) {
        String key = IddaPlateKey.of(plate).replace("%", "").replace("_", "");
        return key.isEmpty() ? null : key;
    }

    private static Expression<String> normalizedPlate(CriteriaBuilder cb, Expression<String> plate) {
        Expression<String> value = plate;
        value = cb.function("replace", String.class, value, cb.literal("-"), cb.literal(""));
        value = cb.function("replace", String.class, value, cb.literal(" "), cb.literal(""));
        value = cb.function("replace", String.class, value, cb.literal("."), cb.literal(""));
        value = cb.function("replace", String.class, value, cb.literal("\u00A0"), cb.literal(""));
        return cb.upper(value);
    }
}
