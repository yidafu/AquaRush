-- Create reviews table
CREATE TABLE IF NOT EXISTS reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    delivery_worker_id UUID NOT NULL,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    is_anonymous BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create delivery_worker_statistics table
CREATE TABLE IF NOT EXISTS delivery_worker_statistics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    delivery_worker_id UUID NOT NULL UNIQUE,
    average_rating DECIMAL(3,2) NOT NULL DEFAULT 0.00,
    total_reviews INTEGER NOT NULL DEFAULT 0,
    one_star_reviews INTEGER NOT NULL DEFAULT 0,
    two_star_reviews INTEGER NOT NULL DEFAULT 0,
    three_star_reviews INTEGER NOT NULL DEFAULT 0,
    four_star_reviews INTEGER NOT NULL DEFAULT 0,
    five_star_reviews INTEGER NOT NULL DEFAULT 0,
    last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for reviews table
CREATE INDEX IF NOT EXISTS idx_reviews_order_id ON reviews(order_id);
CREATE INDEX IF NOT EXISTS idx_reviews_user_id ON reviews(user_id);
CREATE INDEX IF NOT EXISTS idx_reviews_delivery_worker_id ON reviews(delivery_worker_id);
CREATE INDEX IF NOT EXISTS idx_reviews_created_at ON reviews(created_at);
CREATE INDEX IF NOT EXISTS idx_reviews_rating ON reviews(rating);
CREATE INDEX IF NOT EXISTS idx_reviews_composite ON reviews(delivery_worker_id, created_at);

-- Create index for delivery_worker_statistics table
CREATE INDEX IF NOT EXISTS idx_delivery_worker_statistics_id ON delivery_worker_statistics(delivery_worker_id);

-- Add constraints for reviews table
ALTER TABLE reviews ADD CONSTRAINT chk_reviews_rating_range CHECK (rating >= 1 AND rating <= 5);
ALTER TABLE reviews ADD CONSTRAINT chk_reviews_comment_length CHECK (LENGTH(COALESCE(comment, '')) <= 500);

-- Add constraints for delivery_worker_statistics table
ALTER TABLE delivery_worker_statistics ADD CONSTRAINT chk_average_rating_range CHECK (average_rating >= 0.00 AND average_rating <= 5.00);
ALTER TABLE delivery_worker_statistics ADD CONSTRAINT chk_total_reviews_non_negative CHECK (total_reviews >= 0);
ALTER TABLE delivery_worker_statistics ADD CONSTRAINT chk_star_reviews_non_negative CHECK (
    one_star_reviews >= 0 AND
    two_star_reviews >= 0 AND
    three_star_reviews >= 0 AND
    four_star_reviews >= 0 AND
    five_star_reviews >= 0
);

-- Add foreign key constraints (assuming these tables exist)
ALTER TABLE reviews ADD CONSTRAINT fk_reviews_order_id
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE;
ALTER TABLE reviews ADD CONSTRAINT fk_reviews_user_id
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE reviews ADD CONSTRAINT fk_reviews_delivery_worker_id
    FOREIGN KEY (delivery_worker_id) REFERENCES delivery_workers(id) ON DELETE CASCADE;

-- Create trigger to automatically update updated_at timestamp for reviews
CREATE OR REPLACE FUNCTION update_reviews_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER trigger_reviews_updated_at
BEFORE UPDATE ON reviews
FOR EACH ROW
EXECUTE FUNCTION update_reviews_updated_at();

-- Create trigger to automatically update last_updated timestamp for delivery_worker_statistics
CREATE OR REPLACE FUNCTION update_delivery_worker_statistics_last_updated()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_updated = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER trigger_delivery_worker_statistics_last_updated
BEFORE UPDATE ON delivery_worker_statistics
FOR EACH ROW
EXECUTE FUNCTION update_delivery_worker_statistics_last_updated();

-- Add comments
COMMENT ON TABLE reviews IS 'User reviews for delivery workers';
COMMENT ON COLUMN reviews.order_id IS 'Associated order ID (unique constraint ensures one review per order)';
COMMENT ON COLUMN reviews.rating IS '1-5 star rating for delivery worker performance';
COMMENT ON COLUMN reviews.is_anonymous IS 'Whether the review is anonymous';
COMMENT ON TABLE delivery_worker_statistics IS 'Aggregated statistics for delivery worker ratings';
COMMENT ON COLUMN delivery_worker_statistics.average_rating IS 'Average rating across all reviews (0.00-5.00)';
COMMENT ON COLUMN delivery_worker_statistics.total_reviews IS 'Total number of reviews received';