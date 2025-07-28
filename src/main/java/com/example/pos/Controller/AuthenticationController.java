
package com.example.pos.Controller;

import com.example.pos.DTO.AuthenticationDto;
import com.example.pos.Service.AuthenticationService;
import com.example.pos.util.Status;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/authentication")
public class AuthenticationController {
    @Autowired
    private AuthenticationService authenticationService;

    @PostMapping("/register")
    public Status registerUser(@RequestBody AuthenticationDto authenticationDto){
        return authenticationService.register(authenticationDto);
    }
    @PostMapping("/login")
    public Status login(@RequestParam String username, @RequestParam String password) {
        return authenticationService.login(username,password);
    }
    @GetMapping("/validate")
    public Status validateToken(@RequestParam String token) {
        return authenticationService.validateSession(token);
    }
}
