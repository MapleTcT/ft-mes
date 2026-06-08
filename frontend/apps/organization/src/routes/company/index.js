import React from 'react';
import {
  Layout,
  Icon,
  Modal,
  message,
  Divider,
  Form,
  Button,
  Tag,
  Authority
} from 'sup-ui';
import { injectIntl } from 'react-intl';
import _ from 'lodash';
import {
  getAuthority,
  companyTree
} from 'root/services/company.js';
import { delCompanyTree } from 'root/services/groupManage.js';
import SupResize from 'sup-rc-resize';
import SupTree from 'sup-rc-tree';
import SupIcon from 'sup-rc-icon';
import commonMessage from './messages';
import Edit from './edit';
import styles from './styles.less';

const { Header } = Layout;
const confirmModal = Modal.confirm;

@injectIntl
@Form.create()
export default class Company extends React.Component {
  constructor() {
    super();
    this.state = {
      buttonAuthority: [],
      gData: [],
      infoVisible: false,
      gainList: [],
      chooseData: {},
      status: null,
      companyId: '',
      keyword: '',
      selectCompanyId: '',
      visible: false
    };
    this.dataList = [];
    this.searchNode = null;
  }

  componentWillMount() {
    getAuthority('organizationcompany').then((res) => {
      this.setState({
        buttonAuthority: _.get(res, 'data.list', [])
      }, () => {
        this.initTree();
      });
    });
    // this.initTree();
  }

  initTree = (callback) => {
    const { chooseData, companyId, selectCompanyId, keyword, selectedKeys } = this.state;
    const id = _.get(chooseData, 'id', '');
    companyTree({
      companyId,
      selectCompanyId,
      keyword
    }).then((res) => {
      const { list } = res.data;
      const isexist = list.filter((x) => x.id === id).length > 0;
      this.setState({
        selectedKeys,
        companyId: [_.get(list[0], 'id', '')],
        gainList: list,
        chooseData: (id && isexist) ? list.find((x) => x.id === id) : {},
        gData: this.generateTree(list),
        infoVisible: true
      }, () => {
        if (callback) {
          callback();
        }
      });
    });
  }

  generateTree = (data) => {
    const tree = [];
    data.forEach((x) => {
      x.children = [];
      if (!x.parentId) {
        tree.push(x);
      } else {
        const obj = data.find((item) => item.id === x.parentId);
        if (!obj.children) {
          obj.children = [];
        }
        obj.children.push(x);
      }
    });
    return tree;
  }

  edit = () => {
    this.setState({
      visible: true,
      status: 'motify'
    });
  }

  delete = (item) => {
    const _self = this;
    const { intl } = this.props;
    this.setState({
      [item.id]: false
    });
    confirmModal({
      title: intl.formatMessage(commonMessage.deleteTip, { name: ` ${item.shortName} ` }),
      content: intl.formatMessage(commonMessage.deleteConfirm),
      onOk() {
        delCompanyTree(item.id).then(() => {
          message.success(intl.formatMessage(commonMessage.deleteSuccess));
          _self.setState({
            // gData: []
            infoVisible: false
          }, () => {
            _self.initTree();
          });
        });
      },
      onCancel() {
      }
    });
  }

  vagueSearch = (value) => {
    this.setState({
      // gData: [],
      infoVisible: false,
      selectCompanyId: '',
      keyword: value
    }, () => {
      this.initTree();
    });
  };

  accurateSearch = (item) => {
    this.setState({
      // gData: [],
      infoVisible: false,
      selectCompanyId: item.id,
      selectedKeys: [item.id.toString()],
      keyword: ''
    }, () => {
      this.initTree(() => {
        this.onSelect([item.id.toString()], { selected: true });
      });
    });
  }

