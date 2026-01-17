package com.example.budgetwise.security;

public class SecurityConstants {

    // ==================== ROLES ====================
    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";

    // ==================== PUBLIC ENDPOINTS ====================
    public static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/**",
            "/oauth2/**",
            "/error"
    };

    // ==================== ADMIN ENDPOINTS ====================
    public static final String[] ADMIN_ENDPOINTS = {
            "/api/v1/admin/**"
    };

    // ==================== USER ENDPOINTS ====================
    public static final String[] USER_ENDPOINTS = {
            "/api/v1/user/**"
    };





    // ==================== CORS CONFIGURATION ====================
    public static final String[] ALLOWED_ORIGINS = {
            "http://localhost:5173",
            "http://localhost:3000"
    };

    public static final String[] ALLOWED_METHODS = {
            "GET",
            "POST",
            "PUT",
            "DELETE",
            "PATCH",
            "OPTIONS"
    };

    public static final String[] ALLOWED_HEADERS = {
            "*"
    };

    // ==================== JWT CONFIGURATION ====================
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";

    // Prevent instantiation
    private SecurityConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}