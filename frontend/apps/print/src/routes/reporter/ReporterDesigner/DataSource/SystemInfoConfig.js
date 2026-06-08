import message from '../messages';

const systemInfo = function (intl) {
  return [
    { propertyType: 'systemInfo', propertyName: intl.formatMessage(message.printPerson), primitiveType: 'string', data: 'return JSON.parse(localStorage.getItem("loginMsg")).user.userName;' },
    { propertyType: 'systemInfo', propertyName: intl.formatMessage(message.printTime), primitiveType: 'string', data: 'return new Date().toLocaleString();' }
  ];
};

export { systemInfo };
