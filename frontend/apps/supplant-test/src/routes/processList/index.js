import React from 'react';
import { injectIntl } from 'react-intl';
import { Tabs } from 'sup-ui';
import Panel from './Panel.js';
import defaultMessages from './messages.js';
import { getQueryString } from '../../utils/index.js';
import './index.less';

const { TabPane } = Tabs;

@injectIntl
export default class ProcessList extends React.PureComponent {
  constructor(props) {
    super(props);
    const { intl } = props;
    this.tabSort = [
      { key: 'pending', name: intl.formatMessage(defaultMessages.pending) },
      { key: 'on', name: intl.formatMessage(defaultMessages.on) },
      { key: 'over', name: intl.formatMessage(defaultMessages.over) },
      { key: 'handle', name: intl.formatMessage(defaultMessages.handle) }
    ];
    this.dftKey = getQueryString('type') || 'pending';
  }

  render() {
    return (
      <div className="sup-processList-wrap">
        <Tabs
          className="sup-process-menu"
          defaultActiveKey={this.dftKey}
          onTabClick={this.handleTabClick}
        >
          {this.tabSort.map((item) => (
            <TabPane tab={item.name} key={item.key}>
              <Panel type={item.key} />
            </TabPane>
          ))}
        </Tabs>
      </div>
    );
  }
}
