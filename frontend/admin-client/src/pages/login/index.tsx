import React, { useState } from 'react';
import { Form, Input, Button, Card, Checkbox, message, Spin } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import './index.css';

interface LoginForm {
  username: string;
  password: string;
  remember?: boolean;
}

const Login: React.FC = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [form] = Form.useForm();

  // 如果已登录，直接跳转到首页
  React.useEffect(() => {
    const token = localStorage.getItem('token');
    if (token) {
      navigate('/dashboard', { replace: true });
    }
  }, [navigate]);

  // 页面加载时从 localStorage 读取记住的用户名
  React.useEffect(() => {
    const rememberedUsername = localStorage.getItem('rememberedUsername');
    if (rememberedUsername) {
      form.setFieldsValue({ 
        username: rememberedUsername,
        remember: true 
      });
    }
  }, [form]);

  const onFinish = async (values: LoginForm) => {
    setLoading(true);
    
    try {
      console.log('登录信息:', values);
      
      // TODO: 调用登录 API
      // const response = await login(values.username, values.password);
      // localStorage.setItem('token', response.token);
      // localStorage.setItem('userInfo', JSON.stringify(response.userInfo));
      
      // 模拟 API 调用延迟
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      // 处理"记住我"功能
      if (values.remember) {
        localStorage.setItem('rememberedUsername', values.username);
      } else {
        localStorage.removeItem('rememberedUsername');
      }
      
      // 临时存储 token（实际应该从 API 返回）
      localStorage.setItem('token', 'mock-token-' + Date.now());
      
      message.success('登录成功！');
      navigate('/dashboard');
    } catch (error) {
      message.error('登录失败，请检查用户名和密码');
      console.error('登录错误:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <div className="login-container">
        <div className="login-header">
          <div className="logo">
            <img src="/aqua-rush.jpeg" alt="AquaRush" className="logo-img" />
          </div>
          <h1 className="title">桶装水订水系统</h1>
          <p className="subtitle">管理后台</p>
        </div>

        <Card className="login-card" bordered={false}>
          <Spin spinning={loading}>
            <Form
              form={form}
              name="login"
              onFinish={onFinish}
              size="large"
              autoComplete="off"
            >
              <Form.Item
                name="username"
                rules={[
                  { required: true, message: '请输入用户名' },
                  { min: 3, message: '用户名至少3个字符' },
                ]}
              >
                <Input 
                  prefix={<UserOutlined className="input-icon" />} 
                  placeholder="请输入用户名"
                  autoComplete="username"
                />
              </Form.Item>

              <Form.Item
                name="password"
                rules={[
                  { required: true, message: '请输入密码' },
                  { min: 6, message: '密码至少6个字符' },
                ]}
              >
                <Input.Password 
                  prefix={<LockOutlined className="input-icon" />} 
                  placeholder="请输入密码"
                  autoComplete="current-password"
                />
              </Form.Item>

              <Form.Item>
                <Form.Item name="remember" valuePropName="checked" noStyle>
                  <Checkbox>记住用户名</Checkbox>
                </Form.Item>
              </Form.Item>

              <Form.Item>
                <Button 
                  type="primary" 
                  htmlType="submit" 
                  block 
                  loading={loading}
                  size="large"
                >
                  登录
                </Button>
              </Form.Item>
            </Form>
          </Spin>

          <div className="login-footer">
            <p className="tips">默认账号：admin / 123456</p>
          </div>
        </Card>
      </div>
    </div>
  );
};

export default Login;
