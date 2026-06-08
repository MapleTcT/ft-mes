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

// 修改密码
export function changePass(data) {
    return axios({
      url: '/inter-api/auth/v1/currentuser/password',
      data,
      method: 'PUT'
    });
  }

//   获取登录信息
export function getLoginInfo() {
    return axios({
        url: '/inter-api/auth/v1/getCurrentLoginInfo',
        method: 'GET'
    });
}

// 获取人员信息
export function getStaffInfo(params) {
    return axios.get(`/inter-api/organization/v1/person/modify/ref?${stringify(params)}`)
}

// 查图片
export function getImg(filePaths) {
  return axios({
    method: 'POST',
    responseType: 'json',
    url: '/inter-api/organization/v1/persons/downloadImage',
    params: {
      filePaths
    }
  });
}

