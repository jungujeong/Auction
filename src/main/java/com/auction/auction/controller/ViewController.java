package com.auction.auction.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.auction.auction.model.Item;
import com.auction.auction.service.ItemService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ViewController {

    private final ItemService itemService;

    // 홈 페이지
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("items", itemService.getActiveItems());
        return "index";
    }

    // 로그인 페이지
    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("error", "아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        return "login";
    }

    // 회원가입 페이지
    @GetMapping("/signup")
    public String signup() {
        return "signup";
    }

    // 경매 목록 페이지
    @GetMapping("/items")
    public String itemList(Model model) {
        model.addAttribute("items", itemService.getActiveItems());
        return "items/list";
    }

    // 경매 상세 페이지
    @GetMapping("/items/{id}")
    public String itemDetail(@PathVariable Long id, Model model,
                             @AuthenticationPrincipal UserDetails userDetails) {
        Item item = itemService.getItem(id);
        model.addAttribute("item", item);

        // 로그인한 사용자가 판매자인지 확인
        if (userDetails != null) {
            boolean isSeller = item.getSeller().getUsername().equals(userDetails.getUsername());
            model.addAttribute("isSeller", isSeller);
        } else {
            model.addAttribute("isSeller", false);
        }

        return "items/detail";
    }

    // 물건 등록 페이지
    @GetMapping("/items/register")
    public String registerForm() {
        return "items/register";
    }
}
