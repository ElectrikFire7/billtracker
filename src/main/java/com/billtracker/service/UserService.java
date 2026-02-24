package com.billtracker.service;

import com.billtracker.dto.request.UpdateUserRequest;
import com.billtracker.dto.response.UserResponse;
import com.billtracker.entity.User;
import com.billtracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getCurrentUser(User user) {
        return UserResponse.from(user);
    }

    public UserResponse updateUser(User user, UpdateUserRequest request) {
        user.setName(request.getName());
        user = userRepository.save(user);
        return UserResponse.from(user);
    }
}
