import { mount } from '@vue/test-utils';
import { describe, expect, test } from 'vitest';

import LoginPage from './LoginPage.vue';

describe('admin LoginPage', () => {
  test('渲染管理员登录入口并展示提示消息', () => {
    const wrapper = mount(LoginPage, {
      props: { loading: false, message: '登录失败，请检查密码' },
    });

    expect(wrapper.text()).toContain('平台运营后台');
    expect(wrapper.text()).toContain('管理员登录');
    expect(wrapper.text()).toContain('商户治理');
    expect(wrapper.text()).toContain('登录失败，请检查密码');
  });

  test('输入账号密码后提交 submit 事件', async () => {
    const wrapper = mount(LoginPage, {
      props: { loading: false, message: '' },
    });

    await wrapper.find('input[autocomplete="username"]').setValue('demo_admin');
    await wrapper.find('input[type="password"]').setValue('123456');
    await wrapper.find('form.login-card').trigger('submit');

    expect(wrapper.emitted('submit')).toEqual([
      [{ account: 'demo_admin', password: '123456' }],
    ]);
  });

  test('loading 状态禁用登录按钮并显示登录中文案', () => {
    const wrapper = mount(LoginPage, {
      props: { loading: true, message: '' },
    });
    const button = wrapper.find('button.primary-btn');

    expect(button.attributes('disabled')).toBeDefined();
    expect(button.text()).toBe('登录中');
  });
});
