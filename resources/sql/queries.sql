-- :name save-domain! :! :n
-- :doc saves a new domain
INSERT INTO domains
(name)
VALUES(:name)

-- :name get-domains :? :*
-- :doc selects all available domains
-- SELECT * from domains ORDER BY name ASC;
select d.id, d.name, count(r.domain_id) as count from domains d left join records r on d.id=r.domain_id group by d.id, d.name order by d.name ASC;

-- :name get-domain-id :? :1
-- :doc select id from domains where name
select d.id from domains d where d.name = :name


-- :name save-record! :! :n
-- :doc saves a new record
INSERT INTO records
(domain_id, exchange_domain, seller_account_id, account_type, tag_id, comment)
VALUES(:domain_id, :exchange_domain, :seller_account_id, :account_type, :tag_id, :comment)

-- :name get-records :? :*
-- :doc selects all available records
select d.name, r.* from domains d, records r where d.id=r.domain_id order by d.name;


-- :name get-records-for-domain :? :*
-- :doc selects all available records for a specific domain
select d.name, r.* from domains d, records r where d.id=r.domain_id and d.id = :id order by d.name;


-- :name get-domains-count :? :1
-- :doc returns number of domains
SELECT COUNT(*) FROM domains

-- :name get-records-count :? :1
-- :doc returns number of records
SELECT COUNT(*) FROM records

-- :name get-domain-name :? :1
-- :doc returns the domain name for it's id
select d.name from domains d where d.id = :id
