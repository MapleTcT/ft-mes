import React from 'react';
import { injectIntl } from 'react-intl';
import { Checkbox } from 'sup-ui';
import { SupReference } from 'sup-rc-reference';
import defaultMessages from './messages.js';
import { getRefCompanyConfig } from '../../utils/index.js';

@injectIntl
class DepartLower extends React.PureComponent {
  constructor(props) {
    super(props);
    this.state = { isLower: true };
  }

  handleRefChange = (value) => {
    const { onChange } = this.props;
    const { isLower } = this.state;
    const trans = value.map((item) => {
      return { ...item, isLower };
    });
    onChange(trans);
  };

  handleLowerChange = (e) => {
    const { checked } = e.target;
    const { value, onChange } = this.props;
    if (value && value.length > 0) {
      const trans = value.map((item) => {
        return { ...item, isLower: checked };
      });
      onChange(trans);
    }
    this.setState({ isLower: checked });
  };

  render() {
    const { value, intl } = this.props;
    const { isLower } = this.state;
    const refProps = {
      referenceView: {
        title: intl.formatMessage(defaultMessages.department),
        type: 'department',
        companyConfig: getRefCompanyConfig()
      },
      value,
      suffix: <i className="icon-search" />,
      onChange: this.handleRefChange
    };
    return (
      <div className="sup-comp-dlower">
        <span className="dlower-ref">
          <SupReference {...refProps} />
        </span>
        <span className="dlower-ckb">
          <Checkbox onChange={this.handleLowerChange} checked={isLower}>
            {intl.formatMessage(defaultMessages.lower)}
          </Checkbox>
        </span>
      </div>
    );
  }
}
export default DepartLower;
