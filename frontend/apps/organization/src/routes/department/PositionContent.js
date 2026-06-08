import React from 'react';
import { Layout, Icon, Modal, Tooltip, Button, message, Spin, Tag, Radio, Authority, Divider } from 'sup-ui';
import SupTable from 'sup-rc-table';
import { injectIntl } from 'react-intl';
import { SupReferenceView } from 'sup-rc-reference';
import {
  removePositionRole,
  exportFile,
  importStatus,
  downfile,
  getPositionRelatedPerson
} from '../../services/departmentManage';
import WrappedEditForm from './EditForm';
import style from './style.less';
import messages from './messages';
import connectImg from '../../assets/img/connect.svg';
import add from '../../assets/img/add.svg';

const { Header, Content } = Layout;
// const ButtonGroup = Button.Group;

class PositionContent extends React.Component {
  constructor(props) {
    super(props);
    const { intl } = this.props;
    this.positionEditForm = React.createRef();
    this.state = {
      editModalVisible: false,
      modalPositionEditForm: false,
      supReferenceViewVisible: false,
      btnColumns: [],
      selectedRows: [],
      taskStatusId: '',
      spinVisible: false,
      current: 1,
      pageSize: 20,
      disabledFreeze: true,
      keyWord: '',
      value: 1,
      exportVisible: false,
      loadPersonFlag: false,
      columns: [
        {
          title: intl.formatMessage(messages.listName),
          dataIndex: 'name',
          width: 200
        },
        {
          title: intl.formatMessage(messages.gender),
          dataIndex: 'gender',
          width: 200
        },
        {
          title: intl.formatMessage(messages.listCode),
          dataIndex: 'code',
          width: 300
        },
        {
          title: intl.formatMessage(messages.listPhone),
          dataIndex: 'phone',
          width: 100
        }
      ]
      // importVisible: false
    };
  }

  componentWillMount() {
    this.initBtn(this.props);
  }

  componentWillReceiveProps(nextProps) {
    this.initBtn(nextProps);
  }

  initBtn = (props) => {
    const { intl } = props;
    // const _this = this
    // const companyIds = companyId;
    // const { selectedRows } = this.state;
    this.setState({
      btnColumns: [
        {
          key: 'exportMenu',
          content: intl.formatMessage(messages.export),
          // menu: [
          //   {
          //     key: 'some',
          //     disabled: selectedRows.length === 0,
          //     content: intl.formatMessage(messages.exportSelect),
          //     callback: () => {
          //       exportFile({
          //         all: false,
          //         companyId: companyIds,
          //         type: 'Position-Relation',
          //         orgId: activePositionId,
          //         ids: selectedRows.map((item) => item.id)
          //       }).then((res) => {
          //         const { id } = res.data.data;
          //         this.setState({
          //           taskStatusId: id
          //         }, () => {
          //           this.importStatusInterval();
          //         });
          //       });
          //     }
          //   },
          //   {
          //     key: 'all',
          //     content: intl.formatMessage(messages.exportAll),
          //     callback: () => {
          //       exportFile({
          //         all: true,
          //         companyId: companyIds,
          //         type: 'Position-Relation',
          //         orgId: activePositionId
          //       }).then((res) => {
          //         this.setState({
          //           taskStatusId: res.data.data.id
          //         }, () => {
          //           this.importStatusInterval();
          //         });
          //       });
          //     }
          //   }
          // ],
          // callback: (param) => {
          //   param.callback();
          // }
          callback: () => {
            this.setState((prev) => ({ exportVisible: !prev.exportVisible }));
          }
        }
      ]
    });
  }

  downloadFileBolb = () => {
    const { taskStatusId } = this.state;
    downfile(taskStatusId).then(({ headers, data }) => {
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
    });
  }

