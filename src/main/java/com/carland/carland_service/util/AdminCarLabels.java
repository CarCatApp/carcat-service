package com.carland.carland_service.util;

import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Customer;

/**
 * tr: Admin araç tablosu ve Excel için sahip satırı ve path metni.
 * en: Owner line and path text for the admin car table and Excel.
 */
public final class AdminCarLabels {

    private AdminCarLabels() {
    }

    /** tr: Ad Soyad -&gt; telefon. Sahip yoksa tire. / en: Name surname -&gt; phone. A dash when there is no owner. */
    public static String ownerLine(Customer customer) {
        if (customer == null) {
            return "—";
        }
        String name = ownerName(customer);
        String phone = ownerPhone(customer);
        if (name.isEmpty() && phone.isEmpty()) {
            return "—";
        }
        if (name.isEmpty()) {
            return phone;
        }
        if (phone.isEmpty()) {
            return name;
        }
        return name + " -> " + phone;
    }

    public static String ownerName(Customer customer) {
        if (customer == null) {
            return "";
        }
        String name = trim(customer.getName());
        String surname = trim(customer.getSurname());
        if (name.isEmpty()) {
            return surname;
        }
        if (surname.isEmpty()) {
            return name;
        }
        return name + " " + surname;
    }

    public static String ownerPhone(Customer customer) {
        if (customer == null) {
            return "";
        }
        return trim(customer.getPhoneNumber());
    }

    /** tr: Boş path vin sayılır. / en: A blank path counts as vin. */
    public static String pathOf(Car car) {
        if (car == null || car.getPath() == null || car.getPath().isBlank()) {
            return Car.PATH_VIN;
        }
        return car.getPath();
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
