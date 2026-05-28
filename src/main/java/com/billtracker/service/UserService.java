package com.billtracker.service;

import com.billtracker.dto.request.UpdateUserRequest;
import com.billtracker.dto.response.UserResponse;
import com.billtracker.entity.User;
import com.billtracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getCurrentUser(User user) {
        return UserResponse.from(user);
    }

    public UserResponse updateUser(User user, UpdateUserRequest request) {
        user.setName(request.getName());
        user.setUpiId(request.getUpiId());
        user = userRepository.save(user);
        return UserResponse.from(user);
    }

    public List<UserResponse> searchUsers(String query) {
        return userRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(query, query)
                .stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }
}
