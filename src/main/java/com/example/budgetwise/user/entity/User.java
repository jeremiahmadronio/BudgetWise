package com.example.budgetwise.user.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Getter
@Setter
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column
    private String password;



    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private  Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", length = 20)
    private AuthProvider authProvider;

    @Column
    private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Status status;


    @Column
    private String address;

    @CreationTimestamp
    @Column(updatable = false , nullable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    private LocalDateTime updatedDate;




    public enum Role{
        ADMIN,
        USER
    }

    public enum AuthProvider{
        LOCAL,
        GOOGLE
    }

    public enum Status{
        ACTIVE,
        INACTIVE
    }

}
