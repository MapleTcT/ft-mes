// import { message } from 'sup-ui';
// import axios from 'axios';

// const instance = axios.create({
//   timeout: 1000 * 12
// });

// instance.interceptors.request.use((config) => {
//   if (config.method === 'get' || config.method === 'delete') {
//     config.data = true;
//   }
//   const ticket = localStorage.getItem('ticket');
//   config.headers.Authorization = `Bearer ${ticket}`;
//   config.headers['Content-Type'] = 'application/json';
//   return config;
// });

// // Axios.interceptors.request.use(

// instance.interceptors.response.use(
//   // eslint-disable-next-line compat/compat
//   (res) => (res.status === 200 ? Promise.resolve(res) : Promise.reject(res)),
//   (error) => {
//     const { response } = error;

//     errorHandler(response);
//     // eslint-disable-next-line compat/compat
//     return Promise.reject(response);
//   }
// );

// function errorHandler(response) {
//   // error logic

//   console.error(response);
//   const {
//     data: { message: errMsg }
//   } = response;
//   message.error(errMsg);
// }

export { request as default } from 'sup-rc-utility';
