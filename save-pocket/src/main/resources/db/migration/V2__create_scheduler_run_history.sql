CREATE TABLE scheduler_run_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    executed_at DATETIME(6) NOT NULL,
    finished_at DATETIME(6) NOT NULL,
    scanned_count INT NOT NULL,
    success_count INT NOT NULL,
    skipped_count INT NOT NULL,
    failed_count INT NOT NULL,
    error_message VARCHAR(1000),
    created_at DATETIME(6) NOT NULL
);

CREATE INDEX idx_scheduler_run_history_job_executed ON scheduler_run_history (job_name, executed_at);
