-- Simplified loyalty program migration - only adds loyalty_tier column to existing users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS loyalty_tier VARCHAR(50) NOT NULL DEFAULT 'NONE';