  onSelect= (selectedKeys, obj) => {
    if (!obj.selected) {
      this.setState({
        chooseData: {}
      });
      return;
    }
    const { gData, gainList } = this.state;
    const selectNode = selectedKeys[0];
    this.searchTreeNode(selectNode, gData);
    this.setState({
      selectedKeys,
      chooseData: gainList.find((x) => x.id.toString() === selectedKeys[0])
    });
  }

  searchTreeNode = (id, tree) => {
    tree.forEach((x) => {
      if (x.id === id) {
        this.searchNode = x;
        return x;
      }
      if (x.children) {
        return this.searchTreeNode(id, x.children);
      }
    });
  }

  addTreeNode = () => {
    this.setState({
      visible: true,
      status: 'add'
    });
  }

  closeEdit = (flag = true) => {
    if (flag) {
      this.setState({
        visible: false,
        infoVisible: false
        // gData: []
      }, () => {
        this.initTree();
      });
    } else {
      this.setState({
        visible: false
      });
    }
  }

  renderNone = () => {
    const { intl } = this.props;
    return (
      <div className={styles.nomission}>
        <div className={styles.tipBox}>
          <p className={styles.tip2}>
            {intl.formatMessage(commonMessage.selectCompany)}
          </p>
        </div>
      </div>
    );
  }

  onDrag = (info) => {
    const { gData } = this.state;
    const dropKey = info.node.props.eventKey;
    const dragKey = info.dragNode.props.eventKey;
    const dropParentId = info.node.props.item.parentId;
    const dragParentId = info.dragNode.props.item.parentId;
    const dropPos = info.node.props.pos.split('-');
    const dropPosition = info.dropPosition - Number(dropPos[dropPos.length - 1]);
    const loop = (data, key, callback) => {
      for (let i = 0; i < data.length; i += 1) {
        if (data[i].id.toString() === key) {
          return callback(data[i], i, data);
        }
        if (data[i].children) {
          loop(data[i].children, key, callback);
        }
      }
    };
    const data = [].concat(gData);
    if (!info.dropToGap) {
      // 不准将节点移入其他节点内部
      return;
    }
    // 父级相同,属于同级别多才可以移动
    if (dropParentId === dragParentId) {
      let dragObj;
      loop(data, dragKey, (item, index, arr) => {
        arr.splice(index, 1);
        dragObj = item;
      });
      if (
        (info.node.props.children || []).length > 0 // Has children
        && info.node.props.expanded // Is expanded
        && dropPosition === 1 // On the bottom gap
      ) {
        loop(data, dropKey, (item) => {
          item.children = item.children || [];
          // where to insert 示例添加到头部，可以是随意位置
          item.children.unshift(dragObj);
        });
      } else {
        let ar;
        let i;
        loop(data, dropKey, (item, index, arr) => {
          ar = arr;
          i = index;
        });
        if (dropPosition === -1) {
          ar.splice(i, 0, dragObj);
        } else {
          ar.splice(i + 1, 0, dragObj);
        }
      }
    }

    this.setState({
      gData: data
    });
  }

