import React, { Component } from 'react';
import _ from 'lodash';
import { Button, Tabs, List, Input, InputNumber, Checkbox, Select, Radio, Tree, message } from 'sup-ui';
import InfiniteScroll from 'react-infinite-scroller';
import GC from '@grapecity/spread-sheets';
import { fontSizeItems, fontFamilyItems } from '../../utils/constants';
import Modal from './CommonModal';
// import { newQueryRoleList, queryRoleUser } from 'root/services/api';
// import GC from 'root/dependencies/spreadjs/gc.spread.sheets.all.12.0.7.min.js';
import { cellSettings } from '../ReportMenuBarConfig';
import ColorPicker from '../ColorPicker';
import icons from '../Reporter.less';
import styles from './CellSettingModal.less';
import messages from '../messages';

const { TabPane } = Tabs;
const { TreeNode, DirectoryTree } = Tree;
const { Item } = List;
const { Option } = Select;
const RadioGroup = Radio.Group;
const { Search } = Input;

export default class CellSettingModal extends Component {
  constructor(props) {
    super();
    this.numberFormatItems = [
      props.intl.formatMessage(messages.Normal),
      props.intl.formatMessage(messages.Values),
      props.intl.formatMessage(messages.Text),
      props.intl.formatMessage(messages.Dates),
      props.intl.formatMessage(messages.Money),
      props.intl.formatMessage(messages.Percentage),
      props.intl.formatMessage(messages.SelfDefined)
    ];
    this.dateFormatItems = [
      { format: 'yyyy-MM-dd HH:mm:ss', example: '1899-12-30 00:00:00' },
      { format: 'yyyy-MM-dd', example: '1899-12-30' },
      { format: 'HH:mm:ss', example: '00:00:00' }
    ];
    this.dateFormat = _.get(props, 'numberFormat.dateFormat', 'yyyy-MM-dd HH:mm:ss');
    this.negativeFormatItems = [
      { format: { color: 'red', type: 1 }, example: (<span style={{ color: 'red' }} >(1234.10)</span>) },
      { format: { color: 'black', type: 2 }, example: '(1234.10)' },
      { format: { color: 'red', type: 3 }, example: (<span style={{ color: 'red' }} >1234.10</span>) },
      { format: { color: 'black', type: 4 }, example: '-1234.10' },
      { format: { color: 'red', type: 5 }, example: (<span style={{ color: 'red' }} >-1234.10</span>) }
    ];

    this.fontStyleWeightItems = [
      { style: { fontStyle: 'normal', fontWeight: 'normal' }, text: props.intl.formatMessage(messages.Normal) },
      { style: { fontStyle: 'italic', fontWeight: 'normal' }, text: props.intl.formatMessage(messages.Italics) },
      { style: { fontWeight: '700', fontStyle: 'normal' }, text: props.intl.formatMessage(messages.Bolds) },
      { style: { fontWeight: '700', fontStyle: 'italic' }, text: props.intl.formatMessage(messages.ItalicBold) }
    ];
    this.lineItems = [
      { style: 'none', example: (<div style={{ width: 200, textAlign: 'center' }} >{props.intl.formatMessage(messages.Null)}</div>) },
      { style: 'solid', example: (<div style={{ border: '1px solid rgba(0,0,0,1)', width: 200 }} />) },
      { style: 'dotted', example: (<div style={{ border: '1px dotted rgba(0,0,0,1)', width: 200 }} />) },
      { style: 'dashed', example: (<div style={{ border: '1px dashed rgba(0,0,0,1)', width: 200 }} />) },
      { style: 'double', example: (<div style={{ borderTop: 'medium double rgba(0,0,0,1)', width: 200 }} />) }
    ];
    this.lineTypes = ['top', 'bottom', 'left', 'right', 'vertical', 'horizontal'];
    const isNumber = this.isNumber(_.get(props, 'numberFormat.example', false));
    this.state = this.getInitStateObj({ isNumber });

    this.hasInitialFont = false;
    this.hasInitialBorder = false;
    this.hasInitialAlignment = false;
    this.hasInitialBackground = false;
    this.hasInitialCellType = false;
    this.hasAuthority = false;
  }

  componentDidMount() {
    this.pageIndex = 1;
    const { intl } = this.props;
    this.changeTab(intl.formatMessage(messages.Number));
    this.changeNumType(this.props.numberFormat);
  }

  onLoadData = (treeNode) => {
    const { intl } = this.props;
    const { dataRef } = treeNode.props;
    const { roleData } = this.state;
    return new Promise((resolve) => {
      if (treeNode.props.children) {
        resolve();
        return;
      }
      // queryRoleUser({ name: dataRef.name }).then((res) => {
      //   if (res.list) {
      //     if (res.list.length < 1) message.info(intl.formatMessage(messages.NoChild));
      //     for (let i = 0; i < roleData.list.length; i += 1) {
      //       if (roleData.list[i].name === dataRef.name) {
      //         roleData.list[i].userData = res;
      //       }
      //     }
      //     this.setState({
      //       roleData
      //     });
      //   }
      //   resolve();
      // });
    });
  }

  getRoleList = (params) => {
    // newQueryRoleList(params).then((res) => {
    //   if (+res.code === 200) {
    //     const roleData = params.isSearch ? res : {
    //       list: _.get(this.state, 'roleData.list', []).concat(res.list || []),
    //       pagination: res.pagination
    //     };
    //     this.setState({
    //       roleData
    //     });
    //   }
    // });
  }

  getFontStyleWeightItemByStyle = (fontStyle, fontWeight) => {
    return this.fontStyleWeightItems.find((item) => {
      return item.style.fontStyle === fontStyle && item.style.fontWeight === fontWeight;
    });
  }

