package io.life.masterdata.repository;

import io.life.masterdata.entity.ModulePart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModulePartRepository extends JpaRepository<ModulePart, Long> {
    List<ModulePart> findByModuleId(Long moduleId);
    List<ModulePart> findByPartId(Long partId);
}
