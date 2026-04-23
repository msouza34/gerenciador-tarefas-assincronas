package io.github.gerenciadortarefas.workerservice.repository;

import io.github.gerenciadortarefas.workerservice.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
}

