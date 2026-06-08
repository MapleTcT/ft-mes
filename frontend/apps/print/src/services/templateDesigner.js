import { request } from 'sup-rc-utility';
import { getSource } from '../utils';

// 查询设计模板
export function queryTemplateDesign(templateId) {
  return request({
    url: `/inter-api/printer/v1/${templateId}/templateDesign`,
    method: 'GET'
  });
}

// 设计模板那保存
export function saveTemplateDesign(templateId, templateContent, isEnable) {
  const obj = isEnable ? { enabled: 1 } : {};
  return request({
    url: '/inter-api/printer/v1/templateDesign',
    method: 'POST',
    data: {
      content: templateContent,
      templateId,
      ...obj
    }
  });
}

/**
 * 请求自定义服务数据
 * @param {*} url
 * @param {*} isRuntim  1. 设计期， 2. 运行期
 */
export function queryCustomService(url, isRuntim = false) {
  const process = isRuntim ? 2 : 1;
  return request({
    url: ` /inter-api/printer/v1/service?url=${url}&process=${process}`,
    method: 'GET'
  });
}

export function queryEntityObjects(code, formId, list, pageId = '') {
  const source = getSource();
  return request({
    url: '/inter-api/printer/v1/getEntityData',
    method: 'POST',
    data: {
      source,
      appCode: code,
      condition: {
        id: formId,
        viewCode: pageId,
        params: ''
      },
      resultData: list
    }
  });
}

/**
 * 根据实体code查询模型列表
 * @param {*} entityCode
 */
export function queryEntityModelList(entityCode) {
  return request({
    url: `/inter-api/printer/v1/entity/models?entityCode=${entityCode}`,
    method: 'GET'
  });
}

/**
 * 根据模型实体请求属性列表
 * @param {*} modelCode
 * @param {*} propertyCode
 */
export function queryModelPropertys(modelCode, propertyCode) {
  return request({
    url: `/inter-api/printer/v1/model/properties?modelCode=${modelCode}&propertyCode=${propertyCode || ''}`,
    method: 'GET'
  });
}