  getInitStateObj = ({ isNumber }) => {
    return {
      numberExample: '',
      decimal: 2,
      currency: '',
      negativeItem: { format: { type: 4 } },
      useThousandSeparator: false,
      decimalVisiable: false,
      thousandSeparatorVisiable: false,
      currencyVisiable: false,
      dateVisiable: false,
      negativeVisiable: false,
      isNumber,
      roleData: { list: [] },
      selectedKeys: [],
      authority: {},
      expandedKeys: [],
      searchName: '',
      template: ''
    };
  }

  getNegativeExample = (value) => {
    let result = value;
    if (this.isNumber(value)) {
      const { type } = this.state.negativeItem.format;
      result = Math.abs(Number(value)).toFixed(this.state.decimal);
      if (type === 1 || type === 2) {
        return `(${result})`;
      } else if (type === 3) {
        return result;
      } else {
        return Number(value).toFixed(this.state.decimal);
      }
    }
    return result;
  }

  getCurrencyNegativeFormat = (format, currency) => {
    switch (this.state.negativeItem.format.type) {
      case 1: return `${currency}${format}_);[Red](${currency}${format})`;
      case 2: return `${currency}${format}_);(${currency}${format})`;
      case 3: return `${currency}${format};[Red]${currency}${format}`;
      case 4: return `${currency}${format};${currency}-${format}`;
      case 5: return `${currency}${format};[Red]${currency}-${format}`;
      default: return '';
    }
  }

  setCurrency = (e) => {
    this.setState({
      currency: e
    });
  }

  setDecimal = (e) => {
    this.setState({
      decimal: e
    }, () => {
      this.setNumber();
    });
  }

  setThousandSeparator = (e) => {
    this.setState({
      useThousandSeparator: e.target ? e.target.checked : e
    }, () => {
      this.setNumber();
    });
  }

  setNegative = (item) => {
    this.setState({
      negativeItem: item
    }, () => {
      if (this.isNumber(this.props.numberFormat.example) && Number(this.props.numberFormat.example) < 0) {
        this.setNumber();
        this.setState({
          exampleColor: item.format.color
        });
      }
    });
  }

  setDate = (fmt) => {
    this.dateFormat = fmt;
    const instance = new GC.Spread.Formatter.GeneralFormatter(fmt);
    const example = _.get(this.props, 'numberFormat.example', null);
    this.setState({
      numberExample: this.isDate(example) ? instance.format(example) : example
    });
  }

  setNumber = () => {
    const { intl } = this.props;
    let value = _.get(this.props, 'numberFormat.example', null);
    if (!this.isNumber(value)) return;

    if (this.selectedNumType === intl.formatMessage(messages.Percentage)) {
      value = Number(value) * 100;
    }

    // 设置小数
    if (this.state.decimal >= 0) {
      value = Number(value).toFixed(this.state.decimal);
    }

    if ([intl.formatMessage(messages.Values), intl.formatMessage(messages.Money)].includes(this.selectedNumType)) {
      // 设置负数
      let useSpecialType = false;
      if (Number(value) < 0 && this.state.negativeItem.format) {
        const { type } = this.state.negativeItem.format;
        if (type === 1 || type === 2) useSpecialType = true;
        value = this.getNegativeExample(value);
      }

      // 设置千分符
      if (this.state.useThousandSeparator) {
        value = useSpecialType
          ? `(${this.addThousandSeparator(value.slice(1, -1))})`
          : this.addThousandSeparator(value);
      } else {
        value = this.removeThousandSeparator(value);
      }
    }

    this.setState({
      numberExample: value
    });
  }

  setFontColor = (fontColor) => {
    this.setFontStyle({ fontColor });
  }

  setTextDecoration = (textDecoration, e) => {
    this.setFontStyle(e.target.checked ? { textDecoration } : 'none');
  }

  setFontStyle = ({
    fontFamily = this.state.fontFamily,
    fontStyleWeight,
    fontSize = this.state.fontSize,
    fontColor = this.state.fontColor,
    textDecoration = this.state.textDecoration
  }) => {
    if (fontStyleWeight) {
      this.fontStyle = fontStyleWeight.style.fontStyle;
      this.fontWeight = fontStyleWeight.style.fontWeight;
    } else {
      fontStyleWeight = Object.assign({}, this.state.fontStyleWeight);
    }
    this.setState({
      fontFamily, fontStyleWeight, fontSize, fontColor, textDecoration
    });
  }

  setBorderColor = (value) => {
    if (value) {
      this.setBorderStyle({ color: value });
    }
  }

  setBorderStyle = ({ border, style, color, action }) => {
    if (style) {
      this.resetBorderWidth(style === 'double' ? 'medium' : 1);
      this.setPreviewStyle('borderStyle', style);
      this.borderStyle = style;
    } else {
      let width = this.borderStyle === 'double' ? 'medium' : 1;
      if (border) {
        this.borderChanged = true;
        const borderObj = {};
        borderObj[border] = this.state[border] ? 0 : width;
        this.setState(borderObj);
      } else if (color) {
        this.setPreviewStyle('borderColor', color);
        this.borderColor = color;
      } else if (action) {
        this.borderChanged = true;
        switch (action) {
          case 'none': width = 0; break;
          default: break;
        }
        this.resetBorderWidth(width, action);
      }
    }
  }

  setPreviewStyle = (attr, value) => {
    for (let i = 0; i < this.preview.children.length; i += 1) {
      this.preview.children[i].style[attr] = value;
    }
  }

  setStateObject = (name, value) => {
    this.setState({ [name]: value });
  }

  setTemplate = (e) => {
    const { numberFormat: { example } } = this.props;
    const { value } = e.target;
    const index = value.indexOf('$');
    const lastIndex = value.lastIndexOf('$');
    const prefix = value.substr(0, index).replace(/\$/g, '');
    const suffix = value.substr(lastIndex).replace(/\$/g, '');
    if (this.checkCharacter(prefix) || this.checkCharacter(suffix)) return;
    this.setState({
      template: `${prefix}$$$${suffix}`,
      numberExample: `${prefix}${example || '$$$'}${suffix}`
    });
  }

