// 显示格式
export const getFormatOptions = (type) => {
  switch (type) {
    case 'TEXT':
    case 'INTEGER':
      return [{ val: 'TEXT', i18n: 'TEXT' }];

    case 'DECIMAL':
      return [
        { val: 'TEXT', i18n: 'TEXT' },
        { val: 'PERCENT', i18n: 'PERCENT' },
        { val: 'THOUSAND', i18n: 'THOUSAND' },
        { val: 'TEN_THOUSAND', i18n: 'TEN_THOUSAND' }
      ];

    case 'DATETIME':
      return [
        { val: 'YMD_HMS', text: '2000-05-01 06:09:06' },
        { val: 'YMD_HM', text: '2000-05-01 06:09' },
        { val: 'YMD_H', text: '2000-05-01 06' },
        { val: 'YMD', text: '2000-05-01' },
        { val: 'YM', text: '2000-05' },
        { val: 'Y', text: '2000' }
      ];

    case 'SYSTEMCODE':
    case 'OBJECT':
      return [{ val: 'SELECTCOMP', i18n: 'SELECTCOMP' }];

    default:
      return [];
  }
};

// 显示类型
export const getFieldTypeOptions = (type) => {
  switch (type) {
    case 'TEXT':
      return [
        { val: 'TEXTFIELD', i18n: 'TEXTFIELD' },
        { val: 'TEXTAREA', i18n: 'TEXTAREA' }
      ];

    case 'INTEGER':
    case 'DECIMAL':
      return [{ val: 'TEXTFIELD', i18n: 'TEXTFIELD' }];

    case 'DATETIME':
      return [{ val: 'DATETIME', i18n: 'DATETIME' }];

    case 'SYSTEMCODE':
    case 'OBJECT':
      return [{ val: 'SELECTCOMP', i18n: 'SELECTCOMP' }];

    default:
      return [];
  }
};

export const getLanguage = () => localStorage.getItem('language') || 'zh-cn';

export const getI18nLanguage = () => {
  const language = getLanguage();
  let newLanguage = language;
  const lan = language.split('-');
  if (lan.length === 2) {
    newLanguage = `${lan[0]}_${lan[1].toUpperCase()}`;
  }
  return newLanguage;
};

export const getNextSortRowData = (sortData, dragIndex, hoverIndex) => {
  const nextSortData = [...sortData];
  const dragRow = nextSortData[dragIndex];
  // 先删除, 再移动
  nextSortData.splice(dragIndex, 1);
  nextSortData.splice(hoverIndex, 0, dragRow);
  return nextSortData;
};

export const isSingleKeyObj = (obj = {}) => Object.keys(obj).length === 1;
