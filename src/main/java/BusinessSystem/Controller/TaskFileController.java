package BusinessSystem.Controller;

import BusinessSystem.Model.TaskFile;
import BusinessSystem.Model.User;
import BusinessSystem.Security.UserPrincipal;
import BusinessSystem.Service.FileStorageService;
import BusinessSystem.Service.TaskFileService;
import BusinessSystem.Web.CurrentUserHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class TaskFileController {

    private final TaskFileService taskFileService;
    private final FileStorageService fileStorageService;

    @PostMapping("/api/tasks/{taskId}/files")
    public ResponseEntity<TaskFile> upload(@PathVariable Long taskId,
                                           @RequestParam("file") MultipartFile file,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        User user = CurrentUserHelper.require(principal);
        return ResponseEntity.ok(taskFileService.upload(taskId, file, user));
    }

    @GetMapping("/api/tasks/{taskId}/files")
    public ResponseEntity<List<TaskFile>> list(@PathVariable Long taskId,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        User user = CurrentUserHelper.require(principal);
        return ResponseEntity.ok(taskFileService.listForTask(taskId, user));
    }

    @GetMapping("/api/files/{fileId}")
    public ResponseEntity<TaskFile> meta(@PathVariable Long fileId,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        User user = CurrentUserHelper.require(principal);
        return ResponseEntity.ok(taskFileService.get(fileId, user));
    }

    @GetMapping("/api/files/{fileId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long fileId,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        User user = CurrentUserHelper.require(principal);
        TaskFile meta = taskFileService.get(fileId, user);
        Resource resource = fileStorageService.loadAsResource(meta.getStoragePath());

        MediaType mediaType = meta.getContentType() != null
                ? MediaType.parseMediaType(meta.getContentType())
                : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + meta.getFileName() + "\"")
                .body(resource);
    }

    @DeleteMapping("/api/files/{fileId}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long fileId,
                                                      @AuthenticationPrincipal UserPrincipal principal) {
        User user = CurrentUserHelper.require(principal);
        taskFileService.delete(fileId, user);
        return ResponseEntity.ok(Map.of("message", "File deleted"));
    }
}
