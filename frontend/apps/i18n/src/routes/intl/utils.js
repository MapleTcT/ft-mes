import { fetchModuleList, addIntl, createI18nKey } from 'root/services/intl';
import { request } from 'root/services/request';
import MockJS from 'mockjs';
import { XLSX_TYPE } from './constant';

const { Random } = MockJS;

export function clone(data) {
  let nextData = {};
  if (Array.isArray(data)) {
    nextData = [];
  }
  let value;
  // eslint-disable-next-line guard-for-in
  for (const key in data) {
    value = data[key];
    if (typeof value === 'object') {
      nextData[key] = clone(value);
    } else {
      nextData[key] = value;
    }
  }
  return nextData;
}

export function extractEditLan(lans) {
  return lans.map((d) => {
    // eslint-disable-next-line camelcase
    const { languCode, used } = d;
    return {
      langu_code: languCode,
      used
    };
  });
}

const delay = async () => {
  // eslint-disable-next-line compat/compat
  return new Promise((res) => {
    setTimeout(() => {
      res();
    }, 1100);
  });
};

async function mockCreate(moduleCode) {
  // await delay();
  const {
    data: {
      list: [i18nkey]
    }
    // eslint-disable-next-line no-await-in-loop
  } = await createI18nKey({ moduleCode });

  await addIntl({
    i18n_key: i18nkey,
    i18n_value: {
      en_US: Random.word(),
      zh_CN: Random.word(),
      zh_TW: Random.word()
    }
  });
}

export async function autoCreateI18Keys(nums) {
  // 获取模块列表
  const {
    data: { list: modules }
  } = await fetchModuleList();

  const arr = [];

  for (let i = 0; i < nums; i += 1) {
    const { moduleCode } = modules[i % modules.length];
    // 获取国际化key
    // eslint-disable-next-line no-await-in-loop
    // await mockCreate(moduleCode);
    arr.push(mockCreate(moduleCode));
  }
  // eslint-disable-next-line compat/compat
  await Promise.all(arr);
}

export function genFilter(filter) {
  const data = {};
  const keys = Object.keys(filter);
  for (let i = 0; i < keys.length; i += 1) {
    const col = keys[i];
    const val = filter[col];
    if (val && val.length) {
      if (col === 'i18nKey') {
        data.i18n_key = val.join(',');
      } else {
        if (!data.i18n_values) {
          data.i18n_values = {};
        }
        data.i18n_values[col.split('.')[1]] = val;
      }
    }
  }
  return data;
}

export function downloadFile(
  data,
  filename,
  mime = 'application/octet-stream'
) {
  if (window.navigator.msSaveOrOpenBlob) {
    // Internet Explorer
    window.navigator.msSaveOrOpenBlob(
      new Blob([data], { type: mime }),
      filename
    );
  } else {
    const blob = new Blob([data], { type: mime });
    const blobURL = window.URL.createObjectURL(blob);
    const tempLink = document.createElement('a');
    tempLink.style.display = 'none';
    tempLink.href = blobURL;
    tempLink.setAttribute('download', filename);

    document.body.appendChild(tempLink);
    tempLink.click();

    setTimeout(() => {
      document.body.removeChild(tempLink);
      window.URL.revokeObjectURL(blobURL);
    }, 200);
  }
}

export function parseBlobToJson(blob) {
  // eslint-disable-next-line compat/compat
  return new Promise((resolve) => {
    if (blob && blob.text) {
      blob.text().then((res) => {
        resolve(JSON.parse(res));
      });
    } else {
      resolve({});
    }
  });
}

export function trimI18NValues(data) {
  const ret = {};
  if (data) {
    // eslint-disable-next-line guard-for-in
    for (const key in data) {
      ret[key] = (data[key] || '').trim();
    }
  }
  return ret;
}

export function fetchDownloadFile(url, params = {}, fileName, onError) {
  request({
    method: 'GET',
    url,
    params,
    responseType: 'blob',
    timeout: 60e3
  })
    .then(({ headers, data }) => {
      if (!fileName) {
        const [, file] = decodeURIComponent(headers['content-disposition']).split('fileName=');
        fileName = file;
      }
      downloadFile(data, fileName, XLSX_TYPE);
    })
    .catch(onError);
}
