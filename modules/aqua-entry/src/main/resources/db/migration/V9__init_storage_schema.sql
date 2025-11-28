-- Create file_metadata table
CREATE TABLE IF NOT EXISTS file_metadata (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    storage_path VARCHAR(500) NOT NULL UNIQUE,
    file_type VARCHAR(20) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    checksum VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_public BOOLEAN NOT NULL DEFAULT TRUE,
    description VARCHAR(1000),
    extension VARCHAR(10),
    owner_id BIGINT
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_file_metadata_checksum ON file_metadata(checksum);
CREATE INDEX IF NOT EXISTS idx_file_metadata_storage_path ON file_metadata(storage_path);
CREATE INDEX IF NOT EXISTS idx_file_metadata_file_type ON file_metadata(file_type);
CREATE INDEX IF NOT EXISTS idx_file_metadata_owner_id ON file_metadata(owner_id);
CREATE INDEX IF NOT EXISTS idx_file_metadata_created_at ON file_metadata(created_at);
CREATE INDEX IF NOT EXISTS idx_file_metadata_is_public ON file_metadata(is_public);

-- Add constraint to ensure file_type is valid
ALTER TABLE file_metadata ADD CONSTRAINT chk_file_type
CHECK (file_type IN ('IMAGE', 'VIDEO', 'AUDIO', 'DOCUMENT', 'SPREADSHEET', 'PRESENTATION', 'FRONTEND', 'ARCHIVE', 'EXECUTABLE', 'BACKUP', 'OTHER'));

-- Add constraint to ensure file_size is positive
ALTER TABLE file_metadata ADD CONSTRAINT chk_file_size_positive
CHECK (file_size > 0);

-- Create trigger to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_file_metadata_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER trigger_file_metadata_updated_at
BEFORE UPDATE ON file_metadata
FOR EACH ROW
EXECUTE FUNCTION update_file_metadata_updated_at();