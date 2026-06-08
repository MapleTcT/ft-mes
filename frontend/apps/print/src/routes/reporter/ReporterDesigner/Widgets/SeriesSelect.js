// SeriesSelect
import React, { PureComponent } from 'react';
import { Select } from 'sup-ui';

const { Option } = Select;

export default class SelectComp extends PureComponent {
  constructor(props) {
    super(props);
    this.state = {
      value: 0,
      optData: []
    };
  }

  componentWillMount() {
    this.setInfo(this.props);
  }

  // componentWillReceiveProps(nextProps) {
  //   this.setInfo(nextProps);
  // }

  setInfo = (props) => {
    const { editChart, selectedSeriesIndex } = props;
    const optData = editChart.series().AllSers.map((item) => ({
      name: item.Name === '' || item.Name === null ? `series${item.Index + 1}` : item.Name,
      index: item.Index
    }));
    this.setState({
      optData,
      value: selectedSeriesIndex
    });
  }

  selectChange = (value) => {
    // aaa.split('!')[1].split(':').map(item => item.split('$'));
    this.setState({ value });
    this.props.edit(value);
  }

  render() {
    const { value, optData } = this.state;
    const { intl } = this.props;
    return (
      <Select onChange={this.selectChange} value={value} size="small" style={{ width: '100%' }}>
        {
          optData.map((item) => (
            <Option value={item.index} key={item.index}>
              {Object.prototype.toString.call(item.name) !== '[object Object]' ? item.name : intl.formatMessage(item.name)}
            </Option>
          ))
        }
      </Select>
    );
  }
}
