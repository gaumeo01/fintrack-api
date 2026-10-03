ALTER TABLE transactions ADD COLUMN import_key varchar(80);
ALTER TABLE transactions ADD CONSTRAINT uk_import_row UNIQUE (user_id, import_key);
