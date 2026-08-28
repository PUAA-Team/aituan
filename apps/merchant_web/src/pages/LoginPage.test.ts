import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, test, vi } from 'vitest';

import LoginPage from './LoginPage.vue';
import { submitMerchantApplication } from '../api';

vi.mock('../api', () => ({
  submitMerchantApplication: vi.fn(),
}));

describe('merchant LoginPage', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.mocked(submitMerchantApplication).mockReset();
  });

  test('渲染商家登录入口并回填已保存账号', () => {
    localStorage.setItem('aituan_merchant_account', 'demo_merchant');

    const wrapper = mount(LoginPage, {
      props: { loading: false, message: '请输入账号密码' },
    });

    expect(wrapper.text()).toContain('商家经营控制台');
    expect(wrapper.text()).toContain('商家登录');
    expect((wrapper.find('input[autocomplete="username"]').element as HTMLInputElement).value).toBe('demo_merchant');
    expect(wrapper.text()).toContain('请输入账号密码');
  });

  test('输入账号密码后提交 submit 事件', async () => {
    const wrapper = mount(LoginPage, {
      props: { loading: false, message: '' },
    });

    await wrapper.find('input[autocomplete="username"]').setValue('merchant_001');
    await wrapper.find('input[type="password"]').setValue('123456');
    await wrapper.find('form.login-card').trigger('submit');

    expect(wrapper.emitted('submit')).toEqual([
      [{ account: 'merchant_001', password: '123456' }],
    ]);
  });

  test('入驻申请成功后展示申请编号并清空表单', async () => {
    let submittedPayload: unknown;
    vi.mocked(submitMerchantApplication).mockImplementation(async (payload) => {
      submittedPayload = { ...payload };
      return { applicationNo: 'MA202608260001' } as never;
    });
    const wrapper = mount(LoginPage, {
      props: { loading: false, message: '' },
    });

    await wrapper.find('button.secondary-btn').trigger('click');
    const applicationInputs = wrapper.find('form.modal-form').findAll('input[required]');
    await applicationInputs[0].setValue('爱团测试商户');
    await applicationInputs[1].setValue('测试门店');
    await applicationInputs[2].setValue('张三');
    await applicationInputs[3].setValue('18800001111');
    await applicationInputs[4].setValue('北京市海淀区');
    await wrapper.find('form.modal-form').trigger('submit');

    expect(submittedPayload).toEqual({
      merchantName: '爱团测试商户',
      contactName: '张三',
      contactPhone: '18800001111',
      businessType: 'takeaway',
      storeName: '测试门店',
      address: '北京市海淀区',
    });
    expect(wrapper.text()).toContain('申请已提交，申请编号 MA202608260001');
  });

  test('入驻申请失败时展示错误提示', async () => {
    vi.mocked(submitMerchantApplication).mockRejectedValue(new Error('手机号格式不正确'));
    const wrapper = mount(LoginPage, {
      props: { loading: false, message: '' },
    });

    await wrapper.find('button.secondary-btn').trigger('click');
    await wrapper.find('form.modal-form').trigger('submit');

    expect(wrapper.text()).toContain('手机号格式不正确');
  });
});
