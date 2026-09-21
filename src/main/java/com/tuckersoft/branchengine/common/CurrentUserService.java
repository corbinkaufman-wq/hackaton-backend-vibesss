package com.tuckersoft.branchengine.common;

import com.tuckersoft.branchengine.common.exception.ResourceNotFoundException;
import com.tuckersoft.branchengine.entity.User;
import com.tuckersoft.branchengine.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Contrato con la estrella Seguridad: el filtro JWT deja en el
 * SecurityContextHolder una Authentication cuyo getName() es el EMAIL del
 * usuario (el username del UserDetailsService). Aqui solo se recarga la
 * entidad completa desde la base de datos.
 */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado: " + email));
    }

    public boolean isAdmin(User user) {
        return ROLE_ADMIN.equals(user.getRole());
    }
}
