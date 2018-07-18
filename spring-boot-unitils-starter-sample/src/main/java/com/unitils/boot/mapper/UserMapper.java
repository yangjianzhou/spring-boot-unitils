package com.unitils.boot.mapper;

import com.unitils.boot.model.UserDTO;

public interface UserMapper {

    public UserDTO selectByUserId(int userId) ;
}
