import React from 'react';
import { injectIntl } from 'react-intl';
import SupTree from 'sup-rc-tree';
import { message } from 'sup-ui';
import style from './style.less';
import messages from './messages';
import { taskTree } from '../../services/taskServer';

class SchedulerSider extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      dataSource: []
    };
  }

  componentWillMount() {
    this.props.onRef(this);
    this.initTree();
  }

  deselect() {
    this.setState({
      selectedKeys: []
    });
  }

  initTree = (value, id) => {
    const { intl } = this.props;
    const params = {
      keyword: value
    };
    if (id) {
      params.isAccurate = true;
    }
    taskTree(params).then(
      (res) => {
        this.setState({
          dataSource: res.data.list,
          selectedKeys: [id] || []
        });
      },
      () => {
        message.error(intl.formatMessage(messages.errorNetwork));
      }
    );
  };

  handleSelect = (selectedKeys, params) => {
    const { handleSelectModule } = this.props;
    const { item } = params.node.props;
    const activeId = item.moduleId;
    const activeName = item.moduleName;
    const code = item.moduleCode;
    this.setState({
      selectedKeys
    });
    handleSelectModule(activeId, activeName, code);
  };

  fuzzySearch = (param) => {
    this.initTree(param.title);
  }

  advancedSearch = (item) => {
    const { handleSelectModule } = this.props;
    const { id, title, key } = item;
    this.initTree(title, id);
    handleSelectModule(id, title, key);
  }

  onSearch = (params, type) => {
    const { handleSelectModule } = this.props;
    if (type === 'fuzzy') {
      this.fuzzySearch(params);
    } else if (type === 'advanced') {
      this.advancedSearch(params);
    } else {
      this.initTree();
      handleSelectModule();
    }
  }

  render() {
    const { selectedKeys } = this.state;
    const { intl } = this.props;
    return (
      <div className={style.siderTree}>
        <SupTree
          treeKey="moduleId"
          treeTitle="moduleName"
          placeholder={intl.formatMessage(messages.pleaseEnterSearch)}
          dataSource={this.state.dataSource}
          onSelect={this.handleSelect}
          selectedKeys={selectedKeys}
          onSearch={this.onSearch}
          fuzzyParams={
            {
              url: '/inter-api/task-scheduler/v1/job/queryModules',
              param: 'keyword',
              callback: (data) => {
                return data.list.map((item) => {
                  return {
                    key: item.moduleCode,
                    title: item.moduleName,
                    id: item.moduleId
                  };
                });
              }
            }
          }
        />
      </div>
    );
  }
}

export default injectIntl(SchedulerSider);
