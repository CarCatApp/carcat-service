-- Feedback attachment images (optional file part on POST /api/v1/feedback/push).
-- Hibernate ddl-auto=update also creates this; script is for ops / empty environments.

CREATE TABLE IF NOT EXISTS feedback_photos (
    image_id    bigserial PRIMARY KEY,
    feedback_id bigint NOT NULL,
    file_name   varchar(255),
    file_type   varchar(64),
    image_data  bytea
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_feedback_photos_feedback_id ON feedback_photos (feedback_id);
