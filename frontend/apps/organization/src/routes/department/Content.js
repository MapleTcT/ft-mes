import React from 'react';
import { Layout, Icon, Modal, Tooltip, Tag, message, Spin, Radio, Authority } from 'sup-ui';
import { injectIntl } from 'react-intl';
import SupTable from 'sup-rc-table';
import { exportFile, importStatus, getDepRelatedPerson, downfile } from '../../services/departmentManage';
import style from './style.less';
import messages from './messages';
import WrappedEditForm from './EditForm';
// import noData from '../../assets/img/noData.svg';

const { Header, Content } = Layout;

class DepartmentContent extends React.Component {
  constructor(props) {
    super(props);
    const { intl } = props;
    this.departmentEditForm = React.createRef();
    this.state = {
      editModalVisible: false,
      modalDepartmentEditForm: false,
      selectedRows: [],
      btnColumns: [],
      taskStatusId: '',
      spinVisible: false,
      // total: 50,
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
          // filterType: 'search',
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
          // filterType: 'search',
          width: 300
        },
        {
          title: intl.formatMessage(messages.listPhone),
          dataIndex: 'phone',
          // filterType: 'search',
          width: 100
        }
      ]
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
    // const { selectedRows } = this.state;
    // const companyIds = companyId;
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
          //       // window.console.log(selectedRows);
          //       exportFile({
          //         all: false,
          //         companyId: companyIds,
          //         type: 'Department-Relation',
          //         orgId: activeDepartmentId,
          //         ids: selectedRows.map((item) => item.id)
          //       }).then((res) => {
          //         this.setState({
          //           taskStatusId: res.data.data.id
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
          //         type: 'Department-Relation',
          //         orgId: activeDepartmentId
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
    const { activeDepartmentId, companyId, depPersonList } = this.props;
    const { selectedRows } = this.state;
    const companyIds = companyId;
    if (this.state.value === 1) {
      exportFile({
        all: false,
        companyId: companyIds,
        type: 'Department-Relation',
        orgId: activeDepartmentId,
        ids: selectedRows.length > 0
          ? selectedRows.map((item) => item.id)
          : depPersonList.map((item) => item.id)
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
        type: 'Department-Relation',
        orgId: activeDepartmentId
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

  onChange = (e) => {
    this.setState({
      value: e.target.value
    });
  }

  handleEditDepartmentButtonClick = (e) => {
    e.preventDefault();
    this.toggleEditModal(true);
    if (this.props.menuKey === 'department') {
      this.setState({
        modalDepartmentEditForm: true
      });
    }
  };

  toggleEditModal = (visible) => {
    this.setState((state) => {
      state.editModalVisible = !!visible;
      return state;
    });
  };

  handleEditSubmit = () => {
    const departmentEditForm = this.departmentEditForm.current;
    const { editDepartment, activeDepartmentId } = this.props;
    departmentEditForm.validateFields().then(
      (editData) => {
        if (editData.managers.length > 0) {
          const id = editData.managers.map((d) => d.id);
          editData.managerIds = id;
        }
        editData.id = activeDepartmentId;
        editDepartment(editData, () => {
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

  tableSelectItem = (selectedRowKeys, selectedRows) => {
    // window.console.log(selectedRowKeys, selectedRows);
    this.setState({
      selectedRows
    }, () => {
      this.initBtn(this.props);
    });
  }

  importStatusInterval =() => {
    this.importStatus();
    this.searchStatus = setInterval(this.importStatus, 1000);
  }

  importStatus = () => {
    const { taskStatusId } = this.state;
    importStatus(taskStatusId).then((res) => {
      const { data: { data } } = res;
      if (data.status === 1 || data.status === 0) {
        // setTimeout(this.setState({
        //   spinVisible: true
        // }), 2000);
        this.setState({
          spinVisible: true
        });
      } else if (data.status === 2) {
        this.setState({
          spinVisible: false
        });
        // fileDown(data.id);
        // window.location.href = `/inter-api/organization/v1/excel/file?id=${taskStatusId}`;
        this.downloadFileBolb();
        clearInterval(this.searchStatus);
        message.success(this.props.intl.formatMessage(messages.exportSuccess));
      } else if (data.status === 3) {
        this.setState({
          spinVisible: false
        });
        if (data.hasErrorFile) {
          console.error(data.hasErrorFile);
        } else {
          message.error(data.errorMessage);
        }
      }
    });
  }

  handlegetDepRelatedPerson = () => {
    const { activeDepartmentId, changeTotal } = this.props;
    getDepRelatedPerson({
      companyId: this.props.companyId,
      departmentId: activeDepartmentId,
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
      this.handlegetDepRelatedPerson();
    });
  }

  updateColumns = (columns) => {
    this.setState({
      columns
    });
  }

  render() {
    const {
      intl,
      buttonAuthority,
      activeDepartmentData,
      relPosList,
      total,
      depPersonList,
      showSearch,
      companyId
    } = this.props;
    const spinIcon = <Icon type="loading-3-quarters" style={{ fontSize: 30 }} spin />;
    const { modalDepartmentEditForm, btnColumns, spinVisible, current, pageSize, disabledFreeze, columns, exportVisible } = this.state;
    const radioStyle = {
      display: 'block',
      height: '40px',
      lineHeight: '40px'
    };
    return (
      <>
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
              ref={this.departmentEditForm}
              modalDepartmentEditForm={modalDepartmentEditForm}
              initialValueObj={activeDepartmentData}
              isEdit
              companyId={companyId}
              // modalEditForm={modalEditForm}
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
          <Radio.Group onChange={this.onChange} value={this.state.value}>
            <Radio style={radioStyle} value={1}>
              {intl.formatMessage(messages.exportSelect)}
            </Radio>
            <Radio style={radioStyle} value={2}>
              {intl.formatMessage(messages.exportAll)}
            </Radio>
          </Radio.Group>
        </Modal>
        <Layout className={style.contentLayout}>
          <Modal
            title=""
            visible={spinVisible}
            footer={null}
            className={style.importing}
          >
            <Spin indicator={spinIcon} />
            <h3 className={style.impExping}>{intl.formatMessage(messages.exporting)}</h3>
          </Modal>
          <Header className={`${style.header} ${style.shadow}`}>{activeDepartmentData.name}</Header>
          <Content className={style.content}>
            <div className={style.contentWrap}>
              <div style={{ marginBottom: 40 }}>
                <h1 className={style.contentTitle}>
                  <span>{intl.formatMessage(messages.contentInfoBasic)}</span>
                  <Authority permissionList={buttonAuthority} permissionId="updateDepartment">
                    <Tooltip title={intl.formatMessage(messages.edit)}>
                      <a
                        href="#"
                        onClick={this.handleEditDepartmentButtonClick}
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
                        {`${intl.formatMessage(messages.departmentName)}：`}
                      </span>
                      <span className={style.contentBodyItemValue} title={activeDepartmentData.name}>
                        {activeDepartmentData.name}
                      </span>
                    </div>
                    <div className={style.contentBodyItem}>
                      <span className={style.contentBodyItemProp}>
                        {`${intl.formatMessage(messages.modl)}：`}
                      </span>
                      <span className={style.contentBodyItemValue}>
                        {activeDepartmentData.typeName}
                      </span>
                    </div>
                    <div className={style.contentBodyItem}>
                      <span className={style.contentBodyItemProp}>
                        {`${intl.formatMessage(messages.description)}：`}
                      </span>
                      <span className={style.contentBodyItemValue} title={activeDepartmentData.description}>
                        {activeDepartmentData.description}
                      </span>
                    </div>
                  </div>
                  <div style={{ marginLeft: '20px' }}>
                    <div className={style.contentBodyItem}>
                      <span className={style.contentBodyItemProp}>
                        {`${intl.formatMessage(messages.code)}：`}
                      </span>
                      <span className={style.contentBodyItemValue}>
                        {activeDepartmentData.code}
                      </span>
                    </div>
                    <div className={style.contentBodyItem}>
                      <span className={style.contentBodyItemProp}>
                        {`${intl.formatMessage(messages.managerName)}：`}
                      </span>
                      <span className={style.contentBodyItemValue}>
                        {(activeDepartmentData.managers || [])
                          .map((d) => d.managerName)
                          .join(' / ')}
                      </span>
                    </div>
                    <div className={style.contentBodyItem}>
                      <span className={style.contentBodyItemProp}>
                        {`${intl.formatMessage(messages.organizationPath)}：`}
                      </span>
                      <span className={style.contentBodyItemValue} title={activeDepartmentData.fullPath}>
                        {activeDepartmentData.fullPath}
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
                <div
                  className={style.relatedItem}
                >
                  <p className={style.relatedTitle}>
                    {intl.formatMessage(messages.editFormRelatedPosition)}
                  </p>
                  {relPosList.length > 0 ? (
                    <span className={style.relatePos}>
                      {relPosList.map((item) => {
                        return (
                          <Tooltip placement="bottom" title={item.fullPath}>
                            <Tag
                              className={style.depTag}
                            >
                              {item.name}
                            </Tag>
                          </Tooltip>
                        );
                      })}
                    </span>
                  ) : (
                    <div className={style.noRelatedContent} style={{ marginBottom: '20px' }}>
                      {/* <img src={noData} alt="nodata" /> */}
                      {intl.formatMessage(messages.noEditFormRelatedData)}
                    </div>
                  )}
                </div>
                <div className={style.relatedItem} style={{ flex: 1, marginTop: '20px' }}>
                  <p className={style.relatedTitle}>
                    {intl.formatMessage(messages.editFormRelatedPerson)}
                  </p>
                  {depPersonList.length > 0 || showSearch ? (
                    <SupTable
                      rowKey="id"
                      pagination={{
                        total,
                        current,
                        pageSize
                      }}
                      btnColumns={btnColumns}
                      columns={columns}
                      updateColumns={this.updateColumns}
                      dataSource={depPersonList}
                      tableKey="depUserTable"
                      onSelectItem={this.tableSelectItem}
                      onSearch={this.handleOnSearch}
                      disabledFreeze={disabledFreeze}
                      showColumnsFilter={false}
                      searchPlaceholder={`${intl.formatMessage(messages.tableSearchTip)}`}
                    />
                  ) : (
                    <div className={style.noRelatedContent} style={{ marginTop: '20px' }}>
                      {/* <img src={noData} alt="nodata" /> */}
                      {intl.formatMessage(messages.noEditFormRelatedData)}
                    </div>
                  )}
                </div>
              </div>
            </div>
          </Content>
        </Layout>
      </>
    );
  }
}

export default injectIntl(DepartmentContent);
