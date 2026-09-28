package com.foodnest.foodnest.repository;

import com.foodnest.foodnest.entity.Role;
import com.foodnest.foodnest.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
}
