-- :name save-domain! :! :n
-- :doc saves a new domain
INSERT INTO domains
(name, timestamp)
VALUES(:name, :timestamp)

-- :name get-domains :? :*
-- :dic selects all available domains
SELECT * from domains ORDER BY name ASC;
