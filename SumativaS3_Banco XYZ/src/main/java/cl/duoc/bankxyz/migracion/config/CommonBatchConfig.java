package cl.duoc.bankxyz.migracion.config;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Beans compartidos por los 3 Jobs.
 */
@Configuration
public class CommonBatchConfig {

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new JdbcTransactionManager(dataSource);
    }

    /**
     * Escalamiento: pool configurable para comparar ejecuciones reales con
     * distinta cantidad de hilos. El valor por defecto es 3, pero puede
     * cambiarse al iniciar la aplicacion con bankxyz.batch.thread-count.
     *
     * setQueueCapacity(0) + CallerRunsPolicy: si en algun momento se
     * disparara mas de un Job en paralelo y se agotan los 3 hilos, el chunk
     * extra se ejecuta en el hilo que lo solicito en vez de acumularse en
     * una cola indefinida o perderse.
     */
    @Bean
    public TaskExecutor batchTaskExecutor(@Value("${bankxyz.batch.thread-count:3}") int threadCount,
                                          @Value("${bankxyz.batch.queue-capacity:0}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threadCount);
        executor.setMaxPoolSize(threadCount);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("bankxyz-batch-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }
}
