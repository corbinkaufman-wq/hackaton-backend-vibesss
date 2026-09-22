package com.tuckersoft.branchengine.user;

import com.tuckersoft.branchengine.common.CurrentUserService;
import com.tuckersoft.branchengine.common.exception.BadRequestException;
import com.tuckersoft.branchengine.common.exception.ResourceNotFoundException;
import com.tuckersoft.branchengine.entity.User;
import com.tuckersoft.branchengine.repository.UserRepository;
import com.tuckersoft.branchengine.user.dto.UpdateRoleRequest;
import com.tuckersoft.branchengine.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Set<String> VALID_ROLES = Set.of("ROLE_USER", "ROLE_ADMIN");

    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public UserResponse me() {
        return toResponse(currentUserService.getCurrentUser());
    }

    public List<UserResponse> list() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public UserResponse updateRole(Long id, UpdateRoleRequest request) {
        if (!VALID_ROLES.contains(request.role())) {
            throw new BadRequestException("role debe ser ROLE_USER o ROLE_ADMIN");
        }
        User current = currentUserService.getCurrentUser();
        if (current.getId().equals(id)) {
            throw new BadRequestException("No puedes cambiar tu propio rol");
        }
        User target = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un usuario con id " + id));
        target.setRole(request.role());
        target = userRepository.save(target);
        return toResponse(target);
    }

    private UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getDisplayName(), u.getRole(), u.getCreatedAt());
    }
}
