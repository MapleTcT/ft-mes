import axios from './request';

export function getPendingList(param) {
  return axios.post(
    '/msService/baseService/myWorkflow/statistics/pending',
    param
  );
}

export function getPendingNotice() {
  return axios.get('/inter-api/flow-service/v1/task/pending/group/total');
}
