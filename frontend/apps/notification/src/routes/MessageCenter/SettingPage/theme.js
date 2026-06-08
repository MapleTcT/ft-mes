import React from 'react';
import { Layout, Input, Icon, Modal, message, Form } from 'sup-ui';
import { getThemeTree, addThemeTree, updateThemeTree, delThemeTree } from 'root/services/messageCenter';
import mainStyle from 'root/routes/MessageCenter/styles.less';
import { injectIntl } from 'react-intl';
import commonMessage from 'root/common/messages';
import SupTree from 'sup-rc-tree';
import SupResize from 'sup-rc-resize';
import ThemeTable from '../ThemeTable/index';
import styles from './theme.less';
import commonStyles from './styles.less';

@injectIntl
@Form.create()
export default class Theme extends React.Component {
  constructor() {
    super();
    this.state = {
      gData: [],
      selectedKeys: null,
      chooseKey: [],
      treeVisible: false,
      editValue: '',
      status: null
    };
    this.dataList = [];
    this.treeList = [];
    this.tableList = [];
    this.noOp = ['root', 'defaultType', 'defaultType002'];
    this.searchNode = null;
  }

  componentWillMount() {
    this.initTree();
  }

  initTree = (params = {}, callback) => {
    getThemeTree(params).then((res) => {
      const { list } = res.data;
      list.unshift({
        id: '0',
        code: 'root',
        name: this.props.intl.formatMessage(commonMessage.themeType),
        memo: '固有类型',
        parentId: null,
        parentObj: null,
        layRec: 0
      });
      this.setState({
        gData: this.generateTree(list)
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
      if (x.parentId === null) {
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

  generateList = (data) => {
    for (let i = 0; i < data.length; i += 1) {
      const node = data[i];
      const { id, name } = node;
      this.dataList.push({ id, name });
      if (node.children) {
        this.generateList(node.children);
      }
    }
  }

  edit = (item, e) => {
    e.stopPropagation();
    this.setState({
      treeVisible: true,
      status: 'motify',
      editValue: item.name,
      selectedKeys: item.id
    });
  }

  delete = (item, e) => {
    e.stopPropagation();
    const { intl } = this.props;
    delThemeTree(item.id).then(() => {
      message.success(intl.formatMessage(commonMessage.deleteData));
      this.initTree({}, () => {
        this.setState({
          selectedKeys: null,
          chooseKey: []
        });
      });
    }).catch((err) => {
      message.error(err.data.message);
    });
  }

  dataHas = (data) => {
    this.tableList = data;
  }

  onSelect= (selectedKeys, obj) => {
    if (!obj.selected) {
      return;
    }
    const { gData } = this.state;
    const selectNode = selectedKeys[0];
    this.searchTreeNode(selectNode, gData);
    this.setState({
      selectedKeys: null,
      chooseKey: []
    }, () => {
      this.setState({
        selectedKeys: selectNode,
        chooseKey: selectedKeys
      });
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
    if (!this.state.selectedKeys) {
      return;
    }
    this.setState({
      treeVisible: true,
      status: 'add'
    });
  }

  newNode = () => {
    const { status, selectedKeys } = this.state;
    const { intl } = this.props;
    this.props.form.validateFields((err, values) => {
      if (!err) {
        if (status === 'add') {
          addThemeTree({
            code: values.name.trim(),
            name: values.name.trim(),
            parentId: selectedKeys || 0
          }).then(() => {
            message.success(intl.formatMessage(commonMessage.addThemeTypeSuc));
            this.initTree();
            this.setState({
              treeVisible: false,
              editValue: ''
            });
          }).catch((error) => {
            message.error(error.data.message);
          });
        } else {
          updateThemeTree({
            id: selectedKeys,
            name: values.name.trim()
          }).then(() => {
            message.success(intl.formatMessage(commonMessage.updateThemeType));
            this.initTree();
            this.setState({
              treeVisible: false,
              editValue: ''
            });
          }).catch((error) => {
            message.error(error.data.message);
          });
        }
      }
    });
  }

  renderNone = () => {
    return (
      <div className={mainStyle.nomission}>
        <div className={mainStyle.tipBox}>
          <p className={mainStyle.tip2}>{this.props.intl.formatMessage(commonMessage.enterTheme)}</p>
        </div>
      </div>
    );
  }

  vagueSearch = (value) => {
    this.setState({
      // gData: []
      selectedKeys: null,
      chooseKey: []
    }, () => {
      this.initTree({ name: value });
    });
  }

  accurateSearch = (item) => {
    this.setState({
      // gData: []
      selectedKeys: null,
      chooseKey: []
    }, () => {
      this.initTree({ code: item.code }, () => {
        this.onSelect([item.id], { selected: true });
      });
    });
  }

  render() {
    const { gData, treeVisible, editValue, selectedKeys, chooseKey } = this.state;
    const { intl, form } = this.props;
    const { getFieldDecorator } = form;
    let title = '';
    if (this.state.status === 'add') {
      title = intl.formatMessage(commonMessage.addThemeType);
    } else {
      title = intl.formatMessage(commonMessage.updateTy);
    }
    return (
      <Layout className={`${commonStyles.wrap} theme`}>
        <SupResize
          min={219}
          max={320}
        >
          <div className={styles.themeTree}>
            <SupTree
              showAdd
              treeKey="id"
              treeTitle="name"
              placeholder={intl.formatMessage(commonMessage.searchType)}
              onAdd={this.addTreeNode}
              dataSource={gData}
              className={styles.treeSearchBottom}
              onSelect={this.onSelect}
              selectedKeys={chooseKey}
              autoExpandRoot
              addDisabled={!selectedKeys}
              onSearch={(param, type) => {
                if (type === 'fuzzy') {
                  this.vagueSearch(param.title);
                } else if (type === 'advanced') {
                  this.accurateSearch(param);
                } else {
                  this.setState({
                    selectedKeys: null,
                    chooseKey: []
                  }, () => {
                    this.initTree();
                  });
                }
              }}
              fuzzyParams={
                {
                  url: '/inter-api/notification-admin/v1/notice/topictree/keyword',
                  param: 'keyword',
                  callback: (data) => {
                    return data.list.map((item) => {
                      return {
                        key: '',
                        code: item.code,
                        id: item.id,
                        title: item.name
                      };
                    });
                  }
                }
              }
              optRender={(item) => {
                if (this.noOp.includes(item.code)) {
                  return null;
                } else {
                  return (
                    <span className="treeOperation">
                      <Icon type="edit" className={styles.treeItemOp} onClick={(e) => this.edit(item, e)} />
                      <Icon type="delete" className={styles.treeItemOp} onClick={(e) => this.delete(item, e)} />
                    </span>
                  );
                }
              }}
            />
            <Modal
              title={title}
              destroyOnClose
              maskClosable={false}
              visible={treeVisible}
              okText={intl.formatMessage(commonMessage.confirm)}
              cancelText={intl.formatMessage(commonMessage.cancel)}
              onOk={this.newNode}
              onCancel={() => { this.setState({ treeVisible: false, editValue: '' }); }}
            >
              <Form
                name="basic"
                layout="vertical"
                colon={false}
              >
                <Form.Item
                  label={intl.formatMessage(commonMessage.typeName)}
                >
                  {
                    getFieldDecorator('name', {
                      initialValue: editValue,
                      rules: [{
                        required: true,
                        whitespace: true,
                        message: intl.formatMessage(commonMessage.enterTypeName)
                      }, {
                        max: 50,
                        message: intl.formatMessage(commonMessage.maxWord, { num: 50 })
                      }]
                    })(
                      <Input />
                    )
                  }
                </Form.Item>
              </Form>
            </Modal>
          </div>
          <div className={`${styles.themeContent} themeBox`}>
            {selectedKeys && selectedKeys !== '0' ? (
              <ThemeTable
                dataHas={this.dataHas}
                type={selectedKeys}
                id={selectedKeys}
              />
            ) : this.renderNone()}
          </div>
        </SupResize>
      </Layout>
    );
  }
}
