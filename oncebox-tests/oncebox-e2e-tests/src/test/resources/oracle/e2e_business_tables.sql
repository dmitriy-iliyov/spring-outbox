CREATE TABLE e2e_produced_events (
    verify_id VARCHAR2(36) PRIMARY KEY
);

-- No primary key on purpose: a duplicated business effect must be observable as a second row
CREATE TABLE e2e_consumed_events (
    verify_id VARCHAR2(36) NOT NULL
);
