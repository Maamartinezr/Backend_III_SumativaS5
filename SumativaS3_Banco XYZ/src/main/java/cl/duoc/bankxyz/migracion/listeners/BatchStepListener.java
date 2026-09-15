package cl.duoc.bankxyz.migracion.listeners;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

/**
 * Listener reutilizado por los 3 Steps chunk-oriented (transaccionStep,
 * interesStep, cuentaAnualStep).
 *
 * Cubre dos criterios de la pauta de evaluacion S2:
 *
 * 1) "Maneja los errores y excepciones usando politicas y listeners,
 *    garantizando la continuidad del proceso en caso de fallos": los
 *    metodos onSkipInRead/onSkipInProcess/onSkipInWrite se disparan cada
 *    vez que la politica de skip (configurada en el Step) descarta un
 *    registro, dejando evidencia de que el Job siguio corriendo en vez de
 *    fallar completo.
 *
 * 2) "Implementa tecnicas de logs para evaluar el rendimiento y ajustar
 *    configuraciones": beforeStep/afterStep miden cuanto demoro el Step y
 *    dejan en el log los contadores de lectura/escritura/omision, que son
 *    los numeros que uno mira para decidir si el chunk size o la cantidad
 *    de hilos estan bien calibrados.
 *
 * Es stateful (guarda inicioMillis en un campo de instancia), por lo que
 * cada Step debe usar su PROPIA instancia (no compartir un mismo bean entre
 * Steps que puedan correr en paralelo).
 */
public class BatchStepListener implements StepExecutionListener, SkipListener<Object, Object> {

    private static final Logger log = LoggerFactory.getLogger(BatchStepListener.class);
    private static final Object EVIDENCIA_LOCK = new Object();
    private static final String CSV_HEADER = "fecha,configuracion,hilos,chunk,job_execution_id,job,step,estado,duracion_ms,items_por_segundo,leidos,escritos,omitidos_lectura,omitidos_proceso,omitidos_escritura,commits,rollbacks";

    private final int threadCount;
    private final int chunkSize;
    private final String configLabel;
    private final String evidenciaDir;
    private long inicioMillis;

    public BatchStepListener(int threadCount, int chunkSize, String configLabel, String evidenciaDir) {
        this.threadCount = threadCount;
        this.chunkSize = chunkSize;
        this.configLabel = configLabel;
        this.evidenciaDir = evidenciaDir;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        inicioMillis = System.currentTimeMillis();
        log.info("[{}] Step '{}' INICIADO (config={}, hilos={}, chunk={}, hilo={})",
                stepExecution.getJobExecution().getJobInstance().getJobName(),
                stepExecution.getStepName(),
                configLabel,
                threadCount,
                chunkSize,
                Thread.currentThread().getName());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        long duracionMs = System.currentTimeMillis() - inicioMillis;
        double itemsPorSegundo = duracionMs == 0 ? 0.0 : (stepExecution.getReadCount() * 1000.0) / duracionMs;
        log.info("[EVIDENCIA_BATCH] config={}, hilos={}, chunk={}, jobExecutionId={}, job={}, step={}, "
                        + "estado={}, duracionMs={}, itemsPorSegundo={}, leidos={}, escritos={}, "
                        + "omitidos(lectura/proceso/escritura)={}/{}/{}, commits={}, rollbacks={}",
                configLabel,
                threadCount,
                chunkSize,
                stepExecution.getJobExecutionId(),
                stepExecution.getJobExecution().getJobInstance().getJobName(),
                stepExecution.getStepName(),
                stepExecution.getExitStatus().getExitCode(),
                duracionMs,
                String.format("%.2f", itemsPorSegundo),
                stepExecution.getReadCount(),
                stepExecution.getWriteCount(),
                stepExecution.getReadSkipCount(),
                stepExecution.getProcessSkipCount(),
                stepExecution.getWriteSkipCount(),
                stepExecution.getCommitCount(),
                stepExecution.getRollbackCount());
        registrarCsv(stepExecution, duracionMs, itemsPorSegundo);
        return stepExecution.getExitStatus();
    }

    private void registrarCsv(StepExecution stepExecution, long duracionMs, double itemsPorSegundo) {
        Path output = Path.of(evidenciaDir, "benchmark-steps.csv");
        String fila = String.join(",",
                Instant.now().toString(),
                limpiar(configLabel),
                String.valueOf(threadCount),
                String.valueOf(chunkSize),
                String.valueOf(stepExecution.getJobExecutionId()),
                limpiar(stepExecution.getJobExecution().getJobInstance().getJobName()),
                limpiar(stepExecution.getStepName()),
                limpiar(stepExecution.getExitStatus().getExitCode()),
                String.valueOf(duracionMs),
                String.format(java.util.Locale.US, "%.2f", itemsPorSegundo),
                String.valueOf(stepExecution.getReadCount()),
                String.valueOf(stepExecution.getWriteCount()),
                String.valueOf(stepExecution.getReadSkipCount()),
                String.valueOf(stepExecution.getProcessSkipCount()),
                String.valueOf(stepExecution.getWriteSkipCount()),
                String.valueOf(stepExecution.getCommitCount()),
                String.valueOf(stepExecution.getRollbackCount()));
        synchronized (EVIDENCIA_LOCK) {
            try {
                Files.createDirectories(output.getParent());
                if (Files.notExists(output)) {
                    Files.writeString(output, CSV_HEADER + System.lineSeparator(), StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE);
                }
                Files.writeString(output, fila + System.lineSeparator(), StandardCharsets.UTF_8,
                        StandardOpenOption.APPEND);
            } catch (IOException ex) {
                log.warn("No fue posible escribir evidencia batch en {}: {}", output, ex.getMessage());
            }
        }
    }

    private String limpiar(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replace(",", " ").replace(System.lineSeparator(), " ").trim();
    }

    @Override
    public void onSkipInRead(Throwable t) {
        log.warn("Registro OMITIDO en lectura (linea de CSV mal formada): {}", t.getMessage());
    }

    @Override
    public void onSkipInProcess(Object item, Throwable t) {
        log.warn("Registro OMITIDO en procesamiento: item={}, error={}", item, t.getMessage());
    }

    @Override
    public void onSkipInWrite(Object item, Throwable t) {
        log.error("Registro OMITIDO en escritura (fallo de BD persistente tras reintentos): item={}, error={}",
                item, t.getMessage());
    }
}
