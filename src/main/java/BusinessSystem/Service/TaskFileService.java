package BusinessSystem.Service;

import BusinessSystem.Enums.Role;
import BusinessSystem.Model.Task;
import BusinessSystem.Model.TaskFile;
import BusinessSystem.Model.User;
import BusinessSystem.Repository.TaskFileRepository;
import BusinessSystem.Repository.TaskRepository;
import BusinessSystem.Web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Timestamp;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskFileService {

    private final TaskFileRepository taskFileRepository;
    private final TaskRepository taskRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public TaskFile upload(Long taskId, MultipartFile file, User actor) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Task not found"));

        requireUploadAccess(task, actor);

        String storagePath = fileStorageService.store(taskId, file);

        TaskFile tf = new TaskFile();
        tf.setTask(task);
        tf.setUploadedBy(actor);
        tf.setFileName(file.getOriginalFilename());
        tf.setContentType(file.getContentType());
        tf.setFileSize(file.getSize());
        tf.setStoragePath(storagePath);
        tf.setUploadedAt(new Timestamp(System.currentTimeMillis()));
        return taskFileRepository.save(tf);
    }

    public List<TaskFile> listForTask(Long taskId, User actor) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Task not found"));
        requireViewAccess(task, actor);
        return taskFileRepository.findByTask(task);
    }

    public TaskFile get(Long fileId, User actor) {
        TaskFile f = taskFileRepository.findById(fileId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "File not found"));
        requireViewAccess(f.getTask(), actor);
        return f;
    }

    @Transactional
    public void delete(Long fileId, User actor) {
        TaskFile f = taskFileRepository.findById(fileId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "File not found"));
        if (actor.getRole() != Role.SUPERVISOR
                && (f.getUploadedBy() == null || !f.getUploadedBy().getId().equals(actor.getId()))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Cannot delete this file");
        }
        fileStorageService.delete(f.getStoragePath());
        taskFileRepository.delete(f);
    }

    private void requireViewAccess(Task task, User actor) {
        if (actor.getRole() == Role.SUPERVISOR) return;
        if (task.getAssignedTo() != null && task.getAssignedTo().getId().equals(actor.getId())) return;
        throw new ApiException(HttpStatus.FORBIDDEN, "No access to this task");
    }

    private void requireUploadAccess(Task task, User actor) {
        if (actor.getRole() == Role.SUPERVISOR) return;
        if (task.getAssignedTo() != null && task.getAssignedTo().getId().equals(actor.getId())) return;
        throw new ApiException(HttpStatus.FORBIDDEN, "Cannot upload to this task");
    }
}
