import commonMessage from '../commonMessages';
import BaseConfig from './baseConfig';
import messages from '../messages';

const dataLabels = [
  {
    name: messages.Color,
    inputType: 'ColorSelect',
    fatherEvent: 'dataLabels',
    event: 'color',
    defaultValue: 'rgba(89,89,89,1)'
  },
  {
    name: messages.position,
    inputType: 'Select',
    fatherEvent: 'dataLabels',
    event: 'position',
    defaultValue: 7,
    options: [
      { value: 1, label: commonMessage.down },
      // { value: 2, label: 'center' },
      { value: 7, label: commonMessage.positionRight },
      { value: 8, label: commonMessage.up }
    ]
  },
  {
    name: messages.showValue,
    inputType: 'CheckBox',
    fatherEvent: 'dataLabels',
    event: 'showValue'
  },
  {
    name: messages.showName,
    inputType: 'CheckBox',
    fatherEvent: 'dataLabels',
    event: 'showSeriesName',
    funcName: 'SeriesNameVisible'
  },
  {
    name: messages.showType,
    inputType: 'CheckBox',
    fatherEvent: 'dataLabels',
    event: 'showCategoryName',
    funcName: 'CategoryNameVisible'
  }
];

const configClassify = [
  { title: messages.title, configs: BaseConfig.title },
  { title: messages.chartArea, configs: BaseConfig.chartArea },
  { title: messages.legend, configs: BaseConfig.legend },
  { title: messages.series, configs: BaseConfig.series },
  { title: messages.label, configs: dataLabels },
  { title: messages.axis, configs: BaseConfig.axes }
];

export default configClassify;
