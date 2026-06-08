import React from 'react';
import { Input, Select, Radio, Checkbox, DatePicker } from 'sup-ui';

const { TextArea } = Input;
const { Option } = Select;

const widgetsMap = {
  // 输入框
  0: {
    widget: Input
  },
  // 多选checkbox
  1: {
    widget: Checkbox.Group,
    fn: (config) => {
      const { optionalValue } = config;
      return {
        options: optionalValue
      };
    }
  },
  // 单选radio
  2: {
    widget: Radio.Group,
    fn: (config) => {
      const { optionalValue } = config;
      return {
        options: optionalValue
      };
    }
  },
  // 下拉
  3: {
    widget: Select,
    fn: (config) => {
      const { optionalValue, isMore } = config;
      const realOptionValue = [...optionalValue];
      if (!isMore) {
        // 单选手动添加空选项
        realOptionValue.unshift({ value: '', label: '' });
      }
      return {
        showArrow: true,
        mode: isMore ? 'multiple' : '', // 区分多选单选
        children: realOptionValue.map((op, i) => {
          const { value, label } = op;
          return (
            <Option key={i.toString(36)} value={value}>
              {label}
            </Option>
          );
        })
      };
    }
  },
  // 时间
  4: {
    widget: DatePicker,
    fn: (config) => {
      let { timeFormat } = config;
      timeFormat = timeFormat.trim();
      let showTime = false;
      // FIXME 时间格式考虑传入配置项
      const timeFormatArr = timeFormat.split(' ');
      if (timeFormatArr.length > 1) {
        showTime = { format: timeFormatArr[1] };
      }
      return {
        format: timeFormat,
        showTime
      };
    }
  },
  6: {
    widget: TextArea,
    fn: () => {
      return {
        rows: 3
      };
    }
  }
};

export default widgetsMap;
