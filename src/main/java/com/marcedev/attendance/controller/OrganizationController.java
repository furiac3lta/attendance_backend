package com.marcedev.attendance.controller;

import com.marcedev.attendance.dto.OrganizationDTO;
import com.marcedev.attendance.entities.Organization;
import com.marcedev.attendance.entities.User;
import com.marcedev.attendance.enums.Rol;
import com.marcedev.attendance.repository.OrganizationRepository;
import com.marcedev.attendance.repository.UserRepository;
import com.marcedev.attendance.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor

public class OrganizationController {

    private final OrganizationService organizationService;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    // 🔹 Listar todas las organizaciones (solo SUPER_ADMIN)
    @GetMapping
    public ResponseEntity<?> getAllOrganizations() {
        User currentUser = getAuthenticatedUser();

        // 🟣 SUPER_ADMIN ve todas las organizaciones
        if (currentUser.getRole() == Rol.SUPER_ADMIN) {
            var dtos = organizationRepository.findAll().stream()
                    .map(org -> OrganizationDTO.builder()
                            .id(org.getId())
                            .name(org.getName())
                            .type(org.getType())
                            .phone(org.getPhone())
                            .address(org.getAddress())
                            .logoUrl(org.getLogoUrl())
                            .adminFullName(org.getAdmin() != null ? org.getAdmin().getFullName() : null) // ✅ AQUI
                            .build())
                    .toList();

            return ResponseEntity.ok(dtos);
        }

        // 🔵 ADMIN ve solo su organización
        if (currentUser.getRole() == Rol.ADMIN) {
            if (currentUser.getOrganization() == null) {
                return ResponseEntity.badRequest().body("⚠️ No tiene organización asignada");
            }

            var org = currentUser.getOrganization();
            var dto = OrganizationDTO.builder()
                    .id(org.getId())
                    .name(org.getName())
                    .type(org.getType())
                    .phone(org.getPhone())
                    .address(org.getAddress())
                    .logoUrl(org.getLogoUrl())
                    .adminFullName(org.getAdmin() != null ? org.getAdmin().getFullName() : null) // ✅ AQUI
                    .build();

            return ResponseEntity.ok(List.of(dto));
        }

        // 🔴 INSTRUCTOR / USER no pueden ver organizaciones
        return ResponseEntity.status(403)
                .body("🚫 No tiene permisos para ver organizaciones");
    }

    // 🔹 Crear organización (solo SUPER_ADMIN)
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Organization organization) {
        User current = getAuthenticatedUser();

        if (current.getRole() != Rol.SUPER_ADMIN) {
            return ResponseEntity.status(403).body("🚫 Solo SUPER_ADMIN puede crear organizaciones.");
        }

        Organization saved = organizationRepository.save(organization);
        return ResponseEntity.ok(saved);
    }

    // 🔹 Eliminar organización (solo SUPER_ADMIN)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrganization(@PathVariable Long id) {
        // … validaciones de rol, etc.
        organizationService.deleteById(id);
        return ResponseEntity.noContent().build(); // 204 sin body
    }

    // 🧩 Helper: obtener usuario autenticado actual
    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("Usuario no autenticado");
        }

        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + email));
    }

    @PutMapping("/{organizationId}/assign-admin/{userId}")
    public ResponseEntity<?> assignAdmin(
            @PathVariable Long organizationId,
            @PathVariable Long userId) {

        User currentUser = getAuthenticatedUser();

        if (currentUser.getRole() != Rol.SUPER_ADMIN) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "🚫 Solo el SUPER_ADMIN puede asignar administradores."));
        }

        organizationService.assignAdmin(organizationId, userId);

        // ✅ Enviar JSON en vez de texto plano (Angular lo interpreta bien)
        return ResponseEntity.ok(Map.of("message", "✅ Administrador asignado correctamente."));
    }

}
