package com.example.pos.Service;

import com.example.pos.DTO.AuthenticationDto;
import com.example.pos.entity.Authentication;
import com.example.pos.repo.AuthenticationRepo;
import com.example.pos.util.Status;
import com.example.pos.util.StatusMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class AuthenticationService {
    @Autowired
    private AuthenticationRepo authenticationRepo;

    public Status authentication(AuthenticationDto authenticationDto) {

        Authentication userExist = authenticationRepo.findByUsername(authenticationDto.getUsername());
        Authentication passwordExist=authenticationRepo.findByPassword(authenticationDto.getPassword());
        if (Objects.nonNull(userExist)||Objects.nonNull(passwordExist)) {
            return new Status(StatusMessage.FAILURE, "Create strong username and password");
        }
        Authentication authentication = new Authentication();
        authentication.setUsername(authenticationDto.getUsername());
        authentication.setPassword(authenticationDto.getPassword());
        authenticationRepo.save(authentication);
        return new Status(StatusMessage.SUCCESS, "User Login Successfully");
    }

    public Status passwordUpdated(String username,AuthenticationDto authenticationDto) {
        Authentication authentication = authenticationRepo.findByUsername(username);
        if (Objects.isNull(authentication)) {
            return new Status(StatusMessage.SUCCESS, "User not exist");
        }
        if(Objects.nonNull(authentication.getPassword())){
            authentication.setPassword(authenticationDto.getPassword());
        }
        authenticationRepo.save(authentication);
        return new Status(StatusMessage.SUCCESS,authentication);
    }
}
