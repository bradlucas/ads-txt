ALTER TABLE domains RENAME COLUMN timestamp TO createdate;
ALTER TABLE domains ADD COLUMN crawldate TIMESTAMPTZ NULL;
