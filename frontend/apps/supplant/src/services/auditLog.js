import request from './request';

const API_PREFIX = '/inter-api/auditlog/v1';

const blobParams = {
  responseType: 'blob',
  timeout: 0
};

// 导出审计日志
export const exportDataLog = (data) => {
  return request({
    url: `${API_PREFIX}/data-log/export`,
    method: 'POST',
    data,
    ...blobParams
  });
};

// 查询数据日志模型列表
export const fetchDataLog = (traceId, data) => {
  return request({
    url: `${API_PREFIX}/data-log/${traceId}`,
    method: 'POST',
    data
  });
};

// 查询导入数据日志模型列表
export const fetchImportDataLog = (traceId, params, data) => {
  return request({
    url: `${API_PREFIX}/data-log/${traceId}/import`,
    method: 'POST',
    params,
    data
  });
};

// 下载日志内文件
export const downloadDataLog = (traceId, params) => {
  return request({
    url: `${API_PREFIX}/data-log/${traceId}/file`,
    method: 'GET',
    params,
    ...blobParams
  });
};

// 导出审计模型日志
export const exportDataLogModel = (traceId, params) => {
  return request({
    url: `${API_PREFIX}/data-log/${traceId}/model/export`,
    method: 'GET',
    params
  });
};

// 查询审计日志模型属性列表
export const fetchtDataLogModel = (traceId, modelCode, params) => {
  return request({
    url: `${API_PREFIX}/data-log/${traceId}/model/${modelCode}`,
    method: 'GET',
    params
  });
};

// 查询审计日志模型属性列表(导入)
export const fetchtDataLogModelWithCode = (
  traceId,
  modelCode,
  code,
  params
) => {
  return request({
    url: `${API_PREFIX}/data-log/${traceId}/model/${modelCode}/code/${code}`,
    method: 'GET',
    params
  });
};

// 查询数据日志列表
export const fetchDataLogs = (params, data) => {
  return request({
    url: `${API_PREFIX}/data-logs`,
    params,
    method: 'POST',
    data
  });
};
