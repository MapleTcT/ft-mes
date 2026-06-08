import zhCN from 'root/locale/zh-cn.json';
import en from 'root/locale/en.json';

import zhCNSup from 'sup-ui/lib/locale-provider/zh_CN';
import enUS from 'sup-ui/lib/locale-provider/en_US';

const local = [
  {
    locale: 'zh-cn',
    messages: zhCN
  },
  {
    locale: 'en',
    messages: en
  }
];

const componentlocal = {
  'zh-cn': zhCNSup,
  en: enUS
};

export const language = local.filter((e) => e.locale === (window.localStorage.getItem('locale') || 'zh-cn'))[0];

export const componentLanguage = componentlocal[window.localStorage.getItem('locale') || 'zh-cn'];
