-- Branch badge logos. Hibernate ddl-auto=update also creates this table and cars.serviced_branch_ids.
-- Not run on boot.
-- Redis: badge bytes are not cached. cars.serviced_branch_ids is inside carlist:{owner}:* —
-- the visit/history writers evict that key. Do not PUT a patched carlist.

CREATE TABLE IF NOT EXISTS branch_badge_logos (
    image_id   bigserial PRIMARY KEY,
    branch_id  bigint NOT NULL,
    file_name  varchar(255),
    file_type  varchar(255),
    image_data bytea
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_branch_badge_logos_branch_id ON branch_badge_logos (branch_id);
