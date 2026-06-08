// 默认色值配置
const DEFAULTCOLOR = [
  {
    key: 'default',
    color: '#0F71E2',
    menuColor: '#345FB0',
    common: {}
  },
  {
    key: 'dark',
    color: '#19AD87',
    menuColor: '#26292B',
    common: {
      '@supTable-btn-color': 'linear-gradient(0deg, #ECF8F5 0%, #FFFFFF 100%)',
      '@supTable-btn-color-filled':
        'linear-gradient(0deg, #19AD87 0%, #1CC197 100%)'
    }
  }
];

// 默认文字大小配置
const DEFAULTFONTSIZE = {
  default: 12,
  small: 10,
  large: 14
};

module.exports = {
  DEFAULTCOLOR,
  DEFAULTFONTSIZE
};
