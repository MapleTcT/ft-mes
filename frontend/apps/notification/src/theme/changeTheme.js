import { DEFAULTFONTSIZE } from './constant.js';

const getLocalstorage = (name) => {
  let theme = window.localStorage.getItem('theme');
  try {
    theme = theme ? JSON.parse(theme) : {};
    return theme[name];
  } catch (error) {
    return {};
  }
};

const appendTheme = () => {
  const url = 'theme/sup';
  const theme = getLocalstorage('theme') || 'default';
  const fontType = getLocalstorage('font') || 'default';
  const link = document.createElement('link');
  link.type = 'text/css';
  link.id = `theme-${theme}`;
  link.rel = 'stylesheet';
  link.href = `${url}-${theme}.css`;
  document.documentElement.style.fontSize = `${DEFAULTFONTSIZE[fontType]
    || fontType}px`;
  document.getElementsByTagName('head')[0].appendChild(link);
};

export default appendTheme;
