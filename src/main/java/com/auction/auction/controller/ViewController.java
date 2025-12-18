package com.auction.auction.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.auction.auction.model.Item;
import com.auction.auction.model.User;
import com.auction.auction.service.ItemService;
import com.auction.auction.service.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ViewController {

    private final ItemService itemService;
    private final UserService userService;

    // 홈 페이지
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("items", itemService.getActiveItems());
        return "index";
    }

    // 로그인 페이지
    @GetMapping("/login")
    public String login(@RequestParam(required = false, name = "error") String error, Model model) {
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
    public String itemDetail(@PathVariable(name = "id") Long id, Model model,
                             @AuthenticationPrincipal UserDetails userDetails) {
        try {
            System.out.println("=== 상세 페이지 요청: id = " + id);
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
        } catch (Exception e) {
            System.out.println("=== 상세 페이지 오류: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    // 물건 등록 페이지
    @GetMapping("/items/register")
    public String registerForm() {
        return "items/register";
    }

    // 물건 수정 페이지
    @GetMapping("/items/{id}/edit")
    public String editForm(@PathVariable(name = "id") Long id, Model model,
                          @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Item item = itemService.getItem(id);

            // 판매자 본인인지 확인
            if (userDetails == null || !item.getSeller().getUsername().equals(userDetails.getUsername())) {
                model.addAttribute("error", "본인이 등록한 물건만 수정할 수 있습니다.");
                return "error";
            }

            model.addAttribute("item", item);
            return "items/edit";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    // 프로필 페이지
    @GetMapping("/profile")
    public String profile(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (userDetails == null) {
                return "redirect:/login";
            }

            User user = userService.getUser(userDetails.getUsername());
            model.addAttribute("user", user);
            return "profile";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    // 경매방 목록 페이지
    @GetMapping("/auctions/rooms")
    public String auctionRooms() {
        return "auction-rooms";
    }

    // 경매방 상세 페이지
    @GetMapping("/auctions/room/{id}")
    public String auctionRoom(@PathVariable(name = "id") Long id, Model model) {
        model.addAttribute("itemId", id);
        return "auction-room";
    }
}
