-- :name save-domain! :! :n
-- :doc saves a new domain
INSERT INTO domains
(name)
VALUES(:name)

-- :name save-record! :! :n
-- :doc saves a new record
INSERT INTO records
(domain_id, exchange_domain, seller_account_id, account_type, tag_id, comment)
VALUES(:domain_id, :exchange_domain, :seller_account_id, :account_type, :tag_id, :comment)


-- :name get-domains :? :*
-- :doc selects all available domains
-- SELECT * from domains ORDER BY name ASC;
select d.id, d.name, d.timestamp, count(r.domain_id) as count from domains d left join records r on d.id=r.domain_id group by d.id, d.name order by d.name ASC;

-- :name get-domain-id :? :1
-- :doc select id from domains where name
select d.id from domains d where d.name = :name

-- :name get-records-for-domain :? :*
-- :doc selects all available records for a specific domain by name
select d.name, r.* from domains d, records r where d.id=r.domain_id and d.name = :name order by d.name;

-- :name get-records :? :*
-- :doc selects all available records
select d.name, r.* from domains d, records r where d.id=r.domain_id order by d.name;

-- :name get-domain-name :? :1
-- :doc returns the domain name for it's id
select d.name from domains d where d.id = :id

-- :name get-domains-count :? :1
-- :doc returns number of domains
SELECT COUNT(*) FROM domains

-- :name get-records-count :? :1
-- :doc returns number of records
SELECT COUNT(*) FROM records






-- :name get-domain-by-id :? :1
-- :doc get domain by id
select d.* from domains d where d.id=:id

-- :name get-domain-by-name :? :1
-- :doc get domain by name
select d.* from domains d where d.name=:name

-- :name get-records-for-domain-id :? :*
-- :doc selects all available records for a specific domain by id
select d.name, r.* from domains d, records r where d.id=r.domain_id and d.id = :id order by d.name;

-- :name get-records-for-domain-name :? :*
-- :doc selects all available records for a specific domain by name
select d.name, r.* from domains d, records r where d.id=r.domain_id and d.name = :name order by d.name;


-- :name check :? :1
-- :doc check for record instance
select d.name, r.* from domains d, records r where d.id=r.domain_id and d.name=:domain and r.exchange_domain=:exchange-domain and r.seller_account_id=:seller-account-id and r.account_type=:account-type;


-- :name truncate-tables :!
truncate domains cascade;

-- :name reset-domains-index :!
alter sequence domains_id_seq restart with 1;



-- :name delete-domain-records :! :n
delete from records where domain_id = :id

