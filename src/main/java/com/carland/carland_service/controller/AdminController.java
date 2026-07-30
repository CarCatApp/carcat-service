package com.carland.carland_service.controller;

import com.carland.carland_service.entity.Car;
import com.carland.carland_service.repository.CarRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class AdminController {

    private final CarRepository carRepository;

    private static final String ADMIN_URL = "https://digital-innovation.agency";


    @GetMapping({"/admin", "/admin/"})
    public String loginPage(
            @RequestParam(required = false) String error,
            Model model
    ) {

        if (error != null) {
            model.addAttribute("error", true);
        }

        return "login";
    }


    @PostMapping("/admin/login")
    public String login(
            @RequestParam String username,
            @RequestParam String password,
            HttpSession session
    ) {

        if ("nemet".equals(username) && "nemet".equals(password)) {

            session.setAttribute("ADMIN_LOGIN", true);

            return "redirect:" + ADMIN_URL + "/admin/cars";
        }

        return "redirect:" + ADMIN_URL + "/admin/?error=true";
    }


    @GetMapping("/admin/cars")
    public String cars(
            @RequestParam(defaultValue = "0") int page,
            HttpSession session,
            Model model
    ) {

        Boolean loggedIn = (Boolean) session.getAttribute("ADMIN_LOGIN");

        if (!Boolean.TRUE.equals(loggedIn)) {

            return "redirect:" + ADMIN_URL + "/admin/";
        }


        Pageable pageable = PageRequest.of(page, 10);

        Page<Car> carPage = carRepository.findAll(pageable);


        // Thymeleaf tarafında ${cars.content} kullanabilmek için Page gönderiyoruz
        model.addAttribute("cars", carPage);


        return "cars";
    }


    @GetMapping("/admin/logout")
    public String logout(HttpSession session) {

        session.invalidate();

        return "redirect:" + ADMIN_URL + "/admin/";
    }
}