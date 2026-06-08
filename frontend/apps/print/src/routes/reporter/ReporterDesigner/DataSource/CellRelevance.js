import React, { Component } from 'react';
import * as _ from 'lodash';
import { Row, Col, Input, InputNumber, Select, message } from 'sup-ui';
import * as Util from '../ReportUtil.js';
import styles from './DataSource.less';
import messages from '../messages';

const { Option } = Select;

export default class CellRelevance extends Component {
  constructor(props) {
    super(props);
    this.state = this.getState(props);
  }

  componentWillReceiveProps(nextProps) {
    const { value, selections = [] } = nextProps;
    if (value === undefined || !selections.length) return;
    const state = this.getState(nextProps);
    if (state) this.setState({ ...state });
  }

  getState = (props) => {
    const { value = '' } = props;
    const state = {};
    const { row, col, action } = this.state || {};
    const [, colStr, rowStr] = value.match(/^([A-Z]*)(\d*)$/);
    if (colStr && rowStr) {
      state.action = 'add';
      state.col = colStr;
      state.row = Number(rowStr);
    } else {
      state.action = 'delete';
      state.col = '';
      state.row = '';
    }

    // 目标值为默认值
    const { newRow, newCol, id } = this.getDefaultRowCol(props);
    if (id && id === value) state.action = 'default';
    // 目标值为当前值时，不修改
    if ((state.action === 'add' && row === Number(rowStr) && col === colStr && action === state.action)
      || (state.action === 'default' && row === Number(newRow) && col === newCol)) {
      return false;
    }

    return state;
  }

  getDefaultRowCol = (props) => {
    const { selections, tagName } = props;
    let [newRow, newCol, id] = ['', '', ''];
    if (selections && selections.length) {
      const { row, col } = selections[0];
      switch (tagName) {
        case 'paneSettingConf': {
          if (col) {
            newCol = Util.getColString(col);
            newRow = String(row + 1);
            id = Util.getCellPositionString(row + 1, col);
          }
          break;
        }
        case 'paneSettingUpConf': {
          if (row) {
            newCol = Util.getColString(col + 1);
            newRow = String(row);
            id = Util.getCellPositionString(row, col + 1);
          }
          break;
        }
        default: break;
      }
    }
    return { newRow, newCol, id };
  }

  rowChange = (row) => {
    const { intl } = this.props;
    if (row === '' || (_.isNumber(row) && row > 0)) {
      this.setState({ row }, () => {
        this.saveValue();
      });
    } else {
      message.error(intl.formatMessage(messages.EnterNumber));
    }
  }

  colChange = (e) => {
    const { intl } = this.props;
    const col = e.target.value.trim().toUpperCase();
    if (/^[A-Z]*$/.test(col)) {
      this.setState({ col }, () => {
        this.saveValue();
      });
    } else {
      message.error(intl.formatMessage(messages.EnterLetter));
    }
  }

  selectChange = (e) => {
    this.setState({
      action: e
    }, () => {
      if (e !== 'add') this.saveValue(true);
    });
  }

  saveValue = (isActionSelect = false) => {
    const { col, row, action } = this.state;
    const { tagName } = this.props;
    const { newRow, newCol } = this.getDefaultRowCol(this.props);
    switch (action) {
      case 'add': {
        if (row !== '' && col !== '') this.props.saveValue({ tagName, row, col });
        break;
      }
      case 'default': {
        if (newRow !== '' && newCol !== '') this.props.saveValue({ tagName, row: newRow, col: newCol });
        break;
      }
      case 'delete': {
        this.props.saveValue({ tagName, action });
        if (isActionSelect) {
          this.setState({ row: '', col: '' });
        }
        break;
      }
      default: break;
    }
  }


  renderPosConfig = () => {
    const { row, col } = this.state;
    const { intl } = this.props;
    return (
      <div className={styles['row-col-choose']}>
        <Row type="flex" justify="start">
          <Col span={9} offset={6}>
            <label style={{ fontSize: 12 }}>
              {intl.formatMessage(messages.Col)}
              ：
              <Input
                className={styles['input-str']}
                onChange={this.colChange}
                value={col}
              />
            </label>
          </Col>
          <Col span={9}>
            <label style={{ fontSize: 12 }}>
              {intl.formatMessage(messages.Row)}
              ：
              <InputNumber
                className={styles['input-number']}
                onChange={this.rowChange}
                value={row}
                min={0}
              />
            </label>
          </Col>
        </Row>
      </div>
    );
  }

  renderDefault() {
    const { tagName, intl } = this.props;
    if (~tagName.indexOf('pane')) {
      return (<Option value="default">{intl.formatMessage(messages.default)}</Option>);
    } else {
      return null;
    }
  }

  render() {
    const { label, intl } = this.props;
    const { action } = this.state;
    return (
      <div>
        <Row type="flex" align="middle" style={{ height: 45 }}>
          <Col span={7}>
            {label}
            ：
          </Col>
          <Col span={17}>
            <Select onChange={this.selectChange} value={action} style={{ width: '89%' }}>
              <Option value="delete">{intl.formatMessage(messages.none)}</Option>
              {this.renderDefault()}
              <Option value="add">{intl.formatMessage(messages.SelfDefined)}</Option>
            </Select>
          </Col>
        </Row>
        {action === 'add' && (this.renderPosConfig())}
      </div>
    );
  }
}
