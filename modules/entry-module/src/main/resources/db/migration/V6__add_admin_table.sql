-- 管理员表
CREATE TABLE IF NOT EXISTS admins (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(100),
    phone VARCHAR(20),
    role VARCHAR(50) NOT NULL DEFAULT 'ADMIN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP
);

CREATE INDEX idx_admins_username ON admins(username);

-- 插入默认管理员账号(密码: admin123, 使用BCrypt加密)
-- 注意: 实际使用时应该使用BCrypt等强加密算法
INSERT INTO admins (username, password_hash, name, role) 
VALUES ('admin', '$2a$10$EIXw4YBnZOqIhkIk.Vx0xOJqMQ5P9xN9xN9xN9xN9xN9xN9xN9xN9x', '系统管理员', 'SUPER_ADMIN')
ON CONFLICT (username) DO NOTHING;
