package com.carland.carland_service.controller;


import com.carland.carland_service.entity.Car;
import com.carland.carland_service.repository.CarRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class AdminController {

    private final CarRepository carRepository;


    @GetMapping({"/admin", "/admin/"})
    public String loginPage() {
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

            return "redirect:/admin/cars";
        }

        return "redirect:/admin?error=true";
    }


    @GetMapping("/admin/cars")
    public String cars(
            @RequestParam(defaultValue = "0") int page,
            HttpSession session,
            Model model
    ) {

        Boolean login = (Boolean) session.getAttribute("ADMIN_LOGIN");

        if (login == null || !login) {
            return "redirect:/admin";
        }


        Pageable pageable = PageRequest.of(page, 10);

        Page<Car> cars = carRepository.findAll(pageable);


        model.addAttribute("cars", cars);

        return "cars";
    }


    @GetMapping("/admin/logout")
    public String logout(HttpSession session){

        session.invalidate();

        return "redirect:/admin";
    }
}