  render() {
    const {
      gData,
      chooseData,
      visible,
      status,
      companyId,
      selectedKeys,
      buttonAuthority,
      infoVisible
    } = this.state;
    const { intl } = this.props;
    return (
      <Layout className={`${styles.wrap} company`}>
        <Header className={styles.companyHead}>
          {intl.formatMessage(commonMessage.companyManage)}
        </Header>
        <Layout>
          <SupResize
            min={220}
            max={320}
          >
            <SupTree
              placeholder={intl.formatMessage(commonMessage.companySearch)}
              showAdd={buttonAuthority.includes('addCompany')}
              draggable
              treeKey="id"
              treeTitle="shortName"
              onAdd={this.addTreeNode}
              dataSource={gData}
              onDrag={this.onDrag}
              className={styles.treeSearchBottom}
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
                    selectCompanyId: '',
                    selectedKeys: [],
                    keyword: '',
                    chooseData: {}
                  }, () => {
                    this.initTree();
                  });
                }
              }}
              fuzzyParams={
                {
                  url: '/inter-api/organization/v1/company/keyword',
                  param: 'keyword',
                  otherParams: `companyId=${companyId}`,
                  callback: (data) => {
                    return data.list.map((item) => {
                      return {
                        key: item.code,
                        id: item.id,
                        title: item.shortName
                      };
                    });
                  }
                }
              }
              optRender={(item) => {
                return (item.parentId && item.children.length === 0) ? (
                  <span className="treeOperation">
                    <Authority permissionList={buttonAuthority} permissionId="delCompany">
                      <Icon
                        type="delete"
                        onClick={(e) => {
                          e.stopPropagation();
                          this.delete(item);
                        }}
                      />
                    </Authority>
                  </span>
                ) : null;
              }}
            />
            <div className={`${styles.themeContent} themeBox`}>
              {
                Object.keys(chooseData).length > 0 && infoVisible ? (
                  <div>
                    <div className={styles.formTitle}>{chooseData.shortName}</div>
                    <div className={styles.formOp}>
                      <span className={styles.subtitle}>
                        {`1. ${intl.formatMessage(commonMessage.baseInfo)}`}
                      </span>
                      <Authority permissionList={buttonAuthority} permissionId="updateCompany">
                        <Button icon="edit" type="link" onClick={() => this.edit()} />
                      </Authority>
                    </div>
                    <Form
                      labelCol={{ span: 2 }}
                      wrapperCol={{ span: 22 }}
                      layout="horizontal"
                      style={{
                        padding: '0 30px 0 0'
                      }}
                    >
                      <Form.Item label={intl.formatMessage(commonMessage.companyName)}>
                        <span>{chooseData.fullName}</span>
                      </Form.Item>
                      <Form.Item label={intl.formatMessage(commonMessage.shortName)}>
                        {chooseData.shortName}
                      </Form.Item>
                      <Form.Item label={intl.formatMessage(commonMessage.code)}>
                        {chooseData.code}
                      </Form.Item>
                      <Form.Item label={intl.formatMessage(commonMessage.mark)}>
                        {
                          chooseData.tags && chooseData.tags.length > 0 ? chooseData.tags.map((item) => {
                            return (<Tag>{item}</Tag>);
                          }) : null
                        }
                      </Form.Item>
                      <Form.Item
                        label={intl.formatMessage(commonMessage.desc)}
                      >
                        <span
                          style={{
                            width: '90%',
                            wordBreak: 'break-all',
                            display: 'inline-block'
                          }}
                        >
                          {chooseData.description}
                        </span>
                      </Form.Item>
                      {
                        _.get(chooseData, 'users', []).length > 0 ? (
                          <div>
                            <Divider dashed style={{ margin: '30px 0' }} />
                            <Form.Item label={intl.formatMessage(commonMessage.accountInfo)}>
                              {
                                _.get(chooseData, 'users', []).map((item) => {
                                  let ret = null;
                                  if (item.userType === 1) {
                                    ret = (
                                      <span className={styles.roleBox}>
                                        <span className={styles.roleName}>
                                          {intl.formatMessage(commonMessage.administrator)}
                                        </span>
                                        <b>{intl.formatMessage(commonMessage.userSomeName, { name: item.userName })}</b>
                                      </span>
                                    );
                                  }
                                  return ret;
                                })
                              }
                            </Form.Item>
                          </div>
                        ) : null
                      }
                    </Form>
                  </div>
                ) : (
                  <div className={styles.noChoose}>
                    <SupIcon className={styles.backIcon} type="iconpoint" />
                    <span className={styles.nochooseTip}>请选择左侧对象</span>
                  </div>
                )
              }
              {
                visible ? (
                  <Edit
                    data={chooseData}
                    visible={visible}
                    status={status}
                    closeEdit={this.closeEdit}
                  />
                ) : null
              }
            </div>
          </SupResize>
        </Layout>
      </Layout>
    );
  }
}
