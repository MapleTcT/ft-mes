import React, { Component } from 'react';
import { Row, Col, Select, Input } from 'sup-ui';
import * as _ from 'lodash';
import styles from './DataSource.less';
import messages from '../messages';

const { Option } = Select;

export default class OperateFormula extends Component {
  constructor(props) {
    super(props);
    this.state = this.getStateObject(props);
  }

  componentWillReceiveProps(nextProp) {
    if (nextProp.formula !== this.props.formula || nextProp.textVal !== this.props.textVal) {
      this.setState(this.getStateObject(nextProp));
    }
  }

  getStateObject = (props) => {
    const { formula, textVal } = props;
    const [, dataType, params] = (formula && formula.match(/(OPT)\((.*)\)$/)) || [];
    const [dataSource, optType, optScope, text, tableName] = (params && params.replace(/"/g, '').split(',')) || [];
    const obj = {
      dataSource: 'null',
      optType: 'null',
      optScope: 'row',
      text: textVal,
      tableName: ''
    };
    if (dataType) {
      if (dataSource && dataSource.trim()) obj.dataSource = dataSource.trim();
      if (optType && optType.trim()) obj.optType = optType.trim();
      if (optScope && optScope.trim()) obj.optScope = optScope.trim();
      if (tableName && tableName.trim()) obj.tableName = tableName.trim();
      if (text && text.trim()) obj.text = text.trim();
    }
    return obj;
  }

  onTextStateChange = (type, e) => {
    this.setState({ [type]: e.target.value });
  }

  onStateChange = (type, value) => {
    const obj = { [type]: value };
    if (type === 'optType' && value === 'delete') {
      obj.optScope = 'row';
    }
    this.setState(obj, () => {
      this.updateFormula();
    });
  }

  updateFormula = () => {
    const formula = this.generateFormula();
    if (formula) {
      this.props.basicOperate({
        opt: 'generateFormula',
        options: {
          formula,
          selections: this.props.selections || null
        }
      });
    }
  }

  generateFormula = () => {
    const { dataSource, optType, optScope, text, tableName } = this.state;
    const source = dataSource === 'null' ? '' : dataSource;
    const type = optType === 'null' ? '' : optType;
    const textVal = _.isNull(text) ? '' : text;
    return `=OPT("${source}", "${type}", "${optScope}", "${textVal}", "${tableName}")`;
  }

  render() {
    const { intl, sqlInfo } = this.props;
    const { dataSource, optType, optScope, text, tableName } = this.state;
    return (
      <div className={styles.operateFormula}>
        <Row type="flex" justify="space-around" align="middle">
          <Col span={9}>
            {intl.formatMessage(messages.tableName)}
            :
          </Col>
          <Col span={15}>
            <Input
              value={tableName}
              disabled={optType === 'null'}
              onChange={this.onTextStateChange.bind(this, 'tableName')}
              onBlur={this.updateFormula}
            />
          </Col>
        </Row>
        <Row type="flex" justify="space-around" align="middle">
          <Col span={9}>
            {intl.formatMessage(messages.DataSource)}
            :
          </Col>
          <Col span={15}>
            <Select
              value={dataSource}
              onChange={this.onStateChange.bind(this, 'dataSource')}
            >
              <Option value="null">{intl.formatMessage(messages.Null)}</Option>
              {_.map(_.keys(sqlInfo), (name) => <Option value={name} key={name}>{name}</Option>)}
            </Select>
          </Col>
        </Row>
        <Row type="flex" justify="space-around" align="middle">
          <Col span={9}>
            {intl.formatMessage(messages.operateType)}
            :
          </Col>
          <Col span={15}>
            <Select
              value={optType}
              onChange={this.onStateChange.bind(this, 'optType')}
            >
              <Option value="null">{intl.formatMessage(messages.Null)}</Option>
              <Option value="update">{intl.formatMessage(messages.update)}</Option>
              <Option value="delete">{intl.formatMessage(messages.delete)}</Option>
            </Select>
          </Col>
        </Row>
        <Row type="flex" justify="space-around" align="middle">
          <Col span={9}>
            {intl.formatMessage(messages.operateScope)}
            :
          </Col>
          <Col span={15}>
            <Select
              value={optScope}
              disabled={optType !== 'update'}
              onChange={this.onStateChange.bind(this, 'optScope')}
            >
              <Option value="row">{intl.formatMessage(messages.Row)}</Option>
              <Option value="all">{intl.formatMessage(messages.All)}</Option>
            </Select>
          </Col>
        </Row>
        <Row type="flex" justify="space-around" align="middle">
          <Col span={9}>
            {intl.formatMessage(messages.Text)}
            :
          </Col>
          <Col span={15}>
            <Input
              value={text}
              disabled={optType === 'null'}
              onChange={this.onTextStateChange.bind(this, 'text')}
              onBlur={this.updateFormula}
            />
          </Col>
        </Row>
      </div>
    );
  }
}
