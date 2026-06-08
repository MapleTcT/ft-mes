import React from 'react';
import { injectIntl } from 'react-intl';
import './index.less';
import { getPendingNotice } from '../../services/pending.js';
import { IS_DEV } from '../../utils/index.js';
import defaultMessages from './messages.js';

@injectIntl
export default class PendingNotice extends React.PureComponent {
  constructor(props) {
    super(props);
    const { intl } = props;
    this.noticeSort = [
      { key: 'today', label: intl.formatMessage(defaultMessages.today) },
      { key: 'week', label: intl.formatMessage(defaultMessages.week) },
      { key: 'total', label: intl.formatMessage(defaultMessages.total) }
    ];
  }

  state = {};

  componentDidMount() {
    getPendingNotice().then((res) => {
      if (res.data) {
        this.setState({ noticeData: res.data.data });
      }
    });
  }

  handleClick = (key) => {
    const { intl } = this.props;
    try {
      window.parent.CUI.loadPage({
        menuName: intl.formatMessage(defaultMessages.pending),
        code: 'mypendingWorkbench',
        url: `/supplant/#/pendingList?time=${key}`,
        root: 'system',
        target: 'SELF'
      });
    } catch (e) {
      window.open(`${IS_DEV ? '' : '/supplant'}/#/pendingList?time=${key}`);
    }
  };

  render() {
    const { intl } = this.props;
    const { noticeData = {} } = this.state;
    return (
      <ul className="sup-pending-notice">
        {this.noticeSort.map((item) => (
          <li key={item.key} onClick={() => this.handleClick(item.key)}>
            {item.label}
            <b className="number">{noticeData[item.key]}</b>
            {intl.formatMessage(defaultMessages.item)}
          </li>
        ))}
      </ul>
    );
  }
}
