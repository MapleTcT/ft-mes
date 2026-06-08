import React from 'react';
import { injectIntl } from 'react-intl';
import moment from 'moment';
import PropTypes from 'prop-types';
import { Empty } from 'sup-ui';
import { getOpenURL } from '../../services/process.js';
import defaultMessages from './messages.js';
import { getUrlConcat, IS_DEV } from '../../utils/index.js';
import './index.less';

const SHOWNUM = 8; // 显示条数

@injectIntl
export default class Panel extends React.PureComponent {
  // 点击更多
  handleMore = () => {
    const { intl, type } = this.props;
    try {
      window.parent.CUI.loadPage({
        menuName: intl.formatMessage(defaultMessages.myFlow),
        code: 'process',
        url: `/supplant/#/processList?type=${type}`,
        root: 'system',
        target: 'SELF'
      });
    } catch (e) {
      window.open(
        IS_DEV
          ? `/#/processList?type=${type}`
          : `/supplant/#/processList?type=${type}`
      );
    }
  };

  // 超链接
  handleClick = (data, type) => {
    if (type === 'pending') {
      window.open(data.url);
    } else {
      getOpenURL(data.url).then((res) => {
        if (res.data) {
          window.open(res.data.data);
        }
      });
    }
  };

  fmtData = (data, type) => {
    const { intl } = this.props;
    return data.map((item) => {
      const {
        url,
        name, // 流程名称
        sname,
        tableNo,
        createTime,
        statusValue,
        tableInfoId,
        status,
        taskDescription,
        targetTableName,
        targetEntityCode
      } = item;
      const action = `${name}(${tableNo})`;
      const time = moment(createTime).format('YYYY-MM-DD');
      const state = type === 'pending' ? taskDescription : status; // 状态
      const creator = `${sname}${intl.formatMessage(defaultMessages.created)}`;
      const urlParam = getUrlConcat({
        tableInfoId,
        status: statusValue,
        entityCode: targetEntityCode,
        targetTablename: targetTableName
      });
      return {
        time,
        sname: creator,
        state,
        action,
        title: `${time} ${creator} ${action} ${state}`,
        url: type === 'pending' ? url : urlParam
      };
    });
  };

  render() {
    const { list = [], type, intl } = this.props;
    if (list.length === 0) {
      return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} />;
    }
    const dom = [];
    const fmtData = this.fmtData(list, type);
    for (let i = 0; i < SHOWNUM; i += 1) {
      const item = fmtData[i];
      if (!item) break;
      dom.push(
        <div
          className="process-info"
          key={`${i * 10}`}
          title={item.title}
          onClick={() => this.handleClick(item, type)}
        >
          <span className="time">{item.time}</span>
          {['pending', 'handle'].includes(type) && (
            <span className="name">{item.sname}</span>
          )}
          <span className="action">{item.action}</span>
          <span className="state">{item.state}</span>
        </div>
      );
    }
    if (fmtData.length > SHOWNUM) {
      dom.push(
        <div className="process-more">
          <a onClick={this.handleMore}>
            {intl.formatMessage(defaultMessages.more)}
          </a>
        </div>
      );
    }
    return dom;
  }
}
Panel.defaultProps = { type: 'pending', list: [] };
Panel.propTypes = { type: PropTypes.string, list: PropTypes.array };
