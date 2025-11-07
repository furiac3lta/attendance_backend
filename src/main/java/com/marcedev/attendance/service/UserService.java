package com.marcedev.attendance.service;

import com.marcedev.attendance.entities.User;
import java.util.List;
import java.util.Optional;

/**
 * 🔹 Interfaz del servicio de usuarios
 * Maneja la lógica de negocio relacionada con la gestión de usuarios.
 */
public interface UserService {

    List<User> findAll();                            // Obtener todos los usuarios
    Optional<User> findById(Long id);                // Buscar usuario por ID
    Optional<User> findByEmail(String email);        // Buscar usuario por email
    User save(User user);                            // Crear nuevo usuario (con asignación automática de organización)
    void deleteById(Long id);                        // Eliminar usuario por ID
    User assignCourses(Long userId, List<Long> courseIds); // Asignar cursos a un usuario (devuelve el usuario actualizado)
    void updateUser(Long id, User updatedUser);      // Actualizar datos del usuario
    User createAdminForOrganization(Long organizationId, User newAdminData);

}
