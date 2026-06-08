import { DEFAULTFONTSIZE } from './constant.js';

const getCookie = (name) => {
  name += '=';
  const decodedCookie = decodeURIComponent(document.cookie);
  const ca = decodedCookie.split(';');
  for (let i = 0; i < ca.length; i += 1) {
    let c = ca[i];
    while (c.charAt(0) === ' ') {
      c = c.substring(1);
    }
    if (c.indexOf(name) === 0) {
      return c.substring(name.length, c.length);
    }
  }
  return undefined;
};

const appendTheme = () => {
  const url = 'theme/sup';
  let theme = getCookie('theme');
  const fontType = getCookie('fontSize') || 'default';
  theme = theme || 'default';
  const link = document.createElement('link');
  link.type = 'text/css';
  link.id = `theme-${theme}`;
  link.rel = 'stylesheet';
  link.href = `${url}-${theme}.css?${new Date().getTime()}`;
  document.documentElement.style.fontSize = `${DEFAULTFONTSIZE[fontType]
    || fontType}px`;
  document.getElementsByTagName('head')[0].appendChild(link);
};

export default appendTheme;
