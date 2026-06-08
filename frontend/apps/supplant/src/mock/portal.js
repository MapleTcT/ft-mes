/* eslint-disable */
import Mock from 'mockjs';

Mock.mock(/inter-api\/license\/v1\/getLicensePage\?.*/, () => {
  return {
    total: 3,
    list: [
      {
        id: 670680775524352,
        key: 670680775524352,
        moduleCode: 'notest',
        applicationName: '名1',
        description: 1,
        applicationType: 'xxx'
      },
      {
        id: 670739006554112,
        key: 670739006554112,
        moduleCode: '1323',
        applicationName: '3222',
        description: 2,
        applicationType: 'test'
      },
      {
        id: 671227507245056,
        key: 671227507245056,
        moduleCode: 'notest2',
        applicationName: '名2',
        description: 2,
        applicationType: 'xxx'
      }
    ]
  };
});

Mock.mock(/inter-api\/license\/v1\/getLicenseByModule\?.*/, () => {
  return {
    data: {
      id: '@guid',
      applicationName: '@ctitle',
      moduleCode: '@guid',
      description: '@ctitle',
      applicationType: '@name'
    }
  };
});

export const menuList = {
  list: [
    {
      id: 1,
      description: '管理员角色',
      name: '管理员角色',
      code: 'systemRole'
    },
    {
      id: 2,
      description: '公司管理员角色',
      name: '公司管理员角色',
      code: 'companySystemRole'
    },
    {
      id: 3,
      description: '普通用户角色',
      name: '普通用户角色',
      code: 'normalRole'
    }
  ]
};
