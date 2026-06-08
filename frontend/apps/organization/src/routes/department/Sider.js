import React from 'react';
import { injectIntl } from 'react-intl';
import { Layout, Icon, Modal, Button, Upload, message, Spin, Authority } from 'sup-ui';
import _ from 'lodash';
import SupTree from 'sup-rc-tree';
import style from './style.less';
import WrappedEditForm from './EditForm';
import messages from './messages';
import {
  addDepartment,
  removeDepartment,
  removePosition,
  departmentTree,
  positionTree,
  addPosition,
  templateDown,
  importFile,
  moveDep,
  movePosition,
  downfile,
  importStatus
} from '../../services/departmentManage';
import { companyTree } from '../../services/company.js';

const { Sider } = Layout;

class DepartmentSider extends React.Component {
  constructor(props) {
    super(props);
    this.organizationAddForm = React.createRef();
    const { intl } = this.props;
    this.state = {
      selectMenu: ['department'],
      modalTitle: [`${intl.formatMessage(messages.addDepartment)}`],
      addModalVisible: false,
      modalEditForm: true,
      gData: [],
      uploadType: 'Department',
      companyId: '',
      spinVisible: false,
      companyVisible: false,
      taskStatusId: '',
      errorVisible: false,
      selectedKeys: [],
      depSelectedkey: [],
      posSelectedkey: [],
      addDisabled: true,
      isRepeatClick: false,
      treeSearchTip: [`${intl.formatMessage(messages.depTreeSearchTip)}`]
      // url: ''
    };
    this.dataList = [];
  }

  generateList = (data) => {
    for (let i = 0; i < data.length; i += 1) {
      const node = data[i];
      const { id, name, parentId } = node;
      this.dataList.push({ id, name, parentId });
      if (node.children) {
        this.generateList(node.children);
      }
    }
  };

  getParentKey = (id, tree) => {
    let parentKey;
    for (let i = 0; i < tree.length; i += 1) {
      const node = tree[i];
      if (node.children) {
        if (node.children.some((item) => item.id === id)) {
          parentKey = node.id;
        } else if (this.getParentKey(id, node.children)) {
          parentKey = this.getParentKey(id, node.children);
        }
      }
    }
    return parentKey;
  }

  componentWillMount() {
    this.props.onRef(this);
    companyTree().then((res) => {
      this.setState(
        {
          // comTree: _.get(res, 'data.list', []),
          companyId: _.get(res, 'data.list[0].id', '')
        },
        () => {
          this.initTree();
        },
      );
    });
  }

  initTree = (value, id, params = {}, remSelectedKey, clear) => {
    let initFunction = null;
    const { selectMenu, companyId } = this.state;
    const { getCompanyId } = this.props;
    getCompanyId(companyId);
    if (selectMenu[0] === 'department') {
      initFunction = departmentTree;
      this.setState({
        modalEditForm: true
      });
    } else if (selectMenu[0] === 'position') {
      initFunction = positionTree;
      this.setState({
        modalEditForm: false
      });
    }
    initFunction({
      companyId,
      keyword: value,
      ...params
    }).then((res) => {
      const list = res.data.data.children;
      if (list) {
        this.dataList = [];
        this.generateList(list);
      }
      if (id && id !== companyId) {
        this.setState({
          selectedKeys: [id.toString()]
        }, () => {
          this.onSelect(this.state.selectedKeys);
        });
      } else {
        this.setState({
          selectedKeys: (list ? this.dataList.map((item) => item.id).includes(Number(remSelectedKey)) : remSelectedKey) ? [remSelectedKey.toString()] : []
        }, () => {
          if (this.state.selectedKeys.length === 0) {
            this.updateState();
          }
        });
      }
      if (clear === 'clear') {
        this.setState({
          addDisabled: true,
          depSelectedkey: [],
          posSelectedkey: []
        });
        this.updateState();
      }
      this.setState({
        // gData: [_.get(res, 'data.data', [])] || []
        gData: this.handleModifyId(res.data.data)
      });
    });
  };

  handleModifyId = (data) => {
    const datas = [];
    if (data) {
      data.ids = data.id;
      data.id = `com_${data.id}`;
      datas.push(data);
    }
    return datas;
  }

  updateState = () => {
    const { updateDepState, updatePosState } = this.props;
    const newId = null;
    const newData = {};
    if (this.state.modalEditForm) {
      updateDepState(newData, newId);
    } else if (!this.state.modalEditForm) {
      updatePosState(newData, newId);
    }
  }

