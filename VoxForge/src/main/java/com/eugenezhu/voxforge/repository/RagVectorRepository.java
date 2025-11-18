package com.eugenezhu.voxforge.repository;

import com.eugenezhu.voxforge.model.CommandTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @projectName: VoxForge
 * @package: com.eugenezhu.voxforge.repository
 * @className: RagVectorRepository
 * @author: zhuyuchen
 * @description: TODO
 * @date: 2025/11/17 下午9:31
 */
@Repository
@RequiredArgsConstructor
public class RagVectorRepository {

    private final DatabaseClient databaseClient;

    public Mono<Void> initializeSchema(int dimension) {
        String createExt = "CREATE EXTENSION IF NOT EXISTS vector";
        String createTable = "CREATE TABLE IF NOT EXISTS kb_commands (" +
                "id BIGSERIAL PRIMARY KEY, " +
                "cmd TEXT NOT NULL, " +
                "description TEXT NOT NULL, " +
                "os TEXT NOT NULL, " +
                "shell TEXT NOT NULL, " +
                "embedding VECTOR(" + dimension + ") NOT NULL)";
        String uniqueIdx = "CREATE UNIQUE INDEX IF NOT EXISTS kb_commands_unique ON kb_commands (cmd, os, shell)";
        String hnswIdx = "CREATE INDEX IF NOT EXISTS idx_kb_commands_embedding_hnsw ON kb_commands USING hnsw (embedding vector_cosine_ops)";
        String osIdx = "CREATE INDEX IF NOT EXISTS idx_kb_commands_os ON kb_commands (os)";

        return databaseClient.sql(createExt).fetch().rowsUpdated()
                .then(databaseClient.sql(createTable).fetch().rowsUpdated())
                .then(databaseClient.sql(uniqueIdx).fetch().rowsUpdated())
                .then(databaseClient.sql(hnswIdx).fetch().rowsUpdated())
                .then(databaseClient.sql(osIdx).fetch().rowsUpdated())
                .then();
    }

    public Mono<Void> upsert(CommandTemplate t, double[] embedding) {
        String emb = toVectorLiteral(embedding);
        String sql = "INSERT INTO kb_commands (cmd, description, os, shell, embedding) " +
                "VALUES (:cmd, :desc, :os, :shell, CAST(:emb AS vector)) " +
                "ON CONFLICT (cmd, os, shell) DO UPDATE SET description = EXCLUDED.description, embedding = EXCLUDED.embedding";
        return databaseClient.sql(sql)
                .bind("cmd", t.getCmd())
                .bind("desc", t.getDesc())
                .bind("os", t.getOs())
                .bind("shell", t.getShell())
                .bind("emb", emb)
                .fetch().rowsUpdated().then();
    }

    public Mono<Boolean> exists(String cmd, String os, String shell) {
        String sql = "SELECT 1 FROM kb_commands WHERE cmd = :cmd AND os = :os AND shell = :shell LIMIT 1";
        return databaseClient.sql(sql)
                .bind("cmd", cmd)
                .bind("os", os)
                .bind("shell", shell)
                .map((row, meta) -> 1)
                .first()
                .map(v -> true)
                .defaultIfEmpty(false);
    }

    public Flux<CommandTemplate> search(String os, double[] query, int k) {
        String emb = toVectorLiteral(query);
        String sql = "SELECT cmd, description, os, shell FROM kb_commands WHERE os = :os ORDER BY embedding <-> CAST(:emb AS vector) LIMIT :limit";
        return databaseClient.sql(sql)
                .bind("os", os)
                .bind("emb", emb)
                .bind("limit", k)
                .map((row, meta) -> new CommandTemplate(
                        row.get("cmd", String.class),
                        row.get("description", String.class),
                        row.get("os", String.class),
                        row.get("shell", String.class)
                ))
                .all();
    }

    private String toVectorLiteral(double[] v) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(v[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}

