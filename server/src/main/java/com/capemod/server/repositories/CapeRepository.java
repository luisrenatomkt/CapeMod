package com.capemod.server.repositories;

import com.capemod.server.models.Cape;
import java.util.List;

public interface CapeRepository {
    void insert(Cape cape);
    Cape findById(String id);
    List<Cape> findByOwner(String ownerId);
    int countByOwner(String ownerId);
    boolean delete(String id);
    int countBySha(String sha256);
}
