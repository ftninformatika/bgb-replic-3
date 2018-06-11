CREATE TABLE export_history (
  export_date DATE NOT NULL,
  export_type CHAR(1) UNICODE NOT NULL,
  PRIMARY KEY(export_date));