  handleExport = () => {
    const { activePositionId, companyId, positionPersonList } = this.props;
    const { selectedRows } = this.state;
    const companyIds = companyId;
    if (this.state.value === 1) {
      exportFile({
        all: false,
        companyId: companyIds,
        type: 'Position-Relation',
        orgId: activePositionId,
        ids: selectedRows.length > 0
          ? selectedRows.map((item) => item.id)
          : positionPersonList.map((item) => item.id)
      }).then((res) => {
        this.setState({
          taskStatusId: res.data.data.id,
          exportVisible: false
        }, () => {
          this.importStatusInterval();
        });
      });
    } else if (this.state.value === 2) {
      exportFile({
        all: true,
        companyId: companyIds,
        type: 'Position-Relation',
        orgId: activePositionId
      }).then((res) => {
        this.setState({
          taskStatusId: res.data.data.id,
          exportVisible: false
        }, () => {
          this.importStatusInterval();
        });
      });
    }
  }

  handleExportCancle = () => {
    this.setState({
      exportVisible: false
    });
  }

  handleOnChange = (e) => {
    this.setState({
      value: e.target.value
    });
  }

  updateColumns = (columns) => {
    this.setState({
      columns
    });
  }

  handleEditPositionButtonClick = (e) => {
    e.preventDefault();
    this.toggleEditModal(true);
    if (this.props.menuKey === 'position') {
      this.setState({ modalPositionEditForm: true });
    }
  };

  toggleEditModal = (visible) => {
    this.setState((state) => {
      state.editModalVisible = !!visible;
      return state;
    });
  };

  handleEditSubmit = () => {
    const positionEditForm = this.positionEditForm.current;
    const { activePositionId, editPosition } = this.props;
    positionEditForm.validateFields().then(
      (editData) => {
        const [depId] = editData.relatedDepartment.map((d) => d.id);
        editData.id = activePositionId;
        editData.depId = depId;
        editPosition(editData, () => {
          this.handleEditCancle();
        });
      },
      (err) => {
        console.error(err);
      },
    );
  }

  handleEditCancle = () => {
    this.toggleEditModal(false);
  };

  handleRemovePositionRole = (positionId, roleId) => {
    removePositionRole({
      positionId,
      roleId
    }).then(() => {
      this.props.searchPositionRoles(this.props.activePositionId);
    });
  };

  handleRemovePositionRoleBtn = (e, positionId, roleId, name) => {
    const { intl } = this.props;
    e.preventDefault();
    e.stopPropagation();
    Modal.confirm({
      cancelText: intl.formatMessage(messages.modalCancel),
      okText: intl.formatMessage(messages.modalok),
      title: `${intl.formatMessage(messages.removeModalTitle)} ${name}${intl.formatMessage(messages.questionMark)}`,
      content: intl.formatMessage(messages.removeModalContent),
      onOk: () => this.handleRemovePositionRole(positionId, roleId)
    });
  };

  handleRelatedRole = () => {
    this.setState({
      supReferenceViewVisible: true
    });
  };

  handleChangeVisible = () => { };

  handleRelatedRoles = (val) => {
    const { activePositionId, relatedRoles } = this.props;
    const id = val.map((d) => d.id);
    relatedRoles(Number(activePositionId), id);
  };

  tableSelectItem = (selectedRowKeys, selectedRows) => {
    this.setState({
      selectedRows
    }, () => {
      this.initBtn(this.props);
    });
  }

  importStatusInterval = () => {
    this.importStatus();
    this.searchStatus = setInterval(this.importStatus, 1000);
  }

