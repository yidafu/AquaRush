import React, { useState } from 'react';
import { Form, Input, Button, Card, Checkbox, message, Spin } from '@aquarush/admin-ui';
import { UserOutlined, LockOutlined } from '@aquarush/admin-ui/icons';
import { useNavigate } from 'react-router-dom';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';
import { login } from '../../services/auth';
import { userActionTracker } from '../../utils/tracking';

// 登录页(原 index.css 迁 StyleX;品牌渐变/圆角/阴影取自共享 token)
// 说明:logo 徽章投影 0 4px 12px rgba(0,0,0,.15) 与副标题 rgba(255,255,255,.9)
// 为本页局部色值,未入共享 token。
const styles = stylex.create({
  page: {
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    minHeight: '100vh',
    backgroundImage: tokens.brandGradient,
    paddingTop: {
      default: '20px',
      '@media (max-width: 576px)': '12px',
    },
    paddingRight: {
      default: '20px',
      '@media (max-width: 576px)': '12px',
    },
    paddingBottom: {
      default: '20px',
      '@media (max-width: 576px)': '12px',
    },
    paddingLeft: {
      default: '20px',
      '@media (max-width: 576px)': '12px',
    },
  },
  container: {
    width: '100%',
    maxWidth: '400px',
  },
  header: {
    textAlign: 'center',
    marginBottom: '40px',
    color: tokens.onBrand,
  },
  logo: {
    marginBottom: '20px',
  },
  logoImg: {
    width: '80px',
    height: '80px',
    borderRadius: '50%',
    boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)',
    backgroundColor: tokens.card,
    paddingTop: '12px',
    paddingRight: '12px',
    paddingBottom: '12px',
    paddingLeft: '12px',
  },
  title: {
    fontSize: {
      default: '28px',
      '@media (max-width: 576px)': '24px',
    },
    fontWeight: 600,
    marginTop: '0',
    marginRight: '0',
    marginBottom: '8px',
    marginLeft: '0',
    color: tokens.onBrand,
    textShadow: '0 2px 4px rgba(0, 0, 0, 0.1)',
  },
  subtitle: {
    fontSize: '16px',
    marginTop: '0',
    marginRight: '0',
    marginBottom: '0',
    marginLeft: '0',
    color: 'rgba(255, 255, 255, 0.9)',
  },
  card: {
    boxShadow: tokens.shModal,
    borderRadius: tokens.rCard,
    overflow: 'hidden',
  },
  cardBody: {
    paddingTop: {
      default: '32px',
      '@media (max-width: 576px)': '24px',
    },
    paddingRight: {
      default: '32px',
      '@media (max-width: 576px)': '24px',
    },
    paddingBottom: {
      default: '32px',
      '@media (max-width: 576px)': '24px',
    },
    paddingLeft: {
      default: '32px',
      '@media (max-width: 576px)': '24px',
    },
  },
  inputIcon: {
    color: 'rgba(0, 0, 0, 0.25)',
  },
  footer: {
    marginTop: '16px',
    textAlign: 'center',
  },
  tips: {
    marginTop: '0',
    marginRight: '0',
    marginBottom: '0',
    marginLeft: '0',
    color: 'rgba(0, 0, 0, 0.45)',
    fontSize: '13px',
  },
});

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

      // 调用登录 API
      const response = await login({
        username: values.username,
        password: values.password,
      });

      // 保存 token 和用户信息到 localStorage
      localStorage.setItem('token', response.accessToken);
      localStorage.setItem('userInfo', JSON.stringify(response.userInfo));

      // 处理"记住我"功能
      if (values.remember) {
        localStorage.setItem('rememberedUsername', values.username);
      } else {
        localStorage.removeItem('rememberedUsername');
      }

      message.success('登录成功！');
      // 埋点:登录成功
      userActionTracker.track('CLICK', 'login-submit', {
        elementType: 'button',
        elementText: '登录',
        properties: { username: values.username },
      });
      navigate('/dashboard');
    } catch (error: any) {
      message.error(error.message || '登录失败，请检查用户名和密码');
      console.error('登录错误:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div {...stylex.props(styles.page)}>
      <div {...stylex.props(styles.container)}>
        <div {...stylex.props(styles.header)}>
          <div {...stylex.props(styles.logo)}>
            <img src="/aqua-rush-mark.svg" alt="奔泉" {...stylex.props(styles.logoImg)} />
          </div>
          <h1 {...stylex.props(styles.title)}>桶装水订水系统</h1>
          <p {...stylex.props(styles.subtitle)}>管理后台</p>
        </div>

        <Card
          {...stylex.props(styles.card)}
          styles={{ body: { padding: 0 } }}
          bordered={false}
        >
          <div {...stylex.props(styles.cardBody)}>
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
                    prefix={<UserOutlined {...stylex.props(styles.inputIcon)} />}
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
                    prefix={<LockOutlined {...stylex.props(styles.inputIcon)} />}
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

            <div {...stylex.props(styles.footer)}>
              <p {...stylex.props(styles.tips)}>默认账号：admin / 123456</p>
            </div>
          </div>
        </Card>
      </div>
    </div>
  );
};

export default Login;
