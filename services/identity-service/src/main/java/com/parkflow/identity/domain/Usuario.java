package com.parkflow.identity.domain;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import jakarta.persistence.*;

@Entity @Table(name="usuarios")
public class Usuario {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @Column(nullable=false, length=120) private String nombre;
    @Column(nullable=false, length=150) private String email;
    @Column(name="password_hash", nullable=false, length=255) private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=15) private EstadoUsuario estado=EstadoUsuario.ACTIVO;
    @Column(name="creado_en", nullable=false, updatable=false) private Instant createdAt;
    @Column(name="actualizado_en", nullable=false) private Instant updatedAt;
    @ManyToMany(fetch=FetchType.LAZY)
    @JoinTable(name="usuario_roles", joinColumns=@JoinColumn(name="usuario_id"), inverseJoinColumns=@JoinColumn(name="rol_id"))
    private Set<Rol> roles=new HashSet<>();
    protected Usuario() {}
    public Usuario(String nombre,String email,String passwordHash){this.nombre=nombre;this.email=email;this.passwordHash=passwordHash;}
    @PrePersist void onCreate(){createdAt=updatedAt=Instant.now();}
    @PreUpdate void onUpdate(){updatedAt=Instant.now();}
    public void updateDetails(String nombre,String email){this.nombre=nombre;this.email=email;}
    public void setPasswordHash(String passwordHash){this.passwordHash=passwordHash;}
    public void setEstado(EstadoUsuario estado){this.estado=estado;}
    public void setRoles(Set<Rol> roles){this.roles=new HashSet<>(roles);}
    public void addRol(Rol rol){roles.add(rol);}
    public UUID getId(){return id;} public String getNombre(){return nombre;} public String getEmail(){return email;} public String getPasswordHash(){return passwordHash;} public EstadoUsuario getEstado(){return estado;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;} public Set<Rol> getRoles(){return roles;}
}