  importStatus = () => {
    const { taskStatusId } = this.state;
    // const _this = this;
    importStatus(taskStatusId).then((res) => {
      const { data: { data } } = res;
      if (data.status === 1 || data.status === 0) {
        this.setState({
          spinVisible: true
        });
      } else if (data.status === 2) {
        // window.location.href = `/inter-api/organization/v1/excel/file?id=${taskStatusId}`;
        this.downloadFileBolb();
        this.setState({
          spinVisible: false
        });
        message.success(this.props.intl.formatMessage(messages.exportSuccess));
        clearInterval(this.searchStatus);
      } else if (data.status === 3) {
        if (data.hasErrorFile) {
          // this.setState({
          //   importVisible: true
          //   url: ''
          // });
          window.console.log(data.hasErrorFile);
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

  // handleCancel = () => {
  //   this.setState({
  //     importVisible: false
  //   });
  // }

  handlegetPositionRelatedPerson = () => {
    const { activePositionId, changeTotal } = this.props;
    getPositionRelatedPerson({
      companyId: this.props.companyId,
      positionId: activePositionId,
      current: this.state.current,
      pageSize: this.state.pageSize,
      keyword: this.state.keyWord
    }).then((res) => {
      const { data: { list, pagination } } = res;
      changeTotal(pagination.total);
      this.props.updatePersonList(list);
    });
  }

  handleOnSearch = (params) => {
    const { pagination, keyword } = params;
    const { current, pageSize } = pagination;
    this.setState({
      current,
      pageSize,
      loadPersonFlag: true,
      keyWord: keyword || ''
    }, () => {
      this.props.getFlag(this.state.loadPersonFlag);
      this.handlegetPositionRelatedPerson();
    });
  }

  render() {
    const {
      intl,
      buttonAuthority,
      activePositionData,
      positionRoles,
      activePositionId,
      positionPersonList,
      total,
      showSearch,
      companyId
    } = this.props;
    const { modalPositionEditForm, supReferenceViewVisible, btnColumns, spinVisible, current, pageSize, disabledFreeze, columns, exportVisible } = this.state;
    const spinIcon = <Icon type="loading-3-quarters" style={{ fontSize: 30 }} spin />;
    const radioStyle = {
      display: 'block',
      height: '40px',
      lineHeight: '40px'
    };
    // const uploadMes = {
    //   name: 'file',
    //   action: importFile({companyId:companyId, type: 'Position-Relation'}),
    //   showUploadList: false,
    //   headers: {
    //     authorization: `Bearer ${localStorage.getItem('ticket')}`,
    //   },
    //   onChange(info) {
    //     console.log(info)
    //     if (info.file.status === 'uploading') {
    //       console.log(info.file, info.fileList);
    //     }
    //     if (info.file.status === 'done') {
    //       message.success(`${info.file.name} file uploaded successfully`);
    //     } else if (info.file.status === 'error') {
    //       message.error(`${info.file.name} file upload failed.`);

    //     }
    //   },
    // }
    return (
      <Layout className={style.contentLayout}>
        <Modal
          title={intl.formatMessage(messages.basicInfo)}
          visible={this.state.editModalVisible}
          onOk={this.handleEditSubmit}
          onCancel={this.handleEditCancle}
          destroyOnClose
          maskClosable={false}
          wrapClassName={style.editFormWrap}
        >
          <div>
            <WrappedEditForm
              ref={this.positionEditForm}
              modalPositionEditForm={modalPositionEditForm}
              initialValueObj={activePositionData}
              isEdit
              companyId={companyId}
            />
          </div>
        </Modal>
        <Modal
          title={intl.formatMessage(messages.export)}
          visible={exportVisible}
          onOk={this.handleExport}
          onCancel={this.handleExportCancle}
          className={style.exportModal}
          maskClosable={false}
        >
          <Radio.Group onChange={this.handleOnChange} value={this.state.value}>
            <Radio style={radioStyle} value={1}>
              {intl.formatMessage(messages.exportSelect)}
            </Radio>
            <Radio style={radioStyle} value={2}>
              {intl.formatMessage(messages.exportAll)}
            </Radio>
          </Radio.Group>
        </Modal>
        {/* <Modal
          title="提示"
          visible={importVisible}
          onCancel={this.handleCancel}
          footer={
            <div style={{ textAlign: 'right' }}>
              <Button onClick={this.handleCancel}>知道了</Button>
            </div>
          }
        >
          <h3><Icon type="close-circle" />导入失败</h3>
          <p>
            导入文件错误，请
            <a href={this.state.url}>下载</a>
            查看
          </p>
        </Modal> */}
        <Modal
          title=""
          visible={spinVisible}
          footer={null}
          className={style.importing}
        >
          <Spin indicator={spinIcon} />
          <h3 className={style.impExping}>{intl.formatMessage(messages.exporting)}</h3>
        </Modal>
        <Header className={`${style.header} ${style.shadow}`}>{activePositionData.name}</Header>
        <Content className={style.content}>
          <SupReferenceView
            type="role"
            multiple
            bindKey="id"
            destroyOnClose
            selectedRowKeys={positionRoles.map((d = {}) => d.id.toString())}
            selectedRows={positionRoles}
            title={intl.formatMessage(messages.selectRole)}
            visible={supReferenceViewVisible}
            companyConfig={{
              disabled: true,
              parentId: Number(companyId)
            }}
            onCancel={() => {
              this.setState({ supReferenceViewVisible: false });
            }}
            onOk={(val) => {
              this.handleRelatedRoles(val);
              this.setState({
                supReferenceViewVisible: false
              });
            }}
          />
          <div className={style.contentWrap}>
            <div style={{ marginBottom: 40 }}>
              <h1 className={style.contentTitle}>
                <span>{intl.formatMessage(messages.contentInfoBasic)}</span>
                <Authority permissionList={buttonAuthority} permissionId="updatePos">
                  <Tooltip title={intl.formatMessage(messages.edit)}>
                    <a
                      href="#"
                      onClick={this.handleEditPositionButtonClick}
                      className={style.contentEditBtn}
                    >
                      <Icon style={{ fontSize: '16px', marginLeft: '6px' }} type="edit" />
                    </a>
                  </Tooltip>
                </Authority>
              </h1>
              <div className={style.contentBody}>
                <div style={{ marginLeft: '20px', marginBottom: '20px' }}>
                  <div className={style.contentBodyItem}>
                    <span className={style.contentBodyItemProp}>
                      {`${intl.formatMessage(messages.PositionName)}：`}
                    </span>
                    <span className={style.contentBodyItemValue} title={activePositionData.name}>
                      {activePositionData.name}
                    </span>
                  </div>
                  <div className={style.contentBodyItem}>
                    <span className={style.contentBodyItemProp}>
                      {`${intl.formatMessage(messages.editFormRelatedDepartment)}：`}
                    </span>
                    <span className={style.contentBodyItemValue}>
                      {activePositionData.depName}
                    </span>
                  </div>
                  <div className={style.contentBodyItem}>
                    <span className={style.contentBodyItemProp}>
                      {`${intl.formatMessage(messages.description)}：`}
                    </span>
                    <span className={style.contentBodyItemValue} title={activePositionData.description}>
                      {activePositionData.description}
                    </span>
                  </div>
                </div>
                <div style={{ marginLeft: '20px' }}>
                  <div className={style.contentBodyItem}>
                    <span className={style.contentBodyItemProp}>
                      {`${intl.formatMessage(messages.code)}：`}
                    </span>
                    <span className={style.contentBodyItemValue}>
                      {activePositionData.code}
                    </span>
                  </div>
                  <div className={style.contentBodyItem}>
                    <span className={style.contentBodyItemProp}>
                      {`${intl.formatMessage(messages.organizationPath)}：`}
                    </span>
                    <span className={style.contentBodyItemValue} title={activePositionData.fullPath}>
                      {activePositionData.fullPath}
                    </span>
                  </div>
                </div>
              </div>
            </div>
            <div className={style.relatedInfo}>
              <h1 className={style.contentTitle}>
                <span>
                  {intl.formatMessage(messages.associationInformation)}
                </span>
              </h1>
              {positionRoles.length > 0 ? (
                <div
                  className={style.relatedItem}
                >
                  <p className={style.relatedTitle}>
                    {intl.formatMessage(messages.editFormRelatedRoles)}
                  </p>
                  <div className={style.iconBox}>
                    <Authority permissionList={buttonAuthority} permissionId="updatePos">
                      <span className={style.relatedBtn}>
                        <Button
                          onClick={() => {
                            this.handleRelatedRole();
                          }}
                          style={{ height: '26px', lineHeight: '0.499' }}
                        >
                          {/* <Icon type="plus" theme="filled" /> */}
                          <img src={add} alt="add" />
                        </Button>
                      </span>
                      {/* <span style={{ width: 8, height: 22, borderLeft: '1px solid #D8D8D8', marginTop: '3px' }}> </span> */}
                      <Divider type="vertical" style={{ height: '28px' }} />
                    </Authority>
                    <span className={style.relatedRoles}>
                      {positionRoles.map((item) => {
                        return (
                          // <Tooltip placement="bottom" title={activePositionData.fullPath}>
                          <Tag
                            className={style.tagBox}
                            key={item.id}
                          >
                            <span>
                              {item.name}
                            </span>
                            <Authority permissionList={buttonAuthority} permissionId="updatePos">
                              <span className={style.iconShow}>
                                <Icon
                                  type="close-box"
                                  onClick={(e) => {
                                    this.handleRemovePositionRoleBtn(
                                      e,
                                      activePositionId,
                                      item.id,
                                      item.name,
                                    );
                                  }}
                                />
                              </span>
                            </Authority>
                          </Tag>
                          // </Tooltip>
                        );
                      })}
                    </span>
                  </div>
                </div>
              ) : (
                <div className={style.relatedItem} style={{ marginBottom: '20px' }}>
                  <p className={style.relatedTitle}>
                    {intl.formatMessage(messages.editFormRelatedRoles)}
                  </p>
                  {
                      buttonAuthority.includes('updatePos') ? (
                        <div className={style.noRelatedContent} style={{ fontSize: 14, color: '#354052' }}>
                          <div style={{ marginBottom: 8 }}>
                            {intl.formatMessage(messages.noEditFormRelatedRoles)}
                          </div>
                          <div>
                            <Button
                              type="primary"
                              ghost
                              className={style.noRelatedBtn}
                              style={{ width: 80 }}
                              onClick={() => {
                                this.handleRelatedRole();
                              }}
                            >
                              <img src={connectImg} alt="connect" style={{ marginRight: 5 }} />
                              {intl.formatMessage(messages.related)}
                            </Button>
                          </div>
                        </div>
                      ) : (
                        <div className={style.noRelatedContent}>
                          {/* <img src={noData} alt="nodata" /> */}
                          {intl.formatMessage(messages.noEditFormRelatedData)}
                        </div>
                      )
                    }
                </div>
              )}
              <div className={style.relatedItem} style={{ flex: 1, marginTop: '20px' }}>
                <p className={style.relatedTitle}>
                  {intl.formatMessage(messages.editFormRelatedPerson)}
                </p>
                {positionPersonList.length > 0 || showSearch ? (
                  <SupTable
                    rowKey="id"
                    onSelectItem={this.tableSelectItem}
                    pagination={{
                      total,
                      current,
                      pageSize
                    }}
                    btnColumns={btnColumns}
                    columns={columns}
                    updateColumns={this.updateColumns}
                    tableKey="positionUserTable"
                    dataSource={positionPersonList}
                    onSearch={this.handleOnSearch}
                    disabledFreeze={disabledFreeze}
                    showColumnsFilter={false}
                    searchPlaceholder={`${intl.formatMessage(messages.tableSearchTip)}`}
                  />
                ) : (
                  <div className={style.noRelatedContent}>
                    {/* <img src={noData} alt="nodata" /> */}
                    {intl.formatMessage(messages.noEditFormRelatedData)}
                  </div>
                )}
              </div>
            </div>
          </div>
        </Content>
      </Layout>
    );
  }
}

export default injectIntl(PositionContent);
