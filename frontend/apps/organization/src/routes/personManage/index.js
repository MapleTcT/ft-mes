import React from 'react';
import {
  Layout,
  Form,
  Spin
} from 'sup-ui';
import { injectIntl } from 'react-intl';
import _ from 'lodash';
import { companyTree } from 'root/services/company.js';
import {
  getAuthority,
  getPostTree,
  getDepartmentTree
} from 'root/services/personManage';
import SupIcon from 'sup-rc-icon';
import SupTree from 'sup-rc-tree';
import SupResize from 'sup-rc-resize';
import SupSearch from 'sup-rc-search';
import DetailTable from './DetailTable.js';
// import Edit from './edit';
import styles from './styles.less';
import commonMessage from './messages';

const { Header } = Layout;

@injectIntl
@Form.create()
export default class Company extends React.Component {
  constructor() {
    super();
    this.state = {
      spin: false,
      spinTip: '',
      buttonAuthority: [],
      companyVisible: false,
      tableVisible: false,
      gData: [],
      selectMenu: ['post'],
      selectedKeys: [],
      companyId: '',
      keyword: '',
      chooseData: {}
    };
    this.dataList = [];
    this.ppTree = [];
    this.searchNode = null;
  }

  componentWillMount() {
    Promise.all([getAuthority('personmanage'), companyTree()])
      .then((res) => {
        this.setState({
          buttonAuthority: _.get(res[0], 'data.list', []),
          companyId: _.get(res[1], 'data.list[0].id', ''),
          rootId: _.get(res[1], 'data.list[0].id', '')
        }, () => {
          this.initTree();
        });
      });
    // companyTree().then((res) => {
    //   this.setState({
    //     companyId: _.get(res, 'data.list[0].id', ''),
    //     rootId: _.get(res, 'data.list[0].id', '')
    //   }, () => {
    //     this.initTree();
    //   });
    // });
  }

  initTree = (callback, params = {}) => {
    let initFunction = null;
    const { selectMenu, keyword, companyId } = this.state;
    // 获取切换组织，部门之前选中的数据
    const chooseData = _.get(this, `state.${selectMenu[0]}Data`, {});
    const selectedKeys = _.get(this, `state.${selectMenu[0]}`, []);
    if (selectMenu[0] === 'post') {
      initFunction = getPostTree;
    } else if (selectMenu[0] === 'department') {
      initFunction = getDepartmentTree;
    }
    initFunction({
      companyId,
      keyword,
      ...params
    }).then((res) => {
      this.setState({
        // expandedKeys: [_.get(list[0], 'id', '')],
        gData: [_.get(res, 'data.data', [])] || [],
        chooseData,
        selectedKeys,
        tableVisible: true
      }, () => {
        if (callback) {
          callback([_.get(res, 'data.data', [])] || []);
        }
      });
    });
  }

  closeTree = (id) => {
    // 清楚所有记录过点击多部门和岗位
    const menuKey1 = 'post';
    const menuKey2 = 'department';
    const menuDataKey1 = 'postData';
    const menuDataKey2 = 'departmentData';
    this.setState({
      companyId: id,
      rootId: id,
      companyVisible: false,
      gData: [],
      tableVisible: false,
      chooseData: {},
      [menuKey1]: '',
      [menuKey2]: '',
      [menuDataKey1]: '',
      [menuDataKey2]: ''
    }, () => {
      this.initTree();
    });
  }

  vagueSearch = (value) => {
    const { selectMenu } = this.state;
    const menuKey = selectMenu[0];
    const menuDataKey = `${selectMenu[0]}Data`;
    this.setState({
      chooseData: {},
      selectedKeys: [],
      tableVisible: false,
      keyword: value,
      [menuKey]: [],
      [menuDataKey]: {},
      gData: []
    }, () => {
      this.initTree();
    });
  }

  accurateSearch = (item) => {
    let params = {};
    const { selectMenu, companyId } = this.state;
    const menuKey = selectMenu[0];
    const menuDataKey = `${selectMenu[0]}Data`;
    if (selectMenu[0] === 'post') {
      params = {
        positionId: item.id
      };
    } else if (selectMenu[0] === 'department') {
      params = {
        departmentId: item.id
      };
    }
    this.setState({
      // gData: [],
      [menuKey]: [],
      [menuDataKey]: {},
      tableVisible: false,
      keyword: ''
    }, () => {
      this.initTree(() => {
        this.setState({
          selectedKeys: [item.id.toString()],
          chooseData: {
            companyId,
            id: item.id,
            name: item.title
          }
        });
      }, params);
    });
  }

  transformTree = (list) => {
    if (list.length === 0) {
      return [];
    }
    list.forEach((item) => {
      this.ppTree.push(item.id.toString());
      this.ppTree.concat(this.transformTree(item.children || []));
    });
  }

