import axios from './request';

function stringify(params) {
  const items = [];
  for (const i in params) {
    if (Object.prototype.hasOwnProperty.call(params, i) && params[i]) {
      if (typeof params[i] === 'object') {
        if (params[i].length > 0) {
          // params[i].map((item) => encodeURIComponent(item));
          // console.log(params[i])
          items.push(`${i}=${params[i].map((item) => encodeURIComponent(item))}`);
        }
      } else {
        items.push(`${i}=${encodeURIComponent(params[i])}`);
      }
    }
  }
  return items.join('&');
}

export function getMission(params) {
  return axios.get(`/inter-api/notification-admin/v1/notice/task/tasks?${stringify(params)}`);
}
export function getReceive(params) {
  return axios.get(`/inter-api/notification-admin/v1/notice/message/record/messages?${stringify(params)}`);
}
// 查询内容模板
export function getContent(params) {
  return axios.get(`/inter-api/notification-admin/v1/notice/template/templates?${stringify(params)}`);
}
// 非分页查取模板内容
export function getNopageContent() {
  // return axios.get('/content');
  return axios.get('/inter-api/notification-admin/v1/notice/template/nopagelist');
}
// 新增内容模板
export function addContent(params) {
  return axios.post('/inter-api/notification-admin/v1/notice/template/add', params);
}
// 编辑内容模板
export function editContent(params) {
  return axios.post('/inter-api/notification-admin/v1/notice/template/update', params);
}
// 删除内容模板
export function delContent(params) {
  return axios.delete(`/inter-api/notification-admin/v1/notice/template/delete?ids=${params.join(',')}`);
}
// 获取内容模板默认模板
export function getDefaulTemp(params) {
  return axios.get(`/inter-api/notification-admin/v1/notice/template/defulttmpl?noticeTypeId=${params}`);
}
// 通知方式列表查询
export function getNotice() {
  return axios.get('/inter-api/notification-admin/v1/notice/protocol/protocols');
}
// 基础模板查询
export function getBaseModel(params) {
  return axios.get(`/inter-api/notification-admin/v1/notice/protocol/basetempalte?protocolId=${params}`);
}
// 获取主题类型树
export function getThemeTree(params) {
  let url = '';
  if (!stringify(params)) {
    url = '/inter-api/notification-admin/v1/notice/topictree/topictrees';
  } else {
    url = `/inter-api/notification-admin/v1/notice/topictree/topictrees?${stringify(params)}`;
  }
  return axios.get(url);
}
// 添加主题类型树
export function addThemeTree(param) {
  return axios.post('/inter-api/notification-admin/v1/notice/topictree/add', param);
}
// 修改主题类型树
export function updateThemeTree(param) {
  return axios.post('/inter-api/notification-admin/v1/notice/topictree/update', param);
}
// 删除主题类型树
export function delThemeTree(param) {
  return axios.delete(`/inter-api/notification-admin/v1/notice/topictree/delete?ids=${param}`);
}
// 获取主题列表
export function getTheme(params) {
  return axios.get(`/inter-api/notification-admin/v1/notice/topic/topics?${stringify(params)}`);
}
// 删除主题
export function delTheme(params) {
  return axios.delete(`/inter-api/notification-admin/v1/notice/topic/delete?ids=${params.join(',')}`);
}
// 添加主题
export function addTheme(param) {
  return axios.post('/inter-api/notification-admin/v1/notice/topic/add', param);
}
// 修改主题
export function updateTheme(param) {
  return axios.post('/inter-api/notification-admin/v1/notice/topic/update', param);
}
// 主题映射模板关系
export function topictmplmap(id) {
  return axios.get(`/inter-api/notification-admin/v1/notice/template/topictmplmap?topicId=${id}`);
}
// 查询协议配置信息
export function getConfig(id) {
  return axios.get(`/inter-api/notification-admin/v1/notice/protocolconfig/protocolconfig?protocolId=${id}`);
}
// 站内信配置信息
export function saveStationLetter(params) {
  return axios.post('/inter-api/notification-admin/v1/notice/protocolconfig/stationletter', params);
}
// 邮箱配置信息
export function saveEmail(params) {
  return axios.post('/inter-api/notification-admin/v1/notice/protocolconfig/email', params);
}
// 钉钉配置信息
export function saveDingtalk(params) {
  return axios.post('/inter-api/notification-admin/v1/notice/protocolconfig/dingtalk', params);
}
// 获取记录详细内容信息
export function getDetail(params) {
  return axios.get(`/inter-api/notification-admin/v1/notice/message/record?${stringify(params)}`);
}
// 获取接受范围
export function getReceiveRange(id) {
  return axios.get(`/inter-api/notification-admin/v1/notice/receiverange/rangemap?topicId=${id}`);
}
// 测试邮箱正确性
export function testEmail(params) {
  return axios.get(`/inter-api/notification-admin/v1/notice/valid/emailconfig?${stringify(params)}`);
}
// 菜单树
export function getMenuTree() {
  return axios.get('/inter-api/rbac/v1/menus/currentUser');
}
// 获取关于配置的系统参数
export function getSysSetting(appCode, code) {
  return axios.get(`/inter-api/systemconfig/v1/config/catalog/${appCode}/${code}`);
}
// 存储自定义配置页面数据
export function saveSysSetting(params) {
  return axios.put('/inter-api/systemconfig/v1/config/catalog/value', params);
}
// 主题树联想
export function getConnenctTree(params) {
  return axios.get(`/inter-api/notification-admin/v1/notice/topictree/keyword?${stringify(params)}`);
}
// 添加系统默认模板
export function addAddSysModel(params) {
  return axios.post('/inter-api/notification-admin/v1/notice/protocol/template', params);
}
// 修改系统默认模板
export function updateSysModel(params) {
  return axios.put(`/inter-api/notification-admin/v1/notice/protocol/template/${params.templateId}`, params);
}
// 删除系统默认模板
export function deleteSysModel(params) {
  return axios.post('/inter-api/notification-admin/v1/notice/protocol/template/batch', params);
}
// 个人站内信查询
export function getPersonalStation(params) {
  return axios.get(`/inter-api/notification-admin/v1/notice/message/record/stattionletter?${stringify(params)}`);
}
// 全部已读
export function readAll(params) {
  return axios.put('/inter-api/notification-admin/v1/notice/message/record/stattionletter/all', params);
}
// 全部已读
export function readSome(params) {
  return axios.put('/inter-api/notification-admin/v1/notice/message/record/stattionletter', params);
}
// 按钮权限获取
export function getAuthority(code) {
  return axios.get(`/inter-api/rbac/v1/userPermission/findUserOperate?menuInfoCode=${code}`);
}
