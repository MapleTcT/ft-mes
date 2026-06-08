import { request as baseRequest } from 'sup-rc-utility';

function authHeaders(headers = {}) {
  const ticket = localStorage.getItem('ticket');
  return {
    ...headers,
    ...(ticket ? { Authorization: `Bearer ${ticket}` } : {})
  };
}

export function request(config = {}) {
  return baseRequest({
    ...config,
    headers: authHeaders(config.headers)
  });
}

request.get = (url, config = {}) =>
  request({
    ...config,
    method: 'GET',
    url
  });

request.post = (url, data, config = {}) =>
  request({
    ...config,
    method: 'POST',
    url,
    data
  });

request.put = (url, data, config = {}) =>
  request({
    ...config,
    method: 'PUT',
    url,
    data
  });

request.delete = (url, config = {}) =>
  request({
    ...config,
    method: 'DELETE',
    url
  });

export default request;
