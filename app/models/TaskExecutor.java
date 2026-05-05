package models;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.INTERNAL_SERVER_ERROR;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.api.client.util.Throwables;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Provider;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Summary;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import play.libs.Json;
import models.CustomerTask.TaskType;
/**
 * TaskExecutor is the executor service for tasks and their subtasks. It is very similar to the
 * current SubTaskGroupQueue and SubTaskGroup.
 *
 * <p>A task is submitted by first creating a RunnableTask.
 *
 * <pre>
 * RunnableTask runnableTask = taskExecutor.createRunnableTask(taskType, taskParams);
 * UUID taskUUID = taskExecutor.submit(runnableTask, executor);
 * </pre>
 *
 * The RunnableTask instance is first retrieved in the run() method of the task.
 *
 * <pre>
 * RunnableTask runnableTask = taskExecutor.getRunnableTask(UUID).
 * </pre>
 *
 * This is similar to the current implementation of SubTaskGroupQueue queue = new
 * SubTaskGroupQueue(UUID).
 *
 * <p>The subtasks are added by first adding them to their groups and followed by adding the groups
 * to the RunnableTask instance.
 *
 * <pre>
 * void createProvisionNodes(List<Node> nodes, SubTaskGroupType groupType) {
 *   SubTasksGroup group = taskExecutor.createSubTaskGroup("provision-nodes", groupType);
 *   for (Node node : nodes) {
 *     // Create the subtask instance and initialize.
 *     ITask subTask = createAndInitSubTask(node);
 *     // Add the concurrent subtasks to the group.
 *     group.addSubTask(subTask);
 *   }
 *   runnableTask.addSubTaskGroup(group);
 * }
 * </pre>
 *
 * After all the subtasks are added, runSubTasks() is invoked in the run() method of the task. e.g
 *
 * <pre>
 * // Run method of task
 * void run() {
 *   // Same as the current queue = new SubTaskGroupQueue(UUID);
 *   runnableTask = taskExecutor.getRunnableTask(UUID).
 *   // Creates subtask group which is then added to runnableTask.
 *   createTasks1(nodes);
 *   createTasks2(nodes);
 *   createTasks3(nodes);
 *   // Same as the current queue.run();
 *   runnableTask.runSubTasks();
 * }
 * </pre>
 */
@Singleton
@Slf4j
public class TaskExecutor {


  // Task futures are waited for this long before checking abort status.
  private static final long TASK_SPIN_WAIT_INTERVAL_MS = 2000;

  // Max size of the callstack for task creator thread.
  private static final int MAX_TASK_CREATOR_CALLSTACK_SIZE = 15;

  private static final String TASK_EXECUTION_SKIPPED_LABEL = "skipped";

  // Default wait timeout for subtasks to complete since the abort call.
  private final Duration defaultAbortTaskTimeout = Duration.ofSeconds(30);


  private final AtomicBoolean isShutdown = new AtomicBoolean();

  private static final String COMMISSIONER_TASK_WAITING_SEC_METRIC =
          "ybp_commissioner_task_waiting_sec";

  private static final String COMMISSIONER_TASK_EXECUTION_SEC_METRIC =
          "ybp_commissioner_task_execution_sec";

}