  onSelect= (selectedKeys, obj) => {
    if (!obj.selected) {
      this.setState({
        chooseData: {},
        selectedKeys: []
      });
      return;
    }
    // 记录部门，岗位中点击过多节点
    const { selectMenu } = this.state;
    const menuKey = selectMenu[0];
    const menuDataKey = `${selectMenu[0]}Data`;
    this.setState({
      chooseData: obj.node.props.item,
      selectedKeys,
      [menuKey]: selectedKeys,
      [menuDataKey]: obj.node.props.item
    });
  }

  renderNone = () => {
    return (
      <div className={styles.nomission}>
        <div className={styles.tipBox}>
          <SupIcon className={styles.backIcon} type="iconpoint" />
          <span className={styles.nochooseTip}>{this.props.intl.formatMessage(commonMessage.selectObject)}</span>
        </div>
      </div>
    );
  }

  changeOrg = (value) => {
    const { selectMenu } = this.state;
    if (selectMenu[0] !== value.key) {
      this.setState({
        selectMenu: [value.key],
        chooseData: {},
        // gData: [],
        keyword: '',
        tableVisible: false
      }, () => {
        this.initTree();
      });
    }
  }

  globalSearch = (value) => {
    const { companyId } = this.state;
    this.setState({
      chooseData: {
        keyword: value,
        companyId,
        id: companyId
      },
      selectedKeys: []
    });
  }

  onSelectCompany = (params) => {
    this.setState({
      selectedKeys: []
    }, () => {
      this.closeTree(params.id);
    });
  }

  spinRender = (spin, spinTip) => {
    if (spinTip) {
      this.setState({
        spin,
        spinTip
      });
    } else {
      this.setState({
        spin
      });
    }
  }

  render() {
    const {
      gData,
      chooseData,
      selectMenu,
      selectedKeys,
      rootId,
      companyId,
      buttonAuthority,
      spinTip,
      spin,
      tableVisible
    } = this.state;
    const { intl } = this.props;
    let func = null;
    if (selectMenu[0] === 'post') {
      func = '/inter-api/organization/v1/position/keyword/ref';
    } else {
      func = '/inter-api/organization/v1/department/keyword/ref';
    }
    return (
      <Layout
        className={`${styles.wrap} person`}
        onClick={() => {
          if (this.state.companyVisible) {
            this.setState({ companyVisible: false });
          }
        }}
      >
        <Spin
          tip={spinTip}
          spinning={spin}
        >
          <Header className={styles.themeContent}>
            {intl.formatMessage(commonMessage.personManage)}
            {
              Object.keys(chooseData).length > 0 ? (
                <SupSearch
                  placeholder={intl.formatMessage(commonMessage.enterKeyword)}
                  style={{ float: 'right' }}
                  onSearch={(value) => { this.globalSearch(value); }}
                />
              ) : null
            }
          </Header>
          <Layout>
            <SupResize
              min={220}
              max={320}
            >
              <div className={styles.themeTree}>
                <SupTree
                  placeholder={selectMenu[0] === 'post' ? intl.formatMessage(commonMessage.searchPosition) : intl.formatMessage(commonMessage.searchDepartment)}
                  switchCompany
                  onSelectCompany={this.onSelectCompany}
                  tabs={[{ key: 'post', title: intl.formatMessage(commonMessage.post) }, { key: 'department', title: intl.formatMessage(commonMessage.department) }]}
                  onChangeTab={this.changeOrg}
                  treeKey="id"
                  treeTitle="name"
                  showAdd={false}
                  dataSource={gData}
                  onSelect={this.onSelect}
                  selectedKeys={selectedKeys}
                  autoExpandRoot
                  onSearch={(param, type) => {
                    if (type === 'fuzzy') {
                      this.vagueSearch(param.title);
                    } else if (type === 'advanced') {
                      this.accurateSearch(param);
                    } else {
                      this.setState({
                        // gData: [],
                        tableVisible: false,
                        keyword: ''
                      }, () => {
                        this.initTree();
                      });
                    }
                  }}
                  fuzzyParams={
                    {
                      url: func,
                      param: 'keyword',
                      otherParams: `companyId=${companyId}`,
                      callback: (data) => {
                        return data.list.map((item) => {
                          return {
                            key: item.code,
                            id: item.id,
                            title: item.name
                          };
                        });
                      }
                    }
                  }
                />
              </div>
              {
                tableVisible && Object.keys(chooseData).length > 0 ? (
                  <DetailTable
                    rootId={rootId}
                    companyId={companyId}
                    chooseData={chooseData}
                    selectMenu={selectMenu[0]}
                    buttonAuthority={buttonAuthority}
                    spinRender={this.spinRender}
                  />
                ) : this.renderNone()
              }
            </SupResize>
          </Layout>
        </Spin>
      </Layout>
    );
  }
}
