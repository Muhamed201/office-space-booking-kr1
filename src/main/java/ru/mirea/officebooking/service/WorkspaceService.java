package ru.mirea.officebooking.service;

import ru.mirea.officebooking.exception.EntityNotFoundException;
import ru.mirea.officebooking.exception.InvalidEntityException;
import ru.mirea.officebooking.model.Workspace;
import ru.mirea.officebooking.model.WorkspaceStatus;
import ru.mirea.officebooking.repository.WorkspaceRepository;

import java.util.List;

public class WorkspaceService {
    private final WorkspaceRepository workspaceRepository;

    public WorkspaceService(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    public List<Workspace> getAll() {
        return workspaceRepository.findAll();
    }

    public Workspace getById(Long id) {
        return workspaceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Рабочее место с id " + id + " не найдено"));
    }

    public void create(Workspace workspace) {
        if (workspace.getName() == null || workspace.getName().trim().isEmpty()) {
            throw new InvalidEntityException("Название рабочего места не может быть пустым");
        }
        if (workspace.getStatus() == null) {
            workspace.setStatus(WorkspaceStatus.AVAILABLE);
        }
        workspaceRepository.save(workspace);
    }

    public void update(Workspace workspace) {
        workspaceRepository.update(workspace);
    }

    public void delete(Long id) {
        workspaceRepository.delete(id);
    }

    // Требование: доступность места
    public boolean isAvailable(Long workspaceId) {
        Workspace workspace = getById(workspaceId);
        return workspace.getStatus() == WorkspaceStatus.AVAILABLE;
    }

    // Требование: проверка вместимости
    public boolean checkCapacity(Long workspaceId, int peopleCount) {
        Workspace workspace = getById(workspaceId);
        if (peopleCount <= 0) {
            throw new InvalidEntityException("Количество людей должно быть больше 0");
        }
        return workspace.hasCapacity(peopleCount); 
    }
}