// ReportChartSet
import React from 'react';
import { Collapse, Row, Col, message } from 'sup-ui';
import * as _ from 'lodash';
import classnames from 'classnames';
import GC from '@grapecity/spread-sheets';
// import GC from 'root/dependencies/spreadjs/gc.spread.sheets.all.12.0.7.min.js';
import BaseChartSet from './BaseChartSet';
import styles from './Reporter.less';
import TextInput from './Widgets/TextInput';
import SelectComp from './Widgets/SelectComp';
import NumberInput from './Widgets/NumberInput';
import ColorSelect from './Widgets/ColorSelect';
import SelfCheckBox from './Widgets/SelfCheckBox';
import SelfButton from './Widgets/SelfButton';
import SeriesSelect from './Widgets/SeriesSelect';

const { Panel } = Collapse;
const spreadNS = GC.Spread.Sheets;

export default class ReportChartSet extends BaseChartSet {
  constructor(props) {
    super(props);
    this.renderGroup = null;
    this.state = {
      selectedSeriesIndex: 0,
      selectedAxesType: 'primaryCategory',
      legend: {
        position: null
      },
      axes: {
        primaryCategory: {
          title: {
            color: 'rgba(89,89,89,1)',
            fontSize: 13.33,
            fontFamily: 'Calibri Light'
          },
          majorGridLine: {
            width: 1,
            color: 'rgba(89,89,89,1)'
          },
          minorGridLine: {
            width: 1,
            color: 'rgba(89,89,89,1)'
          }
        },
        primaryValue: {
          title: {
            color: 'rgba(89,89,89,1)',
            fontSize: 13.33,
            fontFamily: 'Calibri Light'
          },
          majorGridLine: {
            width: 1,
            color: 'rgba(89,89,89,1)'
          },
          minorGridLine: {
            width: 1,
            color: 'rgba(89,89,89,1)'
          }
        },
        secondaryCategory: {
          title: {
            color: 'rgba(89,89,89,1)',
            fontSize: 13.33,
            fontFamily: 'Calibri Light'
          },
          majorGridLine: {
            width: 1,
            color: 'rgba(89,89,89,1)'
          },
          minorGridLine: {
            width: 1,
            color: 'rgba(89,89,89,1)'
          }
        },
        secondaryValue: {
          title: {
            color: 'rgba(89,89,89,1)',
            fontSize: 13.33,
            fontFamily: 'Calibri Light'
          },
          majorGridLine: {
            width: 1,
            color: 'rgba(89,89,89,1)'
          },
          minorGridLine: {
            width: 1,
            color: 'rgba(89,89,89,1)'
          }
        }
      }
    };
  }

  componentWillMount() {
    this.isAdd = true;
    this.renderConfigClassify(this.props);
    this.registerCommands(this.props);
  }

  componentDidUpdate() {
    this.renderConfigClassify(this.props);
  }

  registerCommands = (props) => {
    const chartJson = props.editChart.toJSON();
    const type = chartJson.name.split('_')[0];
    const configs = this.dynamicImportConfig(type);
    const cmdRegisterArr = ['updataConfig'];
    if (!configs) return;
    configs.then((res) => {
      _.map(res.default, (item) => {
        _.map(item.configs, (cof) => {
          if (cof.funcName) cmdRegisterArr.push(`set${cof.funcName}`);
        });
      });
      _.map(cmdRegisterArr, (cmd) => {
        this.props.spread.commandManager().register(cmd, {
          canUndo: true,
          execute: (context, options, isUndo) => {
            const commands = spreadNS.Commands;
            if (isUndo) {
              commands.undoTransaction(context, options);
              this.forceUpdate();
              return true;
            } else {
              commands.startTransaction(context, options);
              const params = _.merge({}, options);
              delete params.cmd;
              delete params.sheetName;
              this[cmd](params);
              commands.endTransaction(context, options);
              return true;
            }
          }
        });
      });
    });
  }

  basicOperate = ({ opt, type, options = {} }) => {
    const sheet = this.props.spread.getActiveSheet();
    if (type) {
      sheet.options.clipBoardOptions = spreadNS.ClipboardPasteOptions[type];
    }

    if (!options.selections) options.selections = sheet.getSelections();
    this.props.spread.options.allowUndo = true;
    this.props.spread.commandManager().execute({ cmd: opt, sheetName: sheet.name(), ...options });
  }

