import React from 'react';
import { Layout } from 'sup-ui';
import { injectIntl } from 'react-intl';
import SupResize from 'sup-rc-resize';
import SupIcon from 'sup-rc-icon';
import SchedulerSider from './SchedulerSider';
import TaskTable from './TaskTable';
import style from './style.less';
import messages from './messages';
import { EMPTY_SELECT_NODE } from './constant';

const { Header } = Layout;

class Scheduler extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      activeId: null,
      activeName: ''
    };
  }

  handleDeselect = (cb) => {
    this.SchedulerSider.deselect();
    // 设置选中module为空
    this.setState({
      activeId: EMPTY_SELECT_NODE,
      activeName: null,
      moduleCode: null
    }, cb);
  };

  handleSelectModule = (id, name, code) => {
    this.setState({
      activeId: id,
      activeName: name,
      moduleCode: code
    }, () => {
      if (this.TaskTable) {
        this.TaskTable.handleSelectModule();
      }
    });
  };

  render() {
    const { intl } = this.props;
    const { activeId, activeName, moduleCode } = this.state;
    return (
      <Layout className={style.container}>
        <Header className={style.head}>
          {intl.formatMessage(messages.scheduler)}
        </Header>
        <SupResize
          min={220}
          max={320}
          style={{ height: 'calc(100% - 56px)', background: '#fff' }}
          direction="col"
        >
          <SchedulerSider
            handleSelectModule={this.handleSelectModule}
            onRef={(ref) => {
              this.SchedulerSider = ref;
            }}
          />
          {activeId ? (
            <TaskTable
              activeId={activeId}
              activeName={activeName}
              moduleCode={moduleCode}
              // 用于取消选择树
              handleDeselect={this.handleDeselect}
              onRefs={(ref) => {
                this.TaskTable = ref;
              }}
            />
          ) : (
            <div className={style.emptyContent}>
              <SupIcon className={style.leftIcon} type="iconpoint" />
              {intl.formatMessage(messages.chooseLeftItem)}
            </div>
          )}
        </SupResize>
      </Layout>
    );
  }
}

export default injectIntl(Scheduler);
