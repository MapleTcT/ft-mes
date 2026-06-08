import moment from 'moment';

export function downloadFile(data, headers, mime = 'application/octet-stream') {
  const [, file] = decodeURIComponent(headers['content-disposition']).split(
    'filename='
  );
  const fileName = file;
  if (window.navigator.msSaveOrOpenBlob) {
    // Internet Explorer
    window.navigator.msSaveOrOpenBlob(
      new Blob([data], { type: mime }),
      fileName
    );
  } else {
    const blob = new Blob([data], { type: mime });
    const blobURL = window.URL.createObjectURL(blob);
    const tempLink = document.createElement('a');
    tempLink.style.display = 'none';
    tempLink.href = blobURL;
    tempLink.setAttribute('download', fileName);

    document.body.appendChild(tempLink);
    tempLink.click();

    setTimeout(() => {
      document.body.removeChild(tempLink);
      window.URL.revokeObjectURL(blobURL);
    }, 200);
  }
}

export const formatSearchParams = (searchParams) => {
  const dataBody = {};
  const {
    pagination: { total, ...pagination },
    sorter,
    filters
  } = searchParams;
  // 处理排序逻辑
  let [sortKey] = Object.keys(sorter || {});
  if (sortKey) {
    if (sortKey === 'operateType.displayName') {
      sortKey = 'operateType';
    }
    dataBody.sortKey = sortKey;
    dataBody.desc = sorter[sortKey] === 'descend';
  }
  // 处理过滤条件
  const filtersKeys = Object.keys(filters).filter((d) => {
    return filters[d].length > 0;
  });
  if (filtersKeys.length) {
    filtersKeys.reduce((o, k) => {
      const multiParamKkeys = ['operateUserName', 'operateType.displayName'];

      const filterKeyMap = {
        operateUserName: 'userNames',
        'operateType.displayName': 'operateTypes'
      };

      const val = filters[k];

      if (multiParamKkeys.includes(k)) {
        o[filterKeyMap[k]] = val;
      } else if (k === 'operateTime') {
        // 处理操作时间
        const [operateStartTime, operateEndTime] = val;
        o.operateStartTime = formatDateToTImeString(operateStartTime);
        o.operateEndTime = formatDateToTImeString(operateEndTime);
      } else {
        const [firstParam] = val;
        o[k] = firstParam;
      }

      return o;
    }, dataBody);
  }

  return {
    queryParams: pagination,
    dataBody
  };
};

const formatDateToTImeString = (v) => {
  // 一天内:1
  // 三天内:2
  // 一周内:3
  // 一月内:4
  // 三月内:5
  // 半年内:6
  // 一年内:7
  let amount;
  let unit;
  switch (v) {
    case '1':
      amount = 1;
      unit = 'days';
      break;

    case '2':
      amount = 3;
      unit = 'days';
      break;

    case '3':
      amount = 7;
      unit = 'days';
      break;

    case '4':
      amount = 1;
      unit = 'months';
      break;

    case '5':
      amount = 3;
      unit = 'months';
      break;

    case '6':
      amount = 6;
      unit = 'months';
      break;

    case '7':
      amount = 12;
      unit = 'months';
      break;

    default:
      break;
  }
  let m;
  if (amount && unit) {
    m = moment().subtract(amount, unit)
  } else {
    m = moment(v);
  }
  return m.valueOf();
};
