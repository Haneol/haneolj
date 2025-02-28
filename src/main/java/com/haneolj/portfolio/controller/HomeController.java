package com.haneolj.portfolio.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "home/index";
    }

    @GetMapping("/graph")
    public String showGraphView(Model model) {
        model.addAttribute("updateDate", "2025.01.04");
        return "post/graph";  // 그래프 뷰 템플릿
    }

    @GetMapping("/category")
    public String showCategoryView(Model model) {
        model.addAttribute("updateDate", "2025.01.04");
        return "post/category";  // 카테고리 뷰 템플릿
    }
}
