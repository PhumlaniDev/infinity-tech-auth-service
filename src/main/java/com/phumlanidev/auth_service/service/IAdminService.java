package com.phumlanidev.auth_service.service;

import com.phumlanidev.auth_service.dto.CreateUserRequest;
import com.phumlanidev.auth_service.dto.UserDto;
import com.phumlanidev.auth_service.dto.UserSummaryDto;

import java.util.List;

public interface IAdminService {

    List<UserSummaryDto> getAllUsers();

    void createUser(UserDto userDto);

    void updateUserRoles(String userId, List<String> roles);

    void setUserEnabled(String userId, boolean enable);

    void deleteUser(String userId);
}
