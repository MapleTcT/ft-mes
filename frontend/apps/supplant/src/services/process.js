import axios from './request';

export function getMyProcess(param) {
  return axios.post(
    '/msService/baseService/workflow/remind/myflow-info',
    param
  );
}

export function getOpenURL(param) {
  return axios.get(`/msService/baseService/myWorkflow/openURL${param}`);
}

export function getStartProcess() {
  return axios.get(`/msService/baseService/pending/grup/findMyProcesses`);
}
