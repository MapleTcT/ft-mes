import React from 'react';
import Moment from 'moment';
import { Form, Input } from 'sup-ui';
import widgetsMap from './widgets';
import style from '../style.less';

const ruleMap = {
  isRequire: 'required',
  length: 'len', // 长度相等
  min: 'min', // 最小长度
  max: 'max', // 最大长度
  rex: 'pattern', // 正则
  msg: 'message',
  // TODO: 是否直接使用inputnumber组件
  isNumber: {
    pattern: /^[\d]+$/
  }
};

const formatValueToCmpValue = (config) => {
  // type后台api返回为字符类型
  const { type } = config;
  const { typeConfig = {} } = config;
  let { value = [] } = config;
  if (value === null) {
    value = [];
  }
  // 提前过滤不存在的默认值
  value = value.filter((d) => d !== null && d !== '' && d !== undefined);
  const [firstValue] = value;
  // 非多选情况不需要使用数组
  if (type !== 1 && !typeConfig.isMore) {
    value = firstValue;
  }
  // 时间转换
  if (type === 4) {
    value = value ? Moment(value) : null;
  }
  return value;
};

const formatFieldOptions = (config) => {
  const options = {
    initialValue: formatValueToCmpValue(config)
  };

  const { verify = [] } = config;

  options.rules = verify.map((rule) => {
    let formatRule = {};
    for (const key in rule) {
      if ({}.hasOwnProperty.call(rule, key)) {
        const val = rule[key];
        const ruleProp = ruleMap[key];
        if (typeof ruleProp === 'object' && val) {
          formatRule = { ...formatRule, ...ruleProp };
        } else {
          formatRule[ruleProp] = val;
        }
        if (process.env.NODE_ENV === 'development') {
          // TODO for dev only
          if (key === 'rex') {
            formatRule.pattern = /^[\d]+$/;
          }
        }
      }
    }

    return formatRule;
  });
  return options;
};

export default class FormField extends React.Component {
  render() {
    const {
      form: { getFieldDecorator },
      field
    } = this.props;
    const { configId, name, type } = field;
    const widget = widgetsMap[type] || {};

    const Element = widget.widget || Input;
    let widgetProp = {};
    if (widget.fn) {
      widgetProp = widget.fn(field.typeConfig);
    }

    // TODO: 注入属性
    // 初始值
    // 验证规则
    // 组件配置项

    // 帮助提示配置
    let tip = '';
    if (field.typeConfig && field.typeConfig.tip) {
      tip = field.typeConfig.tip;
    }

    return (
      <>
        <Form.Item label={name}>
          {getFieldDecorator(
            configId,
            formatFieldOptions(field)
          )(<Element {...widgetProp} />)}
          {tip ? <div className={style.tip}>{tip}</div> : null}
        </Form.Item>
      </>
    );
  }
}
