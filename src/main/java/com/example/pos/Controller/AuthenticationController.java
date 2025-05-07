
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

    @PostMapping("/login")
    public Status authentication(@RequestBody AuthenticationDto authenticationDto){
        return authenticationService.authentication(authenticationDto);
    }
    @PutMapping("/updatePassword/{username}")
    public Status passwordUpdated(@PathVariable String username,@RequestBody AuthenticationDto authenticationDto){
        return authenticationService.passwordUpdated(username,authenticationDto);
    }
}
