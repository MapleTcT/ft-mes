import axios from './request';

function stringify(params) {
  const items = [];
  for (const i in params) {
    if (
      Object.prototype.hasOwnProperty.call(params, i) &&
      (typeof params[i] === 'boolean' || params[i])
    ) {
      if (typeof params[i] === 'object') {
        if (params[i].length > 0) {
          items.push(
            `${i}=${params[i].map((item) => encodeURIComponent(item))}`
          );
        }
      } else {
        items.push(`${i}=${encodeURIComponent(params[i])}`);
      }
    }
  }
  return items.join('&');
}

export function getAuthorityList(params) {
  return axios.get(
    `/inter-api/supplant-license/v1/getLicensePage?${stringify(params)}`
  );
}

export function getLicenseByModule(code) {
  return axios.get(
    `/inter-api/supplant-license/v1/getLicenseByModule?moduleCode=${code}`
  );
}
