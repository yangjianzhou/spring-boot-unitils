package com.unitils.boot.controller;

import com.unitils.boot.model.UserDTO;
import com.unitils.boot.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("get-username")
    public String getUsername(@RequestParam("userId") Integer userId) {
        UserDTO userDTO = userService.getUsername(userId);
        return userDTO.getName();
    }
}
