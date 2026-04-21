package mk.ukim.finki.konsultacii.calendar.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Component
@Slf4j
public class CalendarReverseSyncTaskDispatcher {

    private final TaskExecutor taskExecutor;
    private final Set<String> runningTasks = ConcurrentHashMap.newKeySet();

    public CalendarReverseSyncTaskDispatcher(@Qualifier("calendarReverseSyncExecutor") TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    public void dispatch(String provider, List<String> professorIds, Consumer<String> syncAction) {
        for (String professorId : professorIds) {
            String taskKey = provider + ":" + professorId;

            if (!runningTasks.add(taskKey)) {
                log.debug("{} reverse sync for professor {} is already running, skipping this cycle", provider, professorId);
                continue;
            }

            try {
                taskExecutor.execute(() -> runSync(provider, professorId, taskKey, syncAction));
            } catch (TaskRejectedException e) {
                runningTasks.remove(taskKey);
                log.warn("{} reverse sync queue is full; professor {} will be checked on the next cycle", provider, professorId);
            }
        }
    }

    private void runSync(String provider, String professorId, String taskKey, Consumer<String> syncAction) {
        try {
            syncAction.accept(professorId);
        } catch (Exception e) {
            log.error("{} reverse sync failed for professor {}: {}", provider, professorId, e.getMessage(), e);
        } finally {
            runningTasks.remove(taskKey);
        }
    }
}
