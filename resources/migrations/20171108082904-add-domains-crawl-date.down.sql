ALTER TABLE domains DROP COLUMN crawldate;
ALTER TABLE domains RENAME COLUMN createdate TO timestamp;
