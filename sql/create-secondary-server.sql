CREATE TABLE replication_history (
  replication_date DATE NOT NULL,
  replication_type CHAR(1) UNICODE NOT NULL,
  PRIMARY KEY(replication_date));