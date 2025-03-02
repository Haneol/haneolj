package com.haneolj.portfolio.controller;

import com.haneolj.portfolio.dto.CategoryNodeDto;
import com.haneolj.portfolio.service.StudyService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping
@RequiredArgsConstructor
public class HomeController {
    private final StudyService studyService;

    @Value("${app.version}")
    private String appVersion;

    @GetMapping("/")
    public String home(Model model) {
        CategoryNodeDto studyRoot = studyService.getStudyStructure();
        model.addAttribute("studyRoot", studyRoot);
        model.addAttribute("updateDate", studyService.getLastUpdateDate());
        model.addAttribute("version", appVersion);
        return "home/index";
    }

    @GetMapping("/refresh")
    @ResponseBody
    public String refreshStudyStructure() {
        try {
            studyService.refreshStudyStructure();

            return "redirect:/";
        } catch (Exception e) {
            return "새로고침 실패: " + e.getMessage();
        }
    }
}