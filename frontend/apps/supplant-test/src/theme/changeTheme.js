import { DEFAULTFONTSIZE, DEFAULTCOLOR } from './constant.js';

const getLocalstorage = (name) => {
  let res = {};
  try {
    res = window.localStorage.getItem('personalTheme');
    res = res ? JSON.parse(res) : {};
  } catch (error) {
    // eslint-disable-next-line no-console
    console.log(error);
    res = {};
  }
  return (res || {})[name] || 'default';
};

const appendTheme = () => {
  const url = 'theme/sup';
  const theme = getLocalstorage('theme');
  const fontType = getLocalstorage('font');
  const link = document.createElement('link');
  const ptheme = DEFAULTCOLOR.map((item) => item.key).includes(theme)
    ? theme
    : 'default';
  link.type = 'text/css';
  link.id = `theme-${ptheme}`;
  link.rel = 'stylesheet';
  link.href = `${url}-${ptheme}.css?${process.env.PAKAGETIME || ''}`;
  document.documentElement.style.fontSize = `${
    DEFAULTFONTSIZE[fontType] || fontType
  }px`;
  document.getElementsByTagName('head')[0].appendChild(link);
};

export default appendTheme;
