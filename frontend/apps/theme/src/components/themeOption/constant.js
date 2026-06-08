import themeDefaultImg from '../../assets/img/theme-default.jpg';
import themeDarkImg from '../../assets/img/theme-dark.jpg';

const { DEFAULTCOLOR } = require('../../theme/constant.js');

export default {
  default: {
    title: '蓝色主题',
    intl: 'theme_title_default',
    menuColor: DEFAULTCOLOR[0].menuColor,
    img: themeDefaultImg
  },
  dark: {
    title: '深色主题',
    intl: 'theme_title_dark',
    menuColor: DEFAULTCOLOR[1].menuColor,
    img: themeDarkImg
  }
};
