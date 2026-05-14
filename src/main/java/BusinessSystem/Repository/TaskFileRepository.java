package BusinessSystem.Repository;

import BusinessSystem.Model.Task;
import BusinessSystem.Model.TaskFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskFileRepository extends JpaRepository<TaskFile, Long> {

    List<TaskFile> findByTask(Task task);
}
