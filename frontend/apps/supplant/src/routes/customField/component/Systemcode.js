import React from 'react';
import { injectIntl } from 'react-intl';
import { Select } from 'sup-ui';
import { getSystemcode } from 'root/services/customProperty';

const { OptGroup, Option } = Select;

class Systemcode extends React.Component {
  constructor(props) {
    super(props);
    this.inited = false;
    this.state = {
      options: null
    };
  }

  componentDidMount() {
    const { moduleCode } = this.props;
    getSystemcode({
      moduleCode
    }).then(({ data }) => {
      const { list: options } = data;
      this.setState({
        options
      });
    });
  }

  renderOptions() {
    const { options } = this.state;
    return (options || []).map((option) => {
      const { list, name } = option;

      return (
        <OptGroup label={name} key={name}>
          {list.map((l) => {
            return (
              <Option key={l.code} value={l.code}>
                {l.name}
              </Option>
            );
          })}
        </OptGroup>
      );
    });
  }

  getDisableState() {
    return !this.state.options;
  }

  handleSelectChange = (code) => {
    this.props.handleSelectChange(code);
  };

  render() {
    const { value } = this.props;
    return (
      <Select
        disabled={this.getDisableState()}
        defaultValue={value}
        style={{ width: '100%' }}
        onChange={this.handleSelectChange}
      >
        {this.renderOptions()}
      </Select>
    );
  }
}

export default injectIntl(Systemcode, { forwardRef: true });
