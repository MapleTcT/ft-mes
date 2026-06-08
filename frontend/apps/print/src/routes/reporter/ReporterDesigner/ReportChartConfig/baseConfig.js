import commonMessage from '../commonMessages';
import messages from '../messages';

const fontFamilyOptions = [
  { value: '微软雅黑', label: commonMessage.MicrosoftYaHei },
  { value: '黑体', label: commonMessage.blackbody },
  { value: '楷体', label: commonMessage.regularscript },
  { value: '隶书', label: commonMessage.officialscript },
  { value: '幼圆', label: commonMessage.youngcircle },
  { value: '宋体', label: commonMessage.songtypeface },
  { value: 'Calibri Light', label: 'Calibri Light' }
];

const legendOptions = [
  { value: 1, label: commonMessage.up },
  { value: 2, label: commonMessage.positionRight },
  { value: 3, label: commonMessage.positionLeft },
  { value: 4, label: commonMessage.down }
];

const tickPositionOptions = [
  { value: 0, label: messages.cross },
  { value: 1, label: messages.inside },
  { value: 2, label: commonMessage.null },
  { value: 3, label: messages.outside }
];

const BaseConfig = {
  title: [
    {
      name: messages.Text,
      inputType: 'Input',
      fatherEvent: 'title',
      event: 'text'
    },
    {
      name: messages.FontSize,
      inputType: 'InputNumber',
      fatherEvent: 'title',
      event: 'fontSize'
    },
    {
      name: messages.Font,
      inputType: 'Select',
      fatherEvent: 'title',
      event: 'fontFamily',
      options: fontFamilyOptions
    },
    {
      name: messages.Color,
      inputType: 'ColorSelect',
      fatherEvent: 'title',
      event: 'color',
      defaultValue: 'rgba(89,89,89,1)'
    }
  ],
  chartArea: [
    {
      name: commonMessage.bgColor,
      inputType: 'ColorSelect',
      fatherEvent: 'chartArea',
      event: 'backColor',
      defaultValue: '#fff'
    },
    {
      name: messages.FontSize,
      inputType: 'InputNumber',
      fatherEvent: 'chartArea',
      event: 'fontSize'
    },
    {
      name: messages.Font,
      inputType: 'Select',
      fatherEvent: 'chartArea',
      event: 'fontFamily',
      options: fontFamilyOptions
    },
    {
      name: messages.FontColor,
      inputType: 'ColorSelect',
      fatherEvent: 'chartArea',
      event: 'color',
      defaultValue: '#000'
    }
  ],
  legend: [
    {
      name: messages.position,
      inputType: 'Select',
      fatherEvent: 'legend',
      event: 'position',
      funcName: 'LengePosition',
      options: legendOptions
    },
    {
      name: messages.seriesOrien,
      inputType: 'Select',
      funcName: 'SeriesDirections',
      options: [
        { value: 0, label: messages.rowSeries },
        { value: 1, label: messages.colSeries }
      ]
    },
    {
      name: messages.visible,
      inputType: 'CheckBox',
      fatherEvent: 'legend',
      funcName: 'LengeVisible',
      event: 'visible'
    }
  ],
  dataLabels: [
    {
      name: messages.fontColor,
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
      options: [
        { value: 1, label: 'below' },
        { value: 2, label: 'center' },
        { value: 7, label: 'right' },
        { value: 8, label: 'above' }
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
  ],
  series: [
    {
      name: commonMessage.data,
      inputType: 'SeriesSelect',
      funcName: 'SeriesSelect'
    },
    {
      name: messages.axisGroup,
      inputType: 'Select',
      funcName: 'AxesGroup',
      options: [
        { value: 0, label: messages.major },
        { value: 1, label: messages.minor }
      ]
    },
    {
      name: messages.Color,
      inputType: 'ColorSelect',
      funcName: 'SeriesColor'
    },
    {
      name: messages.lineColor,
      inputType: 'ColorSelect',
      funcName: 'SeriesLineColor'
    },
    {
      name: messages.lineWidth,
      inputType: 'InputNumber',
      funcName: 'SeriesLineWidth'
    }
  ],
  axes: [
    {
      name: messages.Type,
      inputType: 'Select',
      funcName: 'AxesType',
      options: [
        { value: 'primaryCategory', label: messages.bottomX },
        { value: 'primaryValue', label: messages.leftY },
        { value: 'secondaryCategory', label: messages.topX },
        { value: 'secondaryValue', label: messages.rightY }
      ]
    },
    {
      name: messages.Color,
      inputType: 'ColorSelect',
      fatherEvent: 'axes',
      event: 'style.color',
      defaultValue: 'rgba(89,89,89,1)'
    },
    {
      name: messages.FontSize,
      inputType: 'InputNumber',
      fatherEvent: 'axes',
      event: 'style.fontSize'
    },
    {
      name: messages.Font,
      inputType: 'Select',
      fatherEvent: 'axes',
      event: 'style.fontFamily',
      options: fontFamilyOptions
    },
    {
      name: commonMessage.title,
      inputType: 'Input',
      funcName: 'AxesTitleText',
      fatherEvent: 'axes',
      event: 'title.text'
    },
    {
      name: messages.titleFontSize,
      inputType: 'InputNumber',
      fatherEvent: 'axes',
      funcName: 'AxesTitleFontSize',
      event: 'title.fontSize'
    },
    {
      name: messages.Font,
      inputType: 'Select',
      fatherEvent: 'axes',
      funcName: 'AxesTitleFontFamily',
      event: 'title.fontFamily',
      options: fontFamilyOptions
    },
    {
      name: messages.titleColor,
      inputType: 'ColorSelect',
      fatherEvent: 'axes',
      funcName: 'AxesTitleFontColor',
      event: 'title.color',
      defaultValue: 'rgba(89,89,89,1)'
    },
    {
      name: messages.lineColor,
      inputType: 'ColorSelect',
      fatherEvent: 'axes',
      event: 'lineStyle.color',
      defaultValue: 'rgba(89,89,89,1)'
    },
    {
      name: messages.lineWidth,
      inputType: 'InputNumber',
      fatherEvent: 'axes',
      event: 'lineStyle.width'
    },
    {
      name: messages.majorGridColor,
      inputType: 'ColorSelect',
      fatherEvent: 'axes',
      funcName: 'MajorGridLineColor',
      event: 'majorGridLine.color',
      defaultValue: 'rgba(89,89,89,1)'
    },
    {
      name: messages.majorGridLine,
      inputType: 'InputNumber',
      funcName: 'MajorGridLineWidth',
      fatherEvent: 'axes',
      event: 'majorGridLine.width'
    },
    {
      name: messages.minorGridColor,
      inputType: 'ColorSelect',
      funcName: 'MinorGridLineColor',
      fatherEvent: 'axes',
      event: 'minorGridLine.color',
      defaultValue: 'rgba(89,89,89,1)'
    },
    {
      name: messages.minorGridLine,
      inputType: 'InputNumber',
      funcName: 'MinorGridLineWidth',
      fatherEvent: 'axes',
      event: 'minorGridLine.width'
    },
    {
      name: messages.majorTickPosition,
      inputType: 'Select',
      fatherEvent: 'axes',
      event: 'majorTickPosition',
      options: tickPositionOptions
    },
    {
      name: messages.minorTickPosition,
      inputType: 'Select',
      fatherEvent: 'axes',
      event: 'minorTickPosition',
      options: tickPositionOptions
    },
    {
      name: messages.axis,
      inputType: 'CheckBox',
      fatherEvent: 'axes',
      event: 'visible'
    },
    {
      name: messages.majorGridLine,
      inputType: 'CheckBox',
      funcName: 'MajorGridLineVisible',
      fatherEvent: 'axes',
      event: 'majorGridLine.visible'
    },
    {
      name: messages.minorGridLine,
      inputType: 'CheckBox',
      funcName: 'MinorGridLineVisible',
      fatherEvent: 'axes',
      event: 'minorGridLine.visible'
    },
    {
      name: messages.labelPosition,
      inputType: 'Select',
      fatherEvent: 'axes',
      event: 'tickLabelPosition',
      options: [
        { value: 2, label: messages.paraxial },
        { value: 3, label: commonMessage.null }
      ]
    }
  ]
};

export default BaseConfig;
