import request from './request';

export const fetchTree = (params) => {
  return request({
    url: '/inter-api/customProperty/tree',
    method: 'GET',
    params
  });
};

export const fetchModelManageList = (params) => {
  return request({
    url: '/inter-api/customProperty/modelManage/list',
    method: 'GET',
    params
  });
};

export const fetchViewManageList = (params) => {
  return request({
    url: '/inter-api/customProperty/viewManage/list',
    method: 'GET',
    params
  });
};

export const updateModelManageField = (data) => {
  return request({
    url: '/inter-api/customProperty/modelManage/save',
    method: 'POST',
    data
  });
};

export const updateViewManageField = (data) => {
  return request({
    url: '/inter-api/customProperty/viewManage/save',
    method: 'POST',
    data
  });
};

export const showOrHiddenViewField = (data) => {
  return request({
    url: '/inter-api/customProperty/viewManage/showOrHidden',
    method: 'POST',
    data
  });
};

export const getRelateModuleByCode = (params) => {
  return request({
    url: '/inter-api/customProperty/getRelateModuleByCode',
    params,
    method: 'GET'
  });
};

export const getPKProperty = (params) => {
  return request({
    url: '/inter-api/customProperty/getPKProperty',
    params,
    method: 'GET'
  });
};

export const getRefViews = (params) => {
  return request({
    url: '/inter-api/customProperty/viewManage/refViews',
    params,
    method: 'GET'
  });
};

export const getSystemcode = (params) => {
  return request({
    url: '/inter-api/customProperty/viewManage/systemCode',
    params,
    method: 'GET'
  });
};

export const batcheUpdateEnabledStatus = (data) => {
  return request({
    url: '/inter-api/customProperty/modelManage/batcheUpdateEnabledStatus',
    data,
    method: 'POST'
  });
};

// 视图字段排序
export const sortViewFields = (data) => {
  return request({
    url: '/inter-api/customProperty/viewManage/sort',
    data,
    method: 'POST'
  });
};

// 模型字段排序
export const sortModelFields = (data) => {
  return request({
    url: '/inter-api/customProperty/modelManage/sort',
    data,
    method: 'POST'
  });
};
