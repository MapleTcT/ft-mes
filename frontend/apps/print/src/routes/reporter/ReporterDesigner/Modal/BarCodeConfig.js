import messages from '../messages';

const barCodeFlag = {
    type: 'checkbox',
    name: messages.barCodeFlag,
    default: false,
    key: 'showLabel'
}
const barCodePosition = {
    type: 'select',
    option: ['top', 'bottom'],
    default: 'bottom',
    name: messages.barCodePosition,
    key: 'labelPosition'
}
const fontFamily = {
    type: 'select',
    option: ['sans-serif', 'serif', 'monospace', 'Arial', 'Verdana', 'Times'],
    default: 'sans-serif',
    name: messages.barCodeFontFamily,
    parent: 'font',
    key: 'fontFamily'
}
const fontStyle = {
    type: 'select',
    option: ['normal', 'italic'],
    default: 'normal',
    name: messages.barCodeFontStyle,
    parent: 'font',
    key: 'fontStyle'
}
const fontWeight = {
    type: 'select',
    option: ['normal', 'bold'],
    default: 'normal',
    name: messages.barCodeFontWeight,
    parent: 'font',
    key: 'fontWeight'
}
const textDecoration = {
    type: 'select',
    option: ['none', 'underline', 'overline', 'line-through'],
    default: 'none',
    name: messages.barCodeTextDecoration,
    parent: 'font',
    key: 'textDecoration'
}
const textAlign = {
    type: 'select',
    option: ['center', 'left', 'right', 'group'],
    default: 'center',
    name: messages.barCodeTextAlign,
    parent: 'font',
    key: 'textAlign'
}
const fontSize = {
    type: 'select',
    option: [12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22],
    default: 12,
    name: messages.barCodeFontSize,
    parent: 'font',
    key: 'fontSize'
}
const addtionCode = {
    type: 'input',
    name: messages.addtionCode,
    key: 'addOn'
}
const addtionCodePosition = {
    type: 'select',
    option: ['top', 'bottom'],
    default: 'top',
    name: messages.addtionCodePosition,
    key: 'addOnLabelPosition'
}
const validationBit = {
    type: 'checkbox',
    name: messages.validationBit,
    key: 'checkDigit'
}
const widthRate = {
    type: 'select',
    option: [3, 2],
    default: 3,
    name: messages.widthRate,
    key: 'nwRatio'
}
const startEndFlag = {
    type: 'checkbox',
    name: messages.startEndFlag,
    key: 'labelWithStartAndStopCharacter'
}
const ASCIIFlag = {
    type: 'checkbox',
    name: messages.ASCIIFlag,
    key: 'fullASCII'
}
const groupFlag = {
    type: 'checkbox',
    name: messages.groupFlag,
    key: 'grouping'
}
const groupIndex = {
    type: 'input',
    name: messages.groupIndex,
    key: 'groupNo'
}
const codeCollection = {
    type: 'select',
    option: ['auto', 'A', 'B', 'C'],
    default: 'auto',
    name: messages.codeCollection,
    key: 'codeSet'
}

const baseConfig = [barCodeFlag, barCodePosition, fontFamily, fontStyle, fontWeight, textDecoration, textAlign, fontSize];

export const barCodeConfig = {
    'EAN8': baseConfig,
    'EAN13': baseConfig.concat([addtionCode, addtionCodePosition]),
    'CODE39': baseConfig.concat([startEndFlag, validationBit, widthRate, ASCIIFlag]),
    'CODE93': baseConfig.concat([validationBit, ASCIIFlag]),
    'CODE49': baseConfig.concat([groupFlag, groupIndex]),
    'CODE128': baseConfig.concat([codeCollection]),
    'CODABAR': baseConfig.concat([validationBit, widthRate]),
    'GS1_128': baseConfig,
}

export const baseConfigKeys = ["labelPosition", "fontFamily", "fontStyle", "fontWeight", "textDecoration", "textAlign", "fontSize"];

export const quietZoneConfig = [
    [{
        name: messages.leftQuietZone,
        parent: "quietZone",
        key: 'left'
    },
    {
        name: messages.rightQuietZone,
        parent: "quietZone",
        key: 'right'
    }],
    [{
        name: messages.topQuietZone,
        parent: "quietZone",
        key: 'top'
    },
    {
        name: messages.bottomQuietZone,
        parent: "quietZone",
        key: 'bottom'
    }]
]

export const barCodeOption = [
    'EAN8',
    'EAN13',
    'CODE39',
    'CODE93',
    'CODE49',
    'CODE128',
    'CODABAR',
    'GS1_128'
];

export const qrBarCodeOption = [
    'QRCODE',
    'DATAMATRIX'
];
