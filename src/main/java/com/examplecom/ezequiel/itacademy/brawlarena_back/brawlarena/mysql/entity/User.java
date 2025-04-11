package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.entity;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.common.constant.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.List;


@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @Column("id")
    private Long id;

    @NotBlank(message = "El nickname no puede estar vacío")
    @Size(min = 3, max = 20, message = "El nickname debe tener entre 3 y 20 caracteres")
    @Column("nickname")
    private String nickname;

    @NotBlank(message = "La contraseña no puede estar vacía")
    @Size(min = 8, max = 20, message = "La contraseña debe tener entre 8 y 20 caracteres")
    @Column("password")
    private String password;

    @Column("tokens")
    private Integer tokens;

    @Column("role")
    private Role role;

    @Column("gallery")
    private List<Long> characterIds;

}