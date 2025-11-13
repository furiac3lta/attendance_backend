package com.marcedev.attendance.service.impl;

import com.marcedev.attendance.dto.UserDTO;
import com.marcedev.attendance.entities.Course;
import com.marcedev.attendance.entities.Organization;
import com.marcedev.attendance.entities.User;
import com.marcedev.attendance.enums.Rol;
import com.marcedev.attendance.repository.CourseRepository;
import com.marcedev.attendance.repository.OrganizationRepository;
import com.marcedev.attendance.repository.UserRepository;
import com.marcedev.attendance.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 🧩 Implementación del servicio de usuarios.
 * Incluye lógica de creación, actualización, asignación de cursos
 * y creación de administradores de organización.
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final CourseRepository courseRepository;
    private final PasswordEncoder passwordEncoder;

    // 🔹 Obtener todos los usuarios
    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    // 🔹 Buscar usuario por ID
    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    // 🔹 Buscar usuario por email
    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * 🔹 Crear nuevo usuario
     * Si el usuario autenticado pertenece a una organización, el nuevo usuario
     * se crea automáticamente dentro de esa organización.
     */
    @Override
    public User save(User user) {
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String email = auth.getName();

            userRepository.findByEmail(email).ifPresent(currentUser -> {
                Organization org = currentUser.getOrganization();

                if (org != null && user.getOrganization() == null) {
                    user.setOrganization(org);
                }

                if (org != null && user.getOrganization() != null
                        && !org.getId().equals(user.getOrganization().getId())) {
                    throw new RuntimeException("No puedes asignar usuarios a otra organización");
                }
            });
        }

        return userRepository.save(user);
    }

    // 🔹 Eliminar usuario por ID
    @Override
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    // 🔹 Asignar cursos a un usuario existente
    @Override
    public User assignCourses(Long userId, List<Long> courseIds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        user.getCourses().clear();
        user.getCourses().addAll(courseRepository.findAllById(courseIds));

        userRepository.save(user);
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Error al recargar usuario actualizado"));
    }

    // 🔹 Actualizar datos de usuario
    @Override
    public void updateUser(Long id, User updatedUser) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (updatedUser.getFullName() != null && !updatedUser.getFullName().isBlank()) {
            user.setFullName(updatedUser.getFullName());
        }

        if (updatedUser.getEmail() != null && !updatedUser.getEmail().isBlank()) {
            user.setEmail(updatedUser.getEmail());
        }

        if (updatedUser.getRole() != null) {
            user.setRole(updatedUser.getRole());
        }

        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        }

        if (updatedUser.getOrganization() != null) {
            user.setOrganization(updatedUser.getOrganization());
        }

        userRepository.save(user);
    }

    /**
     * 🔹 Crear un nuevo ADMIN dentro de una organización
     * Solo puede hacerlo un SUPER_ADMIN o un ADMIN de la misma organización.
     */
    @Override
    public User createAdminForOrganization(Long organizationId, User newAdminData) {
        // 1️⃣ Obtener usuario autenticado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado"));

        // 2️⃣ Validar permisos
        if (currentUser.getRole() != Rol.SUPER_ADMIN && currentUser.getRole() != Rol.ADMIN) {
            throw new RuntimeException("Solo los administradores pueden crear otros administradores");
        }

        // 3️⃣ Si es ADMIN, solo puede crear admin dentro de su organización
        if (currentUser.getRole() == Rol.ADMIN) {
            if (currentUser.getOrganization() == null ||
                    !currentUser.getOrganization().getId().equals(organizationId)) {
                throw new RuntimeException("No puedes crear administradores fuera de tu organización");
            }
        }

        // 4️⃣ Validar organización
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organización no encontrada"));

        // 5️⃣ Validar email único
        if (userRepository.existsByEmail(newAdminData.getEmail())) {
            throw new RuntimeException("Ya existe un usuario con el email: " + newAdminData.getEmail());
        }

        // 6️⃣ Validar y encriptar contraseña
        if (newAdminData.getPassword() == null || newAdminData.getPassword().isBlank()) {
            throw new RuntimeException("La contraseña es obligatoria");
        }
        String encodedPassword = passwordEncoder.encode(newAdminData.getPassword());

        // 7️⃣ Crear nuevo administrador
        User admin = User.builder()
                .fullName(newAdminData.getFullName())
                .email(newAdminData.getEmail())
                .password(encodedPassword)
                .role(Rol.ADMIN)
                .organization(org)
                .build();

        return userRepository.save(admin);
    }
    @Override
    public UserDTO updateUser(Long id, UserDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 🔥 Actualizamos solo los campos editables
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setRole(Rol.valueOf(dto.getRole()));

        if (dto.getOrganizationId() != null) {
            user.setOrganization(
                    organizationRepository.findById(dto.getOrganizationId())
                            .orElseThrow(() -> new RuntimeException("Organización no encontrada"))
            );
        }


        // Guardamos cambios
        User saved = userRepository.save(user);

        // Devolvemos DTO actualizado
        return new UserDTO(
                saved.getId(),
                saved.getFullName(),
                saved.getEmail(),
                saved.getRole().name(),
                saved.getOrganization() != null ? saved.getOrganization().getName() : null,
                saved.getCourses() != null ? saved.getCourses().stream().map(c -> c.getName()).toList() : List.of(),
                saved.getOrganization() != null ? saved.getOrganization().getId() : null
        );
    }

}
