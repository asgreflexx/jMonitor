-- Registry of captured heap dumps (Phase 4).
CREATE TABLE IF NOT EXISTS heap_dump (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    pid           BIGINT       NOT NULL,
    file_name     VARCHAR(255) NOT NULL,
    size_bytes    BIGINT       NOT NULL,
    created_millis BIGINT      NOT NULL,
    live          BOOLEAN      NOT NULL
);

-- Registry of saved JFR recordings (Phase 5).
CREATE TABLE IF NOT EXISTS jfr_recording (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    pid           BIGINT       NOT NULL,
    file_name     VARCHAR(255) NOT NULL,
    size_bytes    BIGINT       NOT NULL,
    created_millis BIGINT      NOT NULL,
    profile       VARCHAR(64)  NOT NULL
);
