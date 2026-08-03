-- Insert default user with ID = 1
-- This script creates a default user for initial system setup

INSERT INTO users (
    id,
    wechat_openid,
    wechat_unionid,
    nickname,
    phone,
    avatar_url,
    email,
    status,
    role,
    balance,
    total_spent,
    created_at,
    updated_at,
    last_login_at
) VALUES (
    1,
    'default_openid_001',
    'default_unionid_001',
    'Default User',
    '13800138000',
    'https://example.com/default-avatar.png',
    'default@example.com',
    'ACTIVE',
    'USER',
    0.00,
    0.00,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO UPDATE SET
    wechat_openid = EXCLUDED.wechat_openid,
    wechat_unionid = EXCLUDED.wechat_unionid,
    nickname = EXCLUDED.nickname,
    phone = EXCLUDED.phone,
    avatar_url = EXCLUDED.avatar_url,
    email = EXCLUDED.email,
    status = EXCLUDED.status,
    role = EXCLUDED.role,
    balance = EXCLUDED.balance,
    total_spent = EXCLUDED.total_spent,
    updated_at = CURRENT_TIMESTAMP,
    last_login_at = CURRENT_TIMESTAMP;

-- Also insert a default delivery address for this user
INSERT INTO delivery_addresses (
    user_id,
    receiver_name,
    receiver_phone,
    province,
    city,
    district,
    detail_address,
    is_default,
    created_at,
    updated_at
) VALUES (
    1,
    'Default User',
    '13800138000',
    '北京市',
    '北京市',
    '朝阳区',
    '三里屯街道123号',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT DO NOTHING;

-- Verify the user was created
SELECT * FROM users WHERE id = 1;
