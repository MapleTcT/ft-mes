// import axios from 'axios';

// const instance = axios.create({
//   timeout: 1000 * 12
// });

// instance.interceptors.request.use(
//   (config) => {
//     if (config.method === 'get' || config.method === 'delete') {
//       config.data = true;
//     }
//     const ticket = localStorage.getItem('ticket');
//     config.headers.Authorization = `Bearer ${ticket}`;
//     config.headers['Content-Type'] = 'application/json';
//     config.headers['Accept-Language'] = localStorage.getItem('language') === 'zh_CN' ? 'zh-cn' : 'en-us';
//     return config;
//   });

// instance.interceptors.response.use(
//   (res) => (res.status === 200 ? Promise.resolve(res.data) : Promise.reject(res)),
//   (error) => {
//     const { response } = error;
//     errorHandler(response.status);
//     return Promise.reject(response);
//   }
// );

// function errorHandler(status, other) {
//   // error logic

//   console.error(status, other);
// }
// export default instance;
import { request } from 'sup-rc-utility';

// request.interceptors.response.use(
//   (res) => (res.status === 200 ? Promise.resolve(res.data) : Promise.reject(res))
// );

request.setErrorHandler(() => {});
export default request;
