ALTER TABLE user_accounts
    ADD COLUMN auth_version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE user_accounts
    ADD CONSTRAINT ck_user_accounts_auth_version_nonnegative CHECK (auth_version >= 0);
