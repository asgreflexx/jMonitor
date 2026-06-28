package com.jmonitor.server.jfr;

import com.jmonitor.common.dto.JfrRecordingInfo;
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
 * H2-backed registry of saved JFR recordings (Phase 5).
 */
@Repository
public class JfrRegistry {

    private static final RowMapper<JfrRecordingInfo> MAPPER = (rs, n) -> new JfrRecordingInfo(
            rs.getLong("id"),
            rs.getLong("pid"),
            rs.getString("file_name"),
            rs.getLong("size_bytes"),
            rs.getLong("created_millis"),
            rs.getString("profile"));

    private final JdbcTemplate jdbc;

    public JfrRegistry(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public JfrRecordingInfo insert(long pid, String fileName, long sizeBytes, long createdMillis,
                                   String profile) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO jfr_recording (pid, file_name, size_bytes, created_millis, profile) "
                            + "VALUES (?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, pid);
            ps.setString(2, fileName);
            ps.setLong(3, sizeBytes);
            ps.setLong(4, createdMillis);
            ps.setString(5, profile);
            return ps;
        }, keys);
        long id = keys.getKey() == null ? -1 : keys.getKey().longValue();
        return new JfrRecordingInfo(id, pid, fileName, sizeBytes, createdMillis, profile);
    }

    public List<JfrRecordingInfo> listForPid(long pid) {
        return jdbc.query("SELECT * FROM jfr_recording WHERE pid = ? ORDER BY created_millis DESC",
                MAPPER, pid);
    }

    public Optional<JfrRecordingInfo> findById(long id) {
        return jdbc.query("SELECT * FROM jfr_recording WHERE id = ?", MAPPER, id).stream().findFirst();
    }
}