  checkCharacter = (value) => {
    return !!_.find(['#', '"', ';', '0', '_', '*', '[', '!', '@', '%', '.', '/', '?', '\\'], (item) => value.includes(item));
  }

  treeSelect = (selectedKeys) => {
    this.setState({ selectedKeys });
  }

  checkBoxEvent = (type, e) => {
    this.setStateObject(type, e.target.checked);
  }

  listCellTypeOperation = (opt) => {
    const parent = document.getElementById('combo');
    switch (opt) {
      case 'add': {
        const node = document.createElement('Input');
        node.setAttribute('placeholder', 'New Item');
        node.onfocus = (e) => {
          this.currentInput = e.currentTarget;
        };
        parent.appendChild(node);
        break;
      }
      case 'reduce': {
        if (parent.childNodes.length) {
          parent.removeChild(parent.lastChild);
        }
        break;
      }
      case 'up': {
        const node = this.currentInput.previousSibling;
        if (node) {
          parent.removeChild(this.currentInput);
          parent.insertBefore(this.currentInput, node);
        }
        break;
      }
      case 'down': {
        const node = this.currentInput.nextSibling;
        if (node) {
          parent.removeChild(this.currentInput);
          parent.insertBefore(this.currentInput, node.nextSibling);
        }
        break;
      }
      default: break;
    }
  }

  resetBorderWidth = (width, action) => {
    const borderObj = {};
    this.lineTypes.forEach((item) => {
      if (item === 'vertical' || item === 'horizontal') {
        if (action !== 'all') {
          borderObj[item] = action ? width : (this.state[item] ? width : 0);
        }
      } else if (action !== 'inside') {
        borderObj[item] = action ? width : (this.state[item] ? width : 0);
      }
    });
    this.setState(borderObj);
  }

  changeFontStyle = (state, e) => {
    const fontObj = {};
    fontObj[state] = e.target.value;
    this.setState(fontObj);
  }

  isDate = (date) => {
    return !isNaN(new Date(date).getTime());
  }

  isNumber = (num) => {
    return num !== '' && !isNaN(Number(num));
  }

  removeThousandSeparator = (value) => {
    return value.toString().replace(/,/g, '');
  }

  addThousandSeparator = (value) => {
    return value.toString().replace(/^(-?\d+)((\.\d+)?)$/, (v1, v2, v3) => {
      return v2.replace(/\d{1,3}(?=(\d{3})+$)/g, '$&,') + v3;
    });
  }

  initCellCount = () => {
    if (this.props.borderStyles) {
      this.fourCells = this.props.borderStyles.rowCount > 1 && this.props.borderStyles.colCount > 1;
      this.twoCols = this.props.borderStyles.rowCount === 1 && this.props.borderStyles.colCount > 1;
      this.twoRows = this.props.borderStyles.rowCount > 1 && this.props.borderStyles.colCount === 1;
    }
  }

  changeTab = (key) => {
    const { intl } = this.props;
    switch (key) {
      case intl.formatMessage(messages.BorderSet): {
        if (!this.hasInitialBorder) {
          this.setState({
            top: 0,
            bottom: 0,
            left: 0,
            right: 0,
            vertical: 0,
            horizontal: 0,
            up: 0,
            down: 0
          });
          this.hasInitialBorder = true;
          this.borderColor = 'rgb(0, 0, 0)';
          this.borderStyle = 'solid';
          this.initCellCount();
        }
        break;
      }
      case intl.formatMessage(messages.Font): {
        if (!this.hasInitialFont) {
          const {
            fontFamily = 'Calibri',
            fontSize = '11',
            fontStyle = 'normal',
            fontWeight = 'normal',
            fontColor = 'rgba(0,0,0,1)',
            textDecoration = { underline: false, lineThrough: false }
          } = this.props.fontStyles || {};
          this.setState({
            fontFamily,
            fontSize,
            fontStyleWeight: this.getFontStyleWeightItemByStyle(fontStyle, fontWeight) || { text: '常规' },
            fontColor,
            textDecoration,
            underline: textDecoration.underline,
            lineThrough: textDecoration.lineThrough

          });
          this.hasInitialFont = true;
        }
        break;
      }
      case intl.formatMessage(messages.Picture): {
        if (!this.hasInitialBackground && this.props.bgColor) {
          this.setState({
            bgColor: this.props.bgColor
          });
          this.hasInitialBackground = true;
        }
        break;
      }
      case intl.formatMessage(messages.Type): {
        if (!this.hasInitialCellType) {
          this.setState({
            cellType: 'text'
          });
          this.hasInitialCellType = true;
        }
        break;
      }
      case intl.formatMessage(messages.Align): {
        if (!this.hasInitialAlignment) {
          const {
            vAlign = 'top',
            hAlign = 'general',
            wordWrap,
            isVerticalText
          } = this.props.alignment || {};
          this.setState({ vAlign, hAlign, wordWrap, isVerticalText });
          this.hasInitialAlignment = true;
        }
        break;
      }
      case intl.formatMessage(messages.Authority): {
        if (!this.hasAuthority) {
          this.getRoleList({ roleName: '', pageIndex: 1 });
          this.setState({
            authority: this.props.authority || {}
          });
          this.hasAuthority = true;
        }
        break;
      }
      default: break;
    }
  }