  handleAddButtonClick = () => {
    // e.preventDefault();
    const { intl } = this.props;
    if (!this.state.addDisabled) {
      this.toggleAddModal(true);
    } else {
      message.warning(intl.formatMessage(messages.pleaseSelectNode));
    }
  };

  toggleAddModal = (visible) => {
    this.setState((state) => {
      state.addModalVisible = !!visible;
      return state;
    });
  };

  handleAddSubmit = (e) => {
    e.preventDefault();
    this.setState({
      isRepeatClick: true
    }, () => {
      const { modalEditForm, companyId } = this.state;
      const { activeDepartmentId, activePositionId, intl } = this.props;
      const organizationAddForm = this.organizationAddForm.current;
      let addFunction = null;
      organizationAddForm.validateFields().then(
        (data) => {
          data.companyId = companyId;
          if (modalEditForm) {
            addFunction = addDepartment;
            if (activeDepartmentId) {
              data.parentId = activeDepartmentId;
            }
            if (data.managers.length > 0) {
              const id = data.managers.map((d) => d.id);
              data.managerIds = id;
            }
          } else if (!modalEditForm) {
            addFunction = addPosition;
            if (data.relatedDepartment.length > 0) {
              const id = data.relatedDepartment.map((d) => d.id);
              [data.depId] = id;
            }
            if (activePositionId) {
              data.parentId = activePositionId;
            }
          }
          addFunction(data).then((res) => {
            const { id } = res.data.data;
            this.toggleAddModal(false);
            this.initTree('', id);
            message.success(intl.formatMessage(messages.addSuccess));
            this.supTreeRef.resetValue();
            this.setState({
              isRepeatClick: false
            });
          }, (err) => {
            console.error(err);
            this.setState({
              isRepeatClick: false
            });
          });
        }, () => {
          this.setState({
            isRepeatClick: false
          });
        }
      );
    });
  }

  handleAddCancel = () => {
    this.toggleAddModal(false);
  };

  changeOrg = (value) => {
    const { selectMenu, depSelectedkey, posSelectedkey } = this.state;
    const { intl, getMenuKey, selectDepartment } = this.props;
    if (value.key === 'department') {
      this.setState({
        modalTitle: [`${intl.formatMessage(messages.addDepartment)}`],
        modalEditForm: true,
        treeSearchTip: [`${intl.formatMessage(messages.depTreeSearchTip)}`],
        uploadType: 'Department'
      });
    } else if (value.key === 'position') {
      this.setState({
        modalTitle: [`${intl.formatMessage(messages.addPosition)}`],
        modalEditForm: false,
        treeSearchTip: [`${intl.formatMessage(messages.posTreeSearchTip)}`],
        uploadType: 'Position'
      });
    }
    if (selectMenu[0] !== value.key) {
      this.setState(
        {
          selectMenu: [value.key]
        },
        () => {
          if (value.key === 'department') {
            if (depSelectedkey && depSelectedkey.length > 0 && !depSelectedkey.includes('com_')) {
              this.initTree('', '', '', depSelectedkey);
              selectDepartment(depSelectedkey);
              this.setState({
                addDisabled: false
              });
            } else {
              this.setState({
                addDisabled: true
              });
              this.initTree();
            }
          } else if (value.key === 'position') {
            if (posSelectedkey && posSelectedkey.length > 0 && !posSelectedkey.includes('com_')) {
              this.setState({
                addDisabled: false
              });
              this.initTree('', '', '', posSelectedkey);
            } else {
              this.setState({
                addDisabled: true
              });
              this.initTree();
            }
          }
        },
      );
    }
    getMenuKey(value.key);
  };

