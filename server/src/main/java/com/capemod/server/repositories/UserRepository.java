package com.capemod.server.repositories;

import com.capemod.server.models.User;

public interface UserRepository {
    /** Retorna false se o nome de usuario (sem diferenciar maiusculas) ja existe. */
    boolean insert(User user);
    User findByUsername(String username);
    User findById(String id);
}
