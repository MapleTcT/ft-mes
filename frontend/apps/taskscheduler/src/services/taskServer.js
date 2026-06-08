import axios from './request';

function stringify(params) {
  const items = [];
  for (const i in params) {
    if (Object.prototype.hasOwnProperty.call(params, i) && (typeof params[i] === 'boolean' || params[i])) {
      if (typeof params[i] === 'object') {
        if (params[i].length > 0) {
          items.push(`${i}=${params[i].map((item) => encodeURIComponent(item))}`);
        }
      } else {
        items.push(`${i}=${encodeURIComponent(params[i])}`);
      }
    }
  }
  return items.join('&');
}

const basePath = '/inter-api/task-scheduler/v1';

// 获取树形列表
// 192.168.91.54:30010/servicemanager/msModule/module/modules
export function taskTree(params) {
  return axios.get(
    // '/msService/servicemanager/msModule/module/modules'
    `${basePath}/job/queryModules?${stringify(params)}`
  );
}

// 获取任务列表
export function taskDetail(params) {
  return axios({
    url: `${basePath}/job/gets`,
    params,
    method: 'GET'
  });
}

// 新增任务
export function addJob(data) {
  return axios({
    url: `${basePath}/job/add`,
    method: 'POST',
    data
  });
}

// 修改任务
export function updateJop(data) {
  return axios({
    url: `${basePath}/job/update`,
    method: 'PUT',
    data
  });
}

// 修改触发器
export function updateTrigger(data) {
  return axios({
    url: `${basePath}/job/updateTrigger`,
    method: 'PUT',
    data
  });
}

// 开启任务
export function startTask(data) {
  return axios({
    url: `${basePath}/job/resume`,
    method: 'POST',
    data
  });
}

// 停止任务
export function stopTask(data) {
  return axios({
    url: `${basePath}/job/pause`,
    method: 'POST',
    data
  });
}

// 删除任务
export function removeTask(data) {
  return axios({
    url: `${basePath}/job/delete`,
    method: 'DELETE',
    data
  });
}

// 一键执行
export function immediateExcutionTask(data) {
  return axios({
    url: `${basePath}/job/immediateExcute`,
    method: 'POST',
    data
  });
}

// 日志查询
export function queryTaskLog(params) {
  return axios({
    url: `${basePath}/log/getJobLog`,
    params,
    method: 'GET'
  });
}
