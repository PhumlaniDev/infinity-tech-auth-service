package com.phumlanidev.authservice.service;

import com.phumlanidev.authservice.dto.UserDto;
import com.phumlanidev.authservice.dto.UserSummaryDto;

import java.util.List;

public interface IAdminService {

    List<UserSummaryDto> getAllUsers();

    void createUser(UserDto userDto);

    void updateUserRoles(String userId, List<String> roles);

    void setUserEnabled(String userId, boolean enable);

    void deleteUser(String userId);
}