  onDrop = (info) => {
    // 拖动到dropkey之前
    const dropKey = info.node.props.eventKey;
    const parentId = dropKey && dropKey.includes('com_') ? dropKey.split('com_')[1] : dropKey;
    // 拖动dragkey
    const dragKey = info.dragNode.props.eventKey;
    const dropPos = info.node.props.pos.split('-');
    const dropPosition = info.dropPosition - Number(dropPos[dropPos.length - 1]);
    const { gData } = this.state;

    if (Number(parentId) !== this.state.companyId) {
      const loop = (data, id, callback) => {
        for (let i = 0; i < data.length; i += 1) {
          // 注释
          if ((data[i].id).toString() === id) {
            return callback(data[i], i, data);
          }
          if (data[i].children) {
            loop(data[i].children, id, callback);
          }
        }
      };
      const data = _.cloneDeep(gData);
      let dragObj;
      loop(data, dragKey, (item, index, arr) => {
        arr.splice(index, 1);
        dragObj = item;
      });
      let moveFunction = null;
      if (this.state.modalEditForm) {
        moveFunction = moveDep;
      } else {
        moveFunction = movePosition;
      }
      if (!info.dropToGap) {
        // 拖进某一节点
        loop(data, parentId, (item) => {
          moveFunction({
            id: dragKey,
            parentId
          }).then(() => {
            this.setState({
              gData: data
            });
          });
          item.children = item.children || [];
          item.children.push(dragObj);
        });
      } else if (
        (info.node.props.children || []).length > 0 && info.node.props.expanded && dropPosition === 1
      ) {
        // 从一个父级下移动到另一个父级下
        moveFunction({
          id: dragKey,
          parentId
        }).then(() => {
          this.setState({
            gData: data
          });
        });
        loop(data, parentId, (item) => {
          item.children = item.children || [];
          item.children.unshift(dragObj);
        });
      } else {
        let ar;
        let i;
        loop(data, parentId, (item, index, arr) => {
          let oneParentId;
          let twoParentId;
          for (let o = 0; o < this.dataList.length; o += 1) {
            if ((this.dataList[o].id).toString() === parentId) {
              oneParentId = this.dataList[o].parentId;
            }
            if ((this.dataList[o].id).toString() === dragKey) {
              twoParentId = this.dataList[o].parentId;
            }
          }
          if (oneParentId && twoParentId) {
            moveFunction({
              id: dragKey,
              upId: parentId,
              parentId: oneParentId
            }).then(() => {
              this.setState({
                gData: data
              });
            });
          } else {
            moveFunction({
              id: dragKey,
              upId: parentId
            }).then(() => {
              this.setState({
                gData: data
              });
            });
          }
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
  };

  handleDelete = (id) => {
    const { modalEditForm, selectedKeys } = this.state;
    const { intl } = this.props;
    let removeFunction = null;
    removeFunction = modalEditForm ? removeDepartment : removePosition;
    removeFunction(id).then(() => {
      if (selectedKeys[0] === id.toString()) {
        this.setState({
          addDisabled: true
        });
        if (modalEditForm) {
          this.setState({
            depSelectedkey: []
          });
        } else if (!modalEditForm) {
          this.setState({
            posSelectedkey: []
          });
        }
        this.initTree();
        this.updateState();
      } else {
        this.initTree('', selectedKeys[0]);
      }
      message.success(intl.formatMessage(messages.removeSuccess));
    });
  };

  handleRemoveBtnClick = (item) => {
    const { intl } = this.props;
    // e.preventDefault();
    // e.stopPropagation();
    Modal.confirm({
      cancelText: intl.formatMessage(messages.modalCancel),
      okText: intl.formatMessage(messages.modalok),
      title: `${intl.formatMessage(messages.removeModalTitle)} ${item.name}${intl.formatMessage(messages.questionMark)}`,
      content: intl.formatMessage(messages.removeModalContent),
      onOk: () => this.handleDelete(item.id)
    });
  };

  downloadFileBolb = (type) => {
    let downFunction = null;
    if (type === 'Department' || type === 'Position') {
      downFunction = templateDown;
    } else {
      type = this.state.taskStatusId;
      downFunction = downfile;
    }
    downFunction(type).then(({ headers, data }) => {
      this.downloadFile(headers, data);
      this.setState({
        errorVisible: false
      });
    });
  }

  downloadFile = (headers, data) => {
    const [, file] = decodeURIComponent(headers['content-disposition']).split(
      'filename='
    );
    if (window.navigator.msSaveOrOpenBlob) {
      // Internet Explorer
      window.navigator.msSaveOrOpenBlob(
        new Blob([data]),
        file
      );
    } else {
      const blob = new Blob([data]);
      const blobURL = window.URL.createObjectURL(blob);
      const tempLink = document.createElement('a');
      tempLink.style.display = 'none';
      tempLink.href = blobURL;
      tempLink.setAttribute('download', file);
      document.body.appendChild(tempLink);
      tempLink.click();
      setTimeout(() => {
        document.body.removeChild(tempLink);
        window.URL.revokeObjectURL(blobURL);
      }, 200);
    }
  }

  handleTemplateDown = () => {
    const { modalEditForm } = this.state;
    if (modalEditForm) {
      // window.location.href = templateDown('Department');
      this.downloadFileBolb('Department');
    } else if (!modalEditForm) {
      // window.location.href = templateDown('Position');
      this.downloadFileBolb('Position');
    }
  }

  changeCompany = () => {
    const { companyVisible } = this.state;
    this.setState({
      companyVisible: !companyVisible
    });
  }

  closeTree = (params) => {
    this.setState({
      companyId: params.id,
      addDisabled: false,
      selectedKeys: [],
      depSelectedkey: [],
      posSelectedkey: []
    }, () => {
      this.updateState();
      // this.initTree();
      this.initTree('', params.id);
    });
  }

  onSelect = (selectedKeys) => {
    const { modalEditForm } = this.state;
    const { selectDepartment, selectPosition } = this.props;
    this.setState({
      selectedKeys
    });
    // if (selectedKeys[0] !== companyId.toString()) {
    if (modalEditForm) {
      if (selectedKeys[0].includes('com_')) {
        this.updateState();
      } else {
        selectDepartment(selectedKeys[0]);
      }
      this.setState({
        addDisabled: false,
        depSelectedkey: selectedKeys[0]
      });
    } else {
      if (selectedKeys[0].includes('com_')) {
        this.updateState();
      } else {
        selectPosition(selectedKeys[0]);
      }
      this.setState({
        addDisabled: false,
        posSelectedkey: selectedKeys[0]
      });
    }
    // }
  }

  importInterval = () => {
    this.importStatus();
    this.searchStatus = setInterval(this.importStatus, 2000);
  }

  importStatus = () => {
    const { taskStatusId } = this.state;
    importStatus(taskStatusId).then((res) => {
      const { data: { data } } = res;
      if (data.status === 1 || data.status === 0) {
        this.setState({
          spinVisible: true
        });
      } else if (data.status === 2) {
        message.success(this.props.intl.formatMessage(messages.importSuccess));
        this.setState({
          spinVisible: false
        });
        clearInterval(this.searchStatus);
        this.updateState();
        this.initTree();
      } else if (data.status === 3) {
        if (data.hasErrorFile) {
          this.setState({
            errorVisible: true
            // url: `/inter-api/organization/v1/excel/file?id=${taskStatusId}`
          });
        } else {
          message.error(data.errorMessage);
        }
        this.setState({
          spinVisible: false
        });
        clearInterval(this.searchStatus);
      }
    });
  }

  handleCancel = () => {
    this.setState({ errorVisible: false });
  }

  vagueSearch = (value) => {
    this.initTree(value);
  }

  accurateSearch = (item) => {
    let params = {};
    const { modalEditForm } = this.state;
    if (modalEditForm) {
      params = {
        departmentId: item.id
      };
    } else if (!modalEditForm) {
      params = {
        positionId: item.id
      };
    }
    this.setState({
      gData: []
    }, () => {
      this.initTree(item.title, item.id, params);
    });
  }

  render() {
    const {
      modalEditForm,
      gData,
      uploadType,
      companyId,
      spinVisible,
      // addDisabled,
      selectMenu,
      isRepeatClick,
      treeSearchTip,
      selectedKeys
    } = this.state;
    const companyIds = companyId;
    const { intl, buttonAuthority } = this.props;
    const spinIcon = <Icon type="loading-3-quarters" style={{ fontSize: 30 }} spin />;
    let func = null;
    if (modalEditForm) {
      func = '/inter-api/organization/v1/department/keyword';
    } else {
      func = '/inter-api/organization/v1/position/keyword';
    }
    const btn1 = selectMenu[0] === 'department' ? 'importOrg' : 'importPos';
    return (
      <Sider className={style.siderBox} style={{ backgroundColor: '#FFF', height: '100%' }}>
        <Modal
          title={this.state.modalTitle}
          visible={this.state.addModalVisible}
          onOk={this.handleAddSubmit}
          onCancel={this.handleAddCancel}
          destroyOnClose
          maskClosable={false}
          wrapClassName={style.editFormWrap}
          okButtonProps={{ disabled: isRepeatClick }}
        >
          <div className={style.groupAddForm}>
            <WrappedEditForm
              ref={this.organizationAddForm}
              modalEditForm={modalEditForm}
              companyId={companyId}
            />
          </div>
        </Modal>
        <Modal
          title=""
          visible={spinVisible}
          footer={null}
          className={style.importing}
        >
          <Spin indicator={spinIcon} />
          <h3 className={style.impExping}>{intl.formatMessage(messages.importing)}</h3>
        </Modal>
        <Modal
          title={intl.formatMessage(messages.tips)}
          visible={this.state.errorVisible}
          onCancel={this.handleCancel}
          footer={
            <div style={{ textAlign: 'right' }}>
              <Button onClick={this.handleCancel}>{intl.formatMessage(messages.gotIt)}</Button>
            </div>
          }
          className={style.importError}
        >
          <div className={style.errorContent}>
            <Icon type="error-circle" theme="filled" style={{ fontSize: '30px', color: 'rgba(255,0,10,0.80)' }} />
            <div>
              <h3>
                {/* <Icon type="close-circle" /> */}
                {intl.formatMessage(messages.importFailed)}
              </h3>
              <p>
                {intl.formatMessage(messages.importError)}
                <a onClick={this.downloadFileBolb} style={{ textDecoration: 'underline' }}>{intl.formatMessage(messages.downLoad)}</a>
                {intl.formatMessage(messages.see)}
              </p>
            </div>
          </div>
        </Modal>
        <div className={style.siderTree}>
          <SupTree
            autoExpandRoot
            ref={(res) => { this.supTreeRef = res; }}
            tabs={[{ key: 'department', title: intl.formatMessage(messages.department) }, { key: 'position', title: intl.formatMessage(messages.position) }]}
            onChangeTab={this.changeOrg}
            placeholder={treeSearchTip}
            treeKey="id"
            treeTitle="name"
            switchCompany
            onSelectCompany={this.closeTree}
            showAdd={selectMenu[0] === 'department' ? buttonAuthority.includes('addDepartment') : buttonAuthority.includes('addPosition')}
            onAdd={this.handleAddButtonClick}
            // addDisabled={addDisabled}
            selectedKeys={selectedKeys}
            onSelect={this.onSelect}
            draggable
            onDrag={this.onDrop}
            // onExpand={this.onExpand}
            dataSource={gData}
            onSearch={(param, type) => {
              if (type === 'fuzzy') {
                this.vagueSearch(param.title);
              } else if (type === 'advanced') {
                this.accurateSearch(param);
              } else {
                this.initTree('', '', '', '', 'clear');
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
                      title: item.name,
                      id: item.id
                    };
                  });
                }
              }
            }
            optRender={(item) => {
              return (
                <span className="treeOperation">
                  <Authority
                    permissionList={buttonAuthority}
                    permissionId={selectMenu[0] === 'department' ? 'deleteDep' : 'deletePos'}
                  >
                    <Icon
                      type="delete"
                      onClick={(e) => {
                        e.stopPropagation();
                        this.handleRemoveBtnClick(item);
                      }}
                    />
                  </Authority>
                </span>
              );
            }}
          />
        </div>
        <div
          className={style.SiderBtn}
          style={{ display: buttonAuthority.includes(btn1) ? 'block' : 'none' }}
        >
          <Upload
            className={style.siderImport}
            name="file"
            accept=".xlsx"
            action={importFile({ companyId: companyIds, type: uploadType })}
            showUploadList={false}
            headers={{
              authorization: `Bearer ${localStorage.getItem('ticket')}`
            }}
            onChange={(info) => {
              // if (info.file.status === 'uploading') {
              //   window.console.log(info.file, info.fileList);
              // }
              if (info.file.status === 'done') {
                this.setState({
                  taskStatusId: info.file.response.data.id
                }, () => {
                  this.importInterval();
                });
              } else if (info.file.status === 'error') {
                message.error(`${info.file.response.message}`);
              }
            }}
          >
            <Button className={style.siderImpBtn} style={{ width: '100%' }}>
              {intl.formatMessage(messages.import)}
            </Button>
          </Upload>
          <Button
            className={style.siderImpBtn}
            onClick={this.handleTemplateDown}
            style={{ width: '50%', borderLeft: '1px solid #E6EAEE' }}
          >
            {intl.formatMessage(messages.downloadTemplate)}
          </Button>
        </div>
      </Sider>
    );
  }
}

export default injectIntl(DepartmentSider);
