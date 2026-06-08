import React from 'react';
import { Tabs, Icon } from 'sup-ui';
// import { getMission } from 'root/services/messageCenter';
import { injectIntl } from 'react-intl';
import { getAuthority } from 'root/services/messageCenter';
import commonMessage from 'root/common/messages';
import MissionTable from './MissionTable';
import ReceiveTable from './ReceiveTable';
import styles from './styles.less';

const { TabPane } = Tabs;
@injectIntl
export default class MessageCenter extends React.Component {
  constructor() {
    super();
    this.state = {
      activeKey: '1',
      buttonAuthority: [],
      mission: [{
        id: 'loading',
        code: 'loading',
        name: 'loading',
        proportion: 'loading',
        createTime: 'loading'
      }]
    };
  }

  componentWillMount() {
    getAuthority('messageCenter').then((res) => {
      this.setState({
        buttonAuthority: res.data.list
      });
    });
    // getMission().then((res) => {
    //   this.setState({
    //     mission: res.data.list
    //   });
    // });
  }

  renderLightBulb = () => {
    const { intl } = this.props;
    return (
      <div className={styles.lighterBox}>
        <i className={styles.lighter} />
        <i className={styles.triangle} />
        <span>{intl.formatMessage(commonMessage.clickSetting)}</span>
      </div>
    );
  }

  renderNone = () => {
    const { intl } = this.props;
    return (
      <div className={styles.nomission}>
        <div className={styles.tipBox}>
          <i className={styles.missionIcon} />
          <p className={styles.tip1}>{intl.formatMessage(commonMessage.noMission)}</p>
          <p className={styles.tip2}>{intl.formatMessage(commonMessage.settingTip)}</p>
        </div>
      </div>
    );
  }

  render() {
    const { intl } = this.props;
    const { buttonAuthority } = this.state;
    return (
      <div className={styles.wrap}>
        <Icon
          type="set"
          theme="filled"
          className={styles.setting}
          style={{
            display: buttonAuthority.includes('protocolConfig') ? 'block' : 'none'
          }}
          onClick={() => {
            window.location.hash = '#/Setting/notice';
          }}
        />
        {!this.state.mission.length ? this.renderLightBulb() : null}
        <Tabs
          defaultActiveKey="1"
          activeKey={this.state.activeKey}
          onChange={(activeKey) => {
            this.setState({
              activeKey
            });
          }}
          style={{
            height: '100%'
          }}
          tabBarStyle={{
            padding: '0 20px',
            height: 57,
            margin: 0
          }}
        >
          <TabPane tab={intl.formatMessage(commonMessage.missionManage)} key="1">
            {/* {this.state.mission.length > 0
              ? <MissionTable data={this.state.mission} />
              : this.renderNone()} */}
            {
              this.state.activeKey === '1'
                ? <MissionTable buttonAuthority={buttonAuthority} /> : null
            }
          </TabPane>
          <TabPane tab={intl.formatMessage(commonMessage.receiveInfo)} key="2" disabled={!this.state.mission.length}>
            {
              this.state.activeKey === '2'
                ? <ReceiveTable buttonAuthority={buttonAuthority} /> : null
            }
          </TabPane>
        </Tabs>
      </div>
    );
  }
}
