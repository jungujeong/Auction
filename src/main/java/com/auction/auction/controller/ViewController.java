package com.auction.auction.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping("/")
    public String index() {
        return "index.html";
    }

    @GetMapping("/login")
    public String login() {
        return "login.html";
    }

    @GetMapping("/signup")
    public String signup() {
        return "signup.html";
    }

    @GetMapping("/items")
    public String items() {
        return "items.html";
    }

    @GetMapping("/register-item")
    public String registerItem() {
        return "register-item.html";
    }

    @GetMapping("/auction-detail")
    public String auctionDetail() {
        return "auction-detail.html";
    }
}