  dynamicImportConfig = (type) => {
    return import(`./ReportChartConfig/${type}Config.js`);
  };

  getValue = (fatherEvent, itemEvent) => {
    // const chart = this.props.editChart;
    const sheet = this.props.spread.getActiveSheet();
    let chart = null;
    sheet.charts.all().forEach((item) => {
      if (item.isSelected()) {
        chart = item;
      }
    });
    if (!chart) return;
    let value = null;
    if (fatherEvent === 'axes') {
      const { selectedAxesType } = this.state;
      value = _.get(chart[fatherEvent](), `${selectedAxesType}.${itemEvent}`);
    } else {
      value = _.get(chart[fatherEvent](), itemEvent);
    }
    return value;
  }

  updataConfig = ({ fatherEvent, event, value }) => {
    const obj = {};
    if (fatherEvent === 'axes') {
      const { selectedAxesType } = this.state;
      _.set(obj, `${selectedAxesType}.${event}`, value);
    } else {
      _.set(obj, event, value);
    }
    this.props.editChart[fatherEvent](obj);
    this.forceUpdate();
  }

  getColorByThemeColor = (themeColor) => {
    if (themeColor.indexOf('accent') >= 0) {
      const theme = this.props.spread.getActiveSheet().currentTheme();
      return theme.getColor(themeColor);
    }
    return themeColor;
  }

  setAxesType = ({ value }) => {
    if (this.props.editChart.axes()[value]) {
      this.setState({
        selectedAxesType: value
      });
    } else {
      message.info('请设置合适的坐标轴组');
      this.forceUpdate();
    }
  }

  getAxesType = () => {
    return this.state.selectedAxesType;
  }

  setSeriesSelect = (value) => {
    this.setState({
      selectedSeriesIndex: value
    });
  }

  callFunction = (funcName) => {
    const chart = this.props.editChart;
    if (chart) chart[funcName]();
    this.props.reportHasChanged();
  }

  inputType = (config, props) => {
    const { selectedSeriesIndex } = this.state;
    const parameters = {
      config,
      selectedSeriesIndex,
      updataConfig: this.updataConfig,
      getValue: this.getValue,
      callFunction: this.callFunction,
      edit: this[`set${config.funcName}`],
      fetch: this[`get${config.funcName}`],
      basicOperate: this.basicOperate,
      ...props
    };
    switch (config.inputType) {
      case 'Input':
        return <TextInput {...parameters} />;
      case 'Select':
        return <SelectComp {...parameters} />;
      case 'InputNumber':
        return <NumberInput {...parameters} />;
      case 'ColorSelect':
        return <ColorSelect {...parameters} />;
      case 'CheckBox':
        return <SelfCheckBox {...parameters} />;
      case 'Button':
        return <SelfButton {...parameters} />;
      case 'SeriesSelect':
        return <SeriesSelect {...parameters} />;
      default:
        break;
    }
  }

  renderConfigClassify = (props) => {
    const { intl } = this.props;
    const chartJson = props.editChart.toJSON();
    const type = chartJson.name.split('_')[0];
    const configs = this.dynamicImportConfig(type);
    if (!configs) return;
    configs.then((res) => {
      const renderGroup = res.default.map((item) => {
        const title = Object.prototype.toString.call(item.title) !== '[object Object]' ? item.title : intl.formatMessage(item.title);
        return (
          <Panel header={title} key={title}>
            {
              item.configs.map((config) => {
                const name = Object.prototype.toString.call(config.name) !== '[object Object]' ? config.name : intl.formatMessage(config.name);
                return (
                  <Row style={{ marginBottom: 12 }} key={`${name}${config.funcName}${config.event}`}>
                    <Col span={6}>
                      {name}
                    </Col>
                    <Col span={18}>
                      {this.inputType(config, props)}
                    </Col>
                  </Row>
                );
              })
            }
          </Panel>
        );
      });
      this.renderGroup = renderGroup;
      if (this.isAdd) {
        this.isAdd = false;
        this.forceUpdate();
      }
    });
  }

  render() {
    return (
      <div className={styles['report-chart-set-box']} >
        <Collapse>
          {this.renderGroup}
        </Collapse>
      </div>
    );
  }
}
