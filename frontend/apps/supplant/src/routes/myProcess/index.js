import React from 'react';
import { injectIntl } from 'react-intl';
import { Tabs } from 'sup-ui';
import './index.less';
import { getMyProcess } from '../../services/process.js';
import defaultMessages from './messages.js';
import Panel from './Panel.js';

const { TabPane } = Tabs;

@injectIntl
export default class MyProcess extends React.PureComponent {
  constructor(props) {
    super(props);
    const { intl } = props;
    this.tabSort = [
      { key: 'pending', name: intl.formatMessage(defaultMessages.pending) },
      { key: 'on', name: intl.formatMessage(defaultMessages.on) },
      { key: 'over', name: intl.formatMessage(defaultMessages.over) },
      { key: 'handle', name: intl.formatMessage(defaultMessages.handle) }
    ];
  }

  state = { sourceData: {} };

  componentDidMount() {
    this.fetchData('pending');
  }

  handleTabClick = (type) => this.fetchData(type);

  fetchData = (type) => {
    const { sourceData } = this.state;
    if (!sourceData[type]) {
      getMyProcess({ type }).then((res) => {
        if (res.data) {
          sourceData[type] = res.data.data.result;
          this.setState({ sourceData: { ...sourceData } });
        }
      });
    }
  };

  render() {
    const { sourceData } = this.state;
    return (
      <div className="sup-process-list">
        <Tabs defaultActiveKey="pending" onTabClick={this.handleTabClick}>
          {this.tabSort.map((item) => (
            <TabPane
              tab={<span className="tab-inner">{item.name}</span>}
              key={item.key}
            >
              <Panel list={sourceData[item.key]} type={item.key} />
            </TabPane>
          ))}
        </Tabs>
      </div>
    );
  }
}
