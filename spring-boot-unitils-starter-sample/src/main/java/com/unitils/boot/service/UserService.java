package com.unitils.boot.service;

import com.unitils.boot.mapper.UserMapper;
import com.unitils.boot.model.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper ;

    public UserDTO getUsername(int userId){
        return userMapper.selectByUserId(userId);
    }
}
