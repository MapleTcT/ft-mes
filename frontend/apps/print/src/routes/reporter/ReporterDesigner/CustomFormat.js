import GC from '@grapecity/spread-sheets';
// import GC from 'root/dependencies/spreadjs/gc.spread.sheets.all.12.0.7.min.js';

export const CustomFormat = function () {};
CustomFormat.prototype = new GC.Spread.Formatter.FormatterBase();
CustomFormat.prototype.format = function (value) {
  if (typeof value === 'boolean') {
    return value.toString().toUpperCase();
  }

  return value;
};
