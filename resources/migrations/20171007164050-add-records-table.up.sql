CREATE TABLE records
(domain_id integer REFERENCES domains,
 exchange_domain text,
 seller_account_id text,
 account_type text,
 tag_id text,
 comment text,
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 PRIMARY KEY (domain_id, exchange_domain, seller_account_id, tag_id)
);

CREATE OR REPLACE FUNCTION trigger_set_timestamp()  
RETURNS TRIGGER AS $$  
BEGIN  
  NEW.updated_at = NOW();
  RETURN NEW;
END;  
$$ LANGUAGE plpgsql;

CREATE TRIGGER set_timestamp  
BEFORE UPDATE ON records
FOR EACH ROW  
EXECUTE PROCEDURE trigger_set_timestamp();
