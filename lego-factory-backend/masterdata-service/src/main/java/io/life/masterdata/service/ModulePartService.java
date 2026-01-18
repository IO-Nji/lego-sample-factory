package io.life.masterdata.service;

import io.life.masterdata.entity.ModulePart;
import io.life.masterdata.repository.ModulePartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ModulePartService {
    private final ModulePartRepository repository;

    @Nullable
    public ModulePart save(ModulePart modulePart) {
        return repository.save(modulePart);
    }

    public List<ModulePart> findAll() {
        return repository.findAll();
    }

    public List<ModulePart> findByModuleId(Long moduleId) {
        return repository.findByModuleId(moduleId);
    }

    public List<ModulePart> findByPartId(Long partId) {
        return repository.findByPartId(partId);
    }
}
