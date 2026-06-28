package com.jmonitor.server.diagnostics;

import com.jmonitor.common.dto.HeapDumpInfo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

/**
 * H2-backed registry of captured heap dumps (Phase 4).
 */
@Repository
public class HeapDumpRegistry {

    private static final RowMapper<HeapDumpInfo> MAPPER = (rs, n) -> new HeapDumpInfo(
            rs.getLong("id"),
            rs.getLong("pid"),
            rs.getString("file_name"),
            rs.getLong("size_bytes"),
            rs.getLong("created_millis"),
            rs.getBoolean("live"));

    private final JdbcTemplate jdbc;

    public HeapDumpRegistry(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public HeapDumpInfo insert(long pid, String fileName, long sizeBytes, long createdMillis, boolean live) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO heap_dump (pid, file_name, size_bytes, created_millis, live) VALUES (?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, pid);
            ps.setString(2, fileName);
            ps.setLong(3, sizeBytes);
            ps.setLong(4, createdMillis);
            ps.setBoolean(5, live);
            return ps;
        }, keys);
        long id = keys.getKey() == null ? -1 : keys.getKey().longValue();
        return new HeapDumpInfo(id, pid, fileName, sizeBytes, createdMillis, live);
    }

    public List<HeapDumpInfo> listAll() {
        return jdbc.query("SELECT * FROM heap_dump ORDER BY created_millis DESC", MAPPER);
    }

    public List<HeapDumpInfo> listForPid(long pid) {
        return jdbc.query("SELECT * FROM heap_dump WHERE pid = ? ORDER BY created_millis DESC", MAPPER, pid);
    }

    public Optional<HeapDumpInfo> findById(long id) {
        return jdbc.query("SELECT * FROM heap_dump WHERE id = ?", MAPPER, id).stream().findFirst();
    }
}
