import BaseConfig from './baseConfig';

const dataLabels = [
  {
    name: '颜色',
    inputType: 'ColorSelect',
    fatherEvent: 'dataLabels',
    event: 'color',
    defaultValue: 'rgba(89,89,89,1)'
  },
  {
    name: '位置',
    inputType: 'Select',
    fatherEvent: 'dataLabels',
    event: 'position',
    defaultValue: 6,
    options: [
      { value: 2, label: '中间' },
      { value: 4, label: '内部' },
      { value: 6, label: '外部' }
    ]
  },
  {
    name: '显示值',
    inputType: 'CheckBox',
    fatherEvent: 'dataLabels',
    event: 'showValue'
  },
  {
    name: '显示名称',
    inputType: 'CheckBox',
    fatherEvent: 'dataLabels',
    event: 'showSeriesName',
    funcName: 'SeriesNameVisible'
  },
  {
    name: '显示类型',
    inputType: 'CheckBox',
    fatherEvent: 'dataLabels',
    event: 'showCategoryName',
    funcName: 'CategoryNameVisible'
  }
];

const configClassify = [
  { title: '标题', configs: BaseConfig.title },
  { title: '图表区域', configs: BaseConfig.chartArea },
  { title: '图例', configs: BaseConfig.legend },
  { title: '数据列', configs: BaseConfig.series },
  { title: '数据标签', configs: dataLabels },
  { title: '坐标轴', configs: BaseConfig.axes }
];

export default configClassify;