  changeNumType = ({
    numberType = this.numberFormatItems[0],
    decimal = this.state.decimal,
    currency = this.state.currency,
    negativeItem = this.state.negativeItem,
    useThousandSeparator = this.state.useThousandSeparator || false,
    numberFormat
  }) => {
    const { intl } = this.props;
    this.selectedNumType = numberType;
    this.setState({
      decimalVisiable: false,
      thousandSeparatorVisiable: false,
      currencyVisiable: false,
      dateVisiable: false,
      negativeVisiable: false,
      exampleColor: 'rgb(100, 100, 100)',
      numberExample: _.get(this.props, 'numberFormat.example', null)
    }, () => {
      switch (numberType) {
        case intl.formatMessage(messages.Values): {
          this.setThousandSeparator(useThousandSeparator);
          this.setDecimal(decimal);
          this.setNegative(negativeItem);
          this.setState({
            numberFormatDesp: intl.formatMessage(messages.CellRule1),
            decimalVisiable: true,
            thousandSeparatorVisiable: true,
            negativeVisiable: true
          });
          break;
        }
        case intl.formatMessage(messages.Dates): {
          this.setDate(this.dateFormat);
          this.setState({
            numberFormatDesp: intl.formatMessage(messages.CellRule2),
            dateVisiable: true
          });
          break;
        }
        case intl.formatMessage(messages.Money): {
          this.setCurrency(currency);
          this.setThousandSeparator(useThousandSeparator);
          this.setDecimal(decimal);
          this.setNegative(negativeItem);
          this.setState({
            numberFormatDesp: intl.formatMessage(messages.CellRule3),
            decimalVisiable: true,
            currencyVisiable: true,
            negativeVisiable: true
          });
          break;
        }
        case intl.formatMessage(messages.Percentage): {
          this.setThousandSeparator(false);
          this.setDecimal(decimal);
          this.setState({
            numberFormatDesp: intl.formatMessage(messages.CellRule4),
            decimalVisiable: true
          });
          break;
        }
        case intl.formatMessage(messages.Text): {
          this.setState({
            numberFormatDesp: intl.formatMessage(messages.CellRule5),
            decimalVisiable: false,
            numberExample: _.get(this.props, 'numberFormat.example', null)
          });
          break;
        }
        case intl.formatMessage(messages.Normal): {
          this.setState({
            numberFormatDesp: intl.formatMessage(messages.CellRule6),
            numberExample: _.get(this.props, 'numberFormat.example', null)
          });
          break;
        }
        case intl.formatMessage(messages.SelfDefined): {
          const example = _.get(this.props, 'numberFormat.example', '');
          const { template } = this.state;
          const newTemplate = template || (numberFormat && numberFormat.replace('@', '$$$$$$').replace(/"/g, '')) || '$$$';
          this.setState({
            numberFormatDesp: intl.formatMessage(messages.CellRule7),
            template: newTemplate,
            numberExample: example ? newTemplate.replace('$$$', example) : newTemplate
          });
          break;
        }
        default: break;
      }
    });
  }

  authorityChange = (e) => {
    const { selectedKeys, authority } = this.state;
    if (selectedKeys.length) {
      _.map(selectedKeys, (item) => {
        authority[item] = e.target.value;
      });
      this.setState({ authority });
    }
  }

  searchItem = (value) => {
    this.setState({
      selectedKeys: [],
      expandedKeys: [],
      roleData: { list: [] },
      searchName: value.trim()
    });
    this.getRoleList({ roleName: value.trim(), pageIndex: 1, isSearch: true });
  }

  handleOk = () => {
    const { intl } = this.props;
    const { template, decimal, currency, negativeItem, useThousandSeparator, cellType } = this.state;
    const format = {};
    if (this.selectedNumType === intl.formatMessage(messages.Normal)) {
      format.numberFormat = '';
    } else if (this.selectedNumType === intl.formatMessage(messages.Text)) {
      format.numberFormat = '@';
    } else if (this.selectedNumType === intl.formatMessage(messages.Dates)) {
      format.numberFormat = this.dateFormat;
      format.dateFormat = this.dateFormat;
    } else if (this.selectedNumType === intl.formatMessage(messages.SelfDefined)) {
      const index = template.indexOf('$$$');
      const prefix = template.substr(0, index);
      const suffix = template.substr(index + 3);
      format.numberFormat = `${prefix}##${suffix}`;
    } else {
      // 千分符
      format.useThousandSeparator = useThousandSeparator;
      format.numberFormat = useThousandSeparator ? '##,##0' : '0';
      // 小数
      format.decimal = decimal;
      if (decimal > 0) {
        format.numberFormat = `${format.numberFormat}.${Number().toFixed(decimal).split('.')[1]}`;
      }

      if (this.selectedNumType === intl.formatMessage(messages.Money)) {
        format.numberFormat = this.getCurrencyNegativeFormat(format.numberFormat, currency || '');
        format.negativeItem = negativeItem;
        format.currency = currency;
      } else if (this.selectedNumType === intl.formatMessage(messages.Values)) {
        format.numberFormat = this.getCurrencyNegativeFormat(format.numberFormat, '');
        format.negativeItem = negativeItem;
      } else if (this.selectedNumType === intl.formatMessage(messages.Percentage)) {
        format.useThousandSeparator = false;
        format.numberFormat = `${Number(0).toFixed(decimal)}%`;
      }
    }

    const combo = document.getElementById('combo');
    const comboItems = [];
    if (this.hasInitialCellType && cellType === 'list') {
      for (let i = 0; i < combo.children.length; i += 1) {
        comboItems.push(combo.children[i].value || 'New Item');
      }
    }

    const {
      fontFamily, fontSize, fontColor, underline, lineThrough,
      top, bottom, left, right, vertical, horizontal, up, down,
      vAlign, hAlign, wordWrap, isVerticalText,
      bgColor, authority
    } = this.state;
    const cellSettingSummary = {
      changedTabs: {
        numberType: this.selectedNumType,
        font: this.hasInitialFont,
        border: !!this.borderChanged,
        align: this.hasInitialAlignment,
        background: this.hasInitialBackground,
        cellType: this.hasInitialCellType
      },
      format,
      fontStyles: {
        fontFamily,
        fontSize,
        fontWeight: this.fontWeight,
        fontStyle: this.fontStyle,
        fontColor,
        underline,
        lineThrough
      },
      borderStyles: {
        borderStyle: this.borderStyle,
        borderColor: this.borderColor,
        borderOptions: {
          top: Boolean(top),
          bottom: Boolean(bottom),
          left: Boolean(left),
          right: Boolean(right),
          innerVertical: Boolean(vertical),
          innerHorizontal: Boolean(horizontal),
          diagonalUp: Boolean(up),
          diagonalDown: Boolean(down)
        }
      },
      alignment: {
        vAlign,
        hAlign,
        isWrap: wordWrap,
        isVerticalText
      },
      bgColor,
      cellTypes: {
        type: cellType,
        comboItems
      },
      authority
    };
    this.props.basicOperate({ opt: 'setCellSettings', options: { ...cellSettingSummary } });
    this.handleCancel();
  }

  handleCancel = () => {
    this.props.showOrHideModal({ cellSettingVisiable: false });
  }

  renderNumber = () => {
    const { intl } = this.props;
    const { currency = '', example = '' } = _.get(this.props, 'numberFormat', {});
    return (
      <div className={styles.flexRow}>
        <div className={styles.flexColumn}>
          <span>
            {intl.formatMessage(messages.Classify)}
            :
          </span>
          <List
            bordered
            className={styles.numberFormat}
            itemLayout="horizontal"
            dataSource={this.numberFormatItems}
            renderItem={(item) => (
              <Item
                onClick={this.changeNumType.bind(this, { numberType: item })}
                className={this.selectedNumType === item ? styles.selectedNumType : ''}
              >
                {item}
              </Item>
            )}
          />
        </div>
        <div className={styles.flexColumn} style={{ marginLeft: 20 }}>
          <span>
            {intl.formatMessage(messages.Instances)}
            :
          </span>
          <div
            className={styles.numberFormatExample}
            style={{ color: this.state.exampleColor }}
          >
            {`${this.selectedNumType === intl.formatMessage(messages.Money) && this.state.isNumber ? this.state.currency || currency : ''} ${this.state.numberExample || example}`}
            <span style={{ display: (this.selectedNumType === intl.formatMessage(messages.Percentage) && this.state.isNumber ? 'inline' : 'none') }}>%</span>
          </div>
          <div
            className={styles.decimal}
            style={{ display: (this.state.decimalVisiable ? 'flex' : 'none') }}
          >
            <span>
              {intl.formatMessage(messages.Decimal)}
              ：
            </span>
            <InputNumber
              min={0}
              max={10}
              step={1}
              value={this.state.decimal}
              onChange={this.setDecimal}
            />
          </div>
          <Checkbox
            style={{ display: (this.state.thousandSeparatorVisiable ? 'inline' : 'none') }}
            onChange={this.setThousandSeparator}
            checked={this.state.useThousandSeparator}
          >
            {intl.formatMessage(messages.Micrometer)}
            （，）
          </Checkbox>
          <Select
            style={{ width: 400, margin: '10px 0', display: (this.state.currencyVisiable ? 'inline' : 'none') }}
            onChange={this.setCurrency}
            value={this.state.currency}
          >
            <Option value="">{intl.formatMessage(messages.Null)}</Option>
            <Option value="￥">￥</Option>
            <Option value="€">€</Option>
            <Option value="$">$</Option>
          </Select>
          <div style={{ margin: '10px 0', display: (this.state.dateVisiable ? 'inline' : 'none') }} >
            <span>
              {intl.formatMessage(messages.Type)}
              :
            </span>
            <List
              bordered
              className={styles.dateFormat}
              itemLayout="horizontal"
              dataSource={this.dateFormatItems}
              renderItem={(item) => (
                <Item
                  onClick={this.setDate.bind(this, item.format)}
                  className={this.dateFormat === item.format ? styles.selectedNumType : ''}
                >
                  {item.example}
                </Item>
              )}
            />
          </div>
          <div style={{ margin: '10px 0', display: (this.state.negativeVisiable ? 'inline' : 'none') }}>
            <span>
              {intl.formatMessage(messages.Negative)}
              :
            </span>
            <List
              bordered
              className={styles.dateFormat}
              itemLayout="horizontal"
              dataSource={this.negativeFormatItems}
              renderItem={(item) => (
                <Item
                  onClick={this.setNegative.bind(this, item)}
                  className={this.state.negativeItem.format.type === item.format.type ? styles.selectedNumType : ''}
                >
                  {item.example}
                </Item>
              )}
            />
          </div>
          <div style={{ margin: '20px 0', display: (this.selectedNumType === intl.formatMessage(messages.SelfDefined) ? 'inline' : 'none') }}>
            <span>{intl.formatMessage(messages.Input)}</span>
            <Input
              size="large"
              value={this.state.template}
              onChange={this.setTemplate}
            />
            <span>{intl.formatMessage(messages.CellRule8)}</span>
          </div>
        </div>
      </div>

    );
  }

  renderBorder = () => {
    const { intl } = this.props;
    return (
      <div className={styles.borderStyle}>
        <div className={styles.flexRow}>
          <div className={styles.flexColumn} style={{ marginRight: 20 }}>
            <span>{intl.formatMessage(messages.Line)}</span>
            <div className={styles.lineSelection}>
              <span>{intl.formatMessage(messages.Style)}</span>
              <div className={styles.borderExample}>
                <List
                  bordered
                  className={styles.borderFormat}
                  itemLayout="horizontal"
                  dataSource={this.lineItems}
                  renderItem={(item) => (
                    <Item
                      onClick={this.setBorderStyle.bind(this, { style: item.style })}
                      className={this.borderStyle === item.style ? styles.selectedBorderStyle : ''}
                    >
                      {item.example}
                    </Item>
                  )}
                />
              </div>

              <div style={{ marginTop: 15 }}>
                <span>{intl.formatMessage(messages.Color)}</span>
                <ColorPicker
                  name="borderColor"
                  borderRadius={0}
                  colorWidth={160}
                  colorHeight={20}
                  left={0}
                  edit={this.setBorderColor}
                  defaultValue={this.borderColor}
                />
              </div>
            </div>
          </div>
          <div className={styles.flexColumn}>
            <span>
              {intl.formatMessage(messages.PreSet)}
              :
            </span>
            <div className={styles.borderPreset}>
              {
                _.map(['none', 'all', 'inside'], (item) => {
                  return this.renderBtn(item);
                })
              }
            </div>

            <span>
              {intl.formatMessage(messages.Border)}
              :
            </span>

            <div className={styles.border} >
              <div className={styles.flexRow} >
                <div className={styles.flexColumn} >
                  {
                    _.map(['top', 'horizontal', 'bottom'], (item) => {
                      return this.renderBtn(item);
                    })
                  }
                </div>
                <div className={styles.borderPreview}>
                  <div
                    className={styles.preview}
                    ref={(preview) => { this.preview = preview; }}
                  >
                    <div style={{
                      borderTopWidth: this.state.top,
                      borderBottomWidth: this.fourCells || this.twoRows ? (this.state.horizontal || 0) : this.state.bottom,
                      borderLeftWidth: this.state.left,
                      borderRightWidth: this.fourCells || this.twoCols ? (this.state.vertical || 0) : this.state.right,
                      height: this.twoRows || this.fourCells ? '50%' : '100%',
                      width: this.twoCols || this.fourCells ? '50%' : '100%'
                    }}
                    >
                      {intl.formatMessage(messages.Text)}
                    </div>
                    <div style={{
                      borderTopWidth: this.state.top,
                      borderBottomWidth: this.fourCells ? (this.state.horizontal || 0) : this.state.bottom,
                      borderRightWidth: this.state.right,
                      height: this.twoCols ? '100%' : (this.fourCells ? '50%' : 0),
                      width: this.twoCols || this.fourCells ? '50%' : 0,
                      display: this.twoCols || this.fourCells ? 'flex' : 'none'
                    }}
                    >
                      {intl.formatMessage(messages.Text)}
                    </div>
                    <div style={{
                      borderBottomWidth: this.state.bottom,
                      borderLeftWidth: this.state.left,
                      borderRightWidth: this.fourCells ? (this.state.vertical || 0) : this.state.right,
                      height: this.twoRows || this.fourCells ? '50%' : 0,
                      width: this.twoRows ? '100%' : (this.fourCells ? '50%' : 0),
                      display: this.twoRows || this.fourCells ? 'flex' : 'none'
                    }}
                    >
                      {intl.formatMessage(messages.Text)}
                    </div>
                    <div style={{
                      borderBottomWidth: this.state.bottom,
                      borderRightWidth: this.state.right,
                      height: this.fourCells ? '50%' : 0,
                      width: this.fourCells ? '50%' : 0,
                      display: this.fourCells ? 'flex' : 'none'
                    }}
                    >
                      {intl.formatMessage(messages.Text)}
                    </div>

                  </div>
                </div>
              </div>
              <div className={styles.flexRow}>
                {
                  _.map(['up', 'left', 'vertical', 'right', 'down'], (item) => {
                    return this.renderBtn(item);
                  })
                }
              </div>
            </div>
          </div>
        </div>
        <span>{intl.formatMessage(messages.CellRule9)}</span>
      </div>
    );
  }

  renderBtn = (type) => {
    const item = cellSettings(this.props.intl)[type];
    let disabled = false;
    switch (type) {
      case 'horizontal': disabled = !(this.fourCells || this.twoRows); break;
      case 'vertical': disabled = !(this.fourCells || this.twoCols); break;
      case 'inside': disabled = !(this.fourCells || this.twoRows || this.twoCols); break;
      default: break;
    }
    const paramObj = {};
    if (item.action) {
      paramObj.action = item.action;
    } else {
      paramObj.border = type;
    }
    return (
      <div key={type} className={styles.flexRow}>
        <Button
          className={this.state[type] && !disabled ? styles.selectedBorder : ''}
          onClick={this.setBorderStyle.bind(this, paramObj)}
          disabled={disabled}
        >
          <i className={`${icons[item.icon]} ${styles.borderIcon}`} />
        </Button>
        <span>{item.spanContent}</span>
      </div>
    );
  }

  renderBackground = () => {
    const { intl } = this.props;
    return (
      <div className={styles.background}>
        <span>{intl.formatMessage(messages.Cellshade)}</span>
        <div className={styles.flexRow}>
          <div className={styles.flexColumn}>
            <span>
              {intl.formatMessage(messages.Color)}
              :
            </span>
            <div style={{ width: 260 }} />
          </div>
          <div className={styles.flexColumn}>
            <span>{intl.formatMessage(messages.Example)}</span>
            <div className={styles.backgroundPreview}>
              <ColorPicker
                ifTriangle={false}
                name="bgColor"
                borderRadius={0}
                colorWidth={250}
                colorHeight={100}
                left={-260}
                top={0}
                edit={(color) => { this.setStateObject('bgColor', color); }}
                defaultValue={this.state.bgColor}
                lastingShown
              />
            </div>
          </div>

        </div>
      </div>
    );
  }

  renderType = () => {
    const { intl } = this.props;
    return (
      <div className={styles.cellType}>
        <div style={{ width: '30%', padding: 5 }} className={styles.flexColumn}>
          <span>
            {intl.formatMessage(messages.CellType)}
            :
          </span>
          <Select
            style={{ width: 100, marginTop: 10 }}
            onChange={this.setStateObject.bind(this, 'cellType')}
            value={this.state.cellType}
          >
            <Option value="text">{intl.formatMessage(messages.Editframe)}</Option>
            <Option value="list">{intl.formatMessage(messages.Listframe)}</Option>
          </Select>
        </div>
        <div style={{ width: '70%', padding: 5 }} className={styles.flexColumn}>
          <span>
            {intl.formatMessage(messages.Sets)}
            :
          </span>
          <div className={styles.cellTypeSetting}>
            <div style={{ display: (this.state.cellType === 'list' ? 'inline' : 'none') }}>
              <div>
                <div style={{ float: 'left' }}>
                  {intl.formatMessage(messages.List)}
                  :
                </div >
                <div style={{ float: 'right' }}>
                  <Button className={styles.cellTypeIcon} onClick={this.listCellTypeOperation.bind(this, 'add')}>
                    <i className="fa fa-plus-circle" />
                  </Button>
                  <Button className={styles.cellTypeIcon} onClick={this.listCellTypeOperation.bind(this, 'reduce')}>
                    <i className="fa fa-minus-circle" />
                  </Button>
                  <Button className={styles.cellTypeIcon} onClick={this.listCellTypeOperation.bind(this, 'up')}>
                    <i className="fa fa-chevron-up" />
                  </Button>
                  <Button className={styles.cellTypeIcon} onClick={this.listCellTypeOperation.bind(this, 'down')}>
                    <i className="fa fa-chevron-down" />
                  </Button>
                </div>
              </div>
              <div className={styles.comboContainer}>
                <div className={styles.flexColumn} id="combo" />
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  renderAlign = () => {
    const { intl } = this.props;
    return (
      <div className={styles.alignment}>
        <span>{intl.formatMessage(messages.TextAlign)}</span>
        <div className={styles.alignmentSelection} style={{ height: 150 }}>
          <span>
            {intl.formatMessage(messages.HorizontalAlign)}
            ：
          </span>
          <Select defaultValue={this.state.hAlign} style={{ width: 150 }} onChange={this.setStateObject.bind(this, 'hAlign')}>
            <Option value="general">{intl.formatMessage(messages.Normal)}</Option>
            <Option value="left">{intl.formatMessage(messages.Left)}</Option>
            <Option value="center">{intl.formatMessage(messages.Center)}</Option>
            <Option value="right">{intl.formatMessage(messages.Right)}</Option>
          </Select>
          <span>
            {intl.formatMessage(messages.VerticalAlign)}
            ：
          </span>
          <Select defaultValue={this.state.vAlign} style={{ width: 150 }} onChange={this.setStateObject.bind(this, 'vAlign')}>
            <Option value="top">{intl.formatMessage(messages.Top)}</Option>
            <Option value="center">{intl.formatMessage(messages.Center)}</Option>
            <Option value="bottom">{intl.formatMessage(messages.Bottom)}</Option>
          </Select>
        </div>
        <span>{intl.formatMessage(messages.TextCtrl)}</span>
        <div className={styles.alignmentSelection} >
          <Checkbox
            onChange={this.checkBoxEvent.bind(this, 'wordWrap')}
            checked={this.state.wordWrap}
          >
            {intl.formatMessage(messages.wordWrap)}
          </Checkbox>
          <Checkbox
            onChange={this.checkBoxEvent.bind(this, 'isVerticalText')}
            checked={this.state.isVerticalText}
          >
            {intl.formatMessage(messages.TextVertical)}
          </Checkbox>
        </div>
      </div>
    );
  }

  renderFontStyle = () => {
    const { intl } = this.props;
    return (
      <div className={styles.fontStyle}>
        <div className={styles.basicStyle}>
          <div>
            <span>
              {intl.formatMessage(messages.Font)}
              :
            </span>
            <Input
              value={this.state.fontFamily}
              onChange={this.changeFontStyle.bind(this, 'fontFamily')}
            />
            <List
              bordered
              className={styles.fontFormat}
              itemLayout="horizontal"
              dataSource={fontFamilyItems}
              renderItem={(item) => (
                <Item
                  onClick={this.setFontStyle.bind(this, { fontFamily: item })}
                  className={this.state.fontFamily === item ? styles.selectedFontType : ''}
                >
                  {item}
                </Item>
              )}
            />
          </div>
          <div className={styles.flexColumn}>
            <span>
              {intl.formatMessage(messages.FontStyle)}
              :
            </span>
            <Input
              value={this.state.fontStyleWeight && this.state.fontStyleWeight.text}
              onChange={this.changeFontStyle.bind(this, 'fontStyleWeight')}
            />
            <List
              bordered
              className={styles.fontFormat}
              itemLayout="horizontal"
              dataSource={this.fontStyleWeightItems}
              renderItem={(item) => (
                <Item
                  onClick={this.setFontStyle.bind(this, { fontStyleWeight: item })}
                  className={this.state.fontStyleWeight.text === item.text ? styles.selectedFontType : ''}
                >
                  {item.text}
                </Item>
              )}
            />
          </div>
          <div className={styles.flexColumn}>
            <span>
              {intl.formatMessage(messages.FontSize)}
              :
            </span>
            <Input
              value={this.state.fontSize}
              onChange={this.changeFontStyle.bind(this, 'fontSize')}
            />
            <List
              bordered
              className={styles.fontFormat}
              itemLayout="horizontal"
              dataSource={fontSizeItems}
              renderItem={(item) => (
                <Item
                  onClick={this.setFontStyle.bind(this, { fontSize: item })}
                  className={this.state.fontSize === item ? styles.selectedFontType : ''}
                >
                  {item}
                </Item>
              )}
            />
          </div>
        </div>
        <div>
          <span>
            {intl.formatMessage(messages.Color)}
            ：
          </span>
          <ColorPicker
            ref={(color) => { this.fontColor = color; }}
            name="fontColor"
            borderRadius={0}
            colorWidth={160}
            colorHeight={20}
            left={0}
            edit={this.setFontColor}
            defaultValue={this.state.fontColor}
          />
        </div>
        <div className={styles.flexRow}>
          <div className={styles.fontExample} style={{ marginRight: 30 }}>
            <span>
              {intl.formatMessage(messages.SpecialEffect)}
              :
            </span>
            <div>
              <Checkbox
                onChange={this.checkBoxEvent.bind(this, 'lineThrough')}
                checked={this.state.lineThrough}
              >
                {intl.formatMessage(messages.DelLine)}
              </Checkbox>
              <Checkbox
                onChange={this.checkBoxEvent.bind(this, 'underline')}
                checked={this.state.underline}
              >
                {intl.formatMessage(messages.Underline)}
              </Checkbox>
            </div>
          </div>
          <div className={styles.fontExample}>
            <span>
              {intl.formatMessage(messages.Preview)}
              :
            </span>
            <div style={{ alignItems: 'center', width: 300, overflow: 'hidden' }}>
              <span
                style={{
                  fontSize: Number(this.state.fontSize),
                  fontFamily: this.state.fontFamily,
                  color: this.state.fontColor,
                  textDecoration: this.state.lineThrough ? 'line-through' : 'none',
                  ...(this.state.fontStyleWeight && this.state.fontStyleWeight.style),
                  borderBottom: `${this.state.underline ? '1px' : '0px'} solid ${this.state.fontColor}`
                }}
              >
                supOS
                {intl.formatMessage(messages.Report)}
              </span>
            </div>
          </div>
        </div>
      </div>
    );
  }

  renderTreeNodes = (data) => {
    return data.map((item) => {
      if (_.get(item, 'userData.list')) {
        return (
          <TreeNode
            key={item.username || item.name}
            title={item.username || item.showName}
            isLeaf={!item.showName}
            selectable={!!item.showName}
            dataRef={item}
          >
            {this.renderTreeNodes(item.userData.list)}
          </TreeNode>
        );
      }
      return (
        <TreeNode
          title={item.showName || item.username}
          key={item.name || `${item.username}-leaf`}
          dataRef={item}
          isLeaf={!item.showName}
          selectable={!!item.showName}
        />
      );
    });
  }

  onExpand = (expandedKeys) => {
    this.setState({
      expandedKeys
    });
  }

  queryMore = () => {
    const { roleData } = this.state;
    const { list = [], pagination = {} } = roleData;
    if (list.length < pagination.total) {
      this.getRoleList({ roleName: this.state.searchName, pageIndex: (pagination.current || 0) + 1 });
    }
  }

  renderAuthorityList = () => {
    const { roleData = {}, selectedKeys, expandedKeys } = this.state;
    if (_.get(roleData, 'list.length')) {
      return (
        <DirectoryTree
          multiple
          loadData={this.onLoadData}
          defaultExpandAll={false}
          onSelect={this.treeSelect}
          selectedKeys={selectedKeys}
          expandedKeys={expandedKeys}
          onExpand={this.onExpand}
        >
          {this.renderTreeNodes(roleData.list)}
        </DirectoryTree>
      );
    } else {
      return (
        <Tree />
      );
    }
  }

  renderAuthority = () => {
    const { intl } = this.props;
    const radioStyle = {
      display: 'block',
      height: 30
    };
    const { roleData = {}, selectedKeys, authority } = this.state;
    const { list = [], pagination = {} } = roleData;
    return (
      <div className={styles.authority}>
        <div className={styles.userList}>
          <div style={{ padding: 8 }}>
            <Search size="small" onSearch={this.searchItem} />
          </div>
          <div className={styles.userListTree}>
            <InfiniteScroll
              initialLoad={false}
              pageStart={1}
              loadMore={this.queryMore}
              hasMore={list.length < pagination.total}
              useWindow={false}
            >
              {this.renderAuthorityList()}
            </InfiniteScroll>
          </div>
        </div>
        <div className={styles.MSPermissionsClass}>
          <div className={styles.MSPermissionsClassHaed}>{intl.formatMessage(messages.AuthoritySet)}</div>
          <div style={{ margin: '10px 0 0 20px' }}>
            {
              !!selectedKeys.length && (
                <RadioGroup onChange={this.authorityChange} value={authority[selectedKeys[0]] || 2}>
                  <Radio style={radioStyle} value={1}>{intl.formatMessage(messages.WriteRead)}</Radio>
                  <Radio style={radioStyle} value={2}>{intl.formatMessage(messages.Read)}</Radio>
                  <Radio style={radioStyle} value={3}>{intl.formatMessage(messages.Hidden)}</Radio>
                </RadioGroup>
              )
            }
          </div>
        </div>
      </div>
    );
  }

  render() {
    const { intl } = this.props;
    return (
      <div>
        <Modal
          destroyOnClose
          visible
          width="600px"
          bodyStyle={{ height: '500px' }}
          title={intl.formatMessage(messages.CellFormats)}
          onOk={this.handleOk}
          onCancel={this.handleCancel}
          okText={intl.formatMessage(messages.ok)}
          cancelText={intl.formatMessage(messages.cancel)}
        >
          <Tabs type="card" className={styles.reportCellSetting} style={{ overflow: 'visible' }} onChange={this.changeTab}>
            <TabPane tab={intl.formatMessage(messages.Number)} key={intl.formatMessage(messages.Number)}>
              {this.renderNumber()}
              <div style={{ marginTop: 20 }}>{this.state.numberFormatDesp}</div>
            </TabPane>
            <TabPane tab={intl.formatMessage(messages.BorderSet)} key={intl.formatMessage(messages.BorderSet)}>
              {this.renderBorder()}
            </TabPane>
            <TabPane tab={intl.formatMessage(messages.Picture)} key={intl.formatMessage(messages.Picture)}>
              {this.renderBackground()}
            </TabPane>
            <TabPane tab={intl.formatMessage(messages.Type)} key={intl.formatMessage(messages.Type)}>
              {this.renderType()}
            </TabPane>
            <TabPane tab={intl.formatMessage(messages.Align)} key={intl.formatMessage(messages.Align)}>
              {this.renderAlign()}
            </TabPane>
            <TabPane tab={intl.formatMessage(messages.Font)} key={intl.formatMessage(messages.Font)}>
              {this.renderFontStyle()}
            </TabPane>
            <TabPane tab={intl.formatMessage(messages.Authority)} key={intl.formatMessage(messages.Authority)}>
              {this.renderAuthority()}
            </TabPane>
          </Tabs>
        </Modal>
      </div>
    );
  }
}
