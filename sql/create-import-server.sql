CREATE TABLE import_history (
  import_date DATE NOT NULL,
  PRIMARY KEY(import_date));

CREATE TABLE record_map (
  my_record_rn INTEGER NOT NULL,
  imported_record_rn INTEGER NOT NULL,
  last_sync_date DATE NOT NULL,
  PRIMARY KEY (my_record_rn));