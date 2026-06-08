import React from 'react';
import PropTypes from 'prop-types';
import { injectIntl } from 'react-intl';
import './index.less';
import defaultMessages from './messages.js';

@injectIntl
class SortGroup extends React.PureComponent {
  constructor(props) {
    super(props);
    const { intl } = props;
    this.sortData = [
      {
        name: intl.formatMessage(defaultMessages.byGroup),
        type: 'category',
        sorts: [
          {
            name: intl.formatMessage(defaultMessages.activity),
            key: 'task'
          },
          {
            name: intl.formatMessage(defaultMessages.process),
            key: 'process'
          }
        ]
      },
      {
        name: intl.formatMessage(defaultMessages.byTime),
        type: 'timeType',
        sorts: [
          {
            name: intl.formatMessage(defaultMessages.all),
            key: 'all'
          },
          {
            name: intl.formatMessage(defaultMessages.today),
            key: 'TODAY'
          },
          {
            name: intl.formatMessage(defaultMessages.threeDays),
            key: 'THREE_DAYS'
          },
          {
            name: intl.formatMessage(defaultMessages.sevenDays),
            key: 'SEVEN_DAYS'
          },
          {
            name: intl.formatMessage(defaultMessages.thisMonth),
            key: 'THIS_MONTH'
          },
          {
            name: intl.formatMessage(defaultMessages.lastMonth),
            key: 'LAST_MONTH'
          },
          {
            name: intl.formatMessage(defaultMessages.inThreeMonths),
            key: 'IN_THREE_MONTHS'
          },
          {
            name: intl.formatMessage(defaultMessages.outOfThreeMonths),
            key: 'OUT_OF_THREE_MONTHS'
          }
        ]
      },
      {
        name: intl.formatMessage(defaultMessages.byType),
        type: 'taskType',
        sorts: [
          {
            name: intl.formatMessage(defaultMessages.all),
            key: 'all'
          },
          {
            name: intl.formatMessage(defaultMessages.normal),
            key: 0
          },
          {
            name: intl.formatMessage(defaultMessages.assign),
            key: 2
          }
        ]
      }
    ];
  }

  handleClick = (val, type) => {
    const { value = {}, onChange } = this.props;
    value[type] = val;
    onChange({ ...value });
  };

  render() {
    const { value } = this.props;
    return (
      <div className="sup-sortGroup-wrap">
        {this.sortData.map((item) => (
          <div className="sort-box" key={item.type}>
            <b className="sb-title">{`${item.name}：`}</b>
            <ul className="sb-list">
              {item.sorts.map((obj) => (
                <span
                  className={`${value[item.type] === obj.key ? 'current' : ''}`}
                  key={obj.key}
                  onClick={() => this.handleClick(obj.key, item.type)}
                >
                  {obj.name}
                </span>
              ))}
            </ul>
          </div>
        ))}
      </div>
    );
  }
}

SortGroup.propTypes = {
  onChange: PropTypes.func,
  value: PropTypes.oneOfType([PropTypes.object])
};

SortGroup.defaultProps = {
  onChange: () => {},
  value: {}
};
export default SortGroup;
