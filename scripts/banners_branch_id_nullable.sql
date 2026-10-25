-- adds / info banner has no branch. Hibernate ddl-auto=update does not drop NOT NULL.
-- Not run on boot. Run once on the server.

ALTER TABLE banners ALTER COLUMN branch_id DROP NOT NULL;
