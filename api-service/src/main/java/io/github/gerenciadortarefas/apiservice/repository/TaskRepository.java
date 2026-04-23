package io.github.gerenciadortarefas.apiservice.repository;

import io.github.gerenciadortarefas.apiservice.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
}

