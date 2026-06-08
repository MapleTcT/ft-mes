import React from 'react';
import {
  Form,
  Modal,
  message,
  Divider,
  Radio,
  Authority
} from 'sup-ui';
import { injectIntl } from 'react-intl';
import {
  getPostPerson,
  uploadUrl,
  delPerson,
  getDepartmentPerson,
  getSysCode,
  exportXls,
  template,
  importStatus,
  downfile,
  getImg
} from 'root/services/personManage';
import SupTable from 'sup-rc-table';
import styles from './styles.less';
import AddPerson from './AddPerson';
import PostTransfer from './PostTransfer';
import PostTransferOut from './PostTransferOut';
import commonMessage from './messages';
import IconFemale from '../../assets/img/tx_female.png';
import IconMale from '../../assets/img/tx_male.png';

const confirmModal = Modal.confirm;

export const XLSX_TYPE = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';

@injectIntl
@Form.create()
export default class DetailTable extends React.Component {
  constructor(props) {
    super(props);
    const { intl, buttonAuthority } = props;
    const url = props.selectMenu === 'post'
      ? '/inter-api/organization/v1/position/condition/person/ref'
      : '/inter-api/organization/v1/department/condition/person/ref';
    this.state = {
      postVisible: false,
      outVisible: false,
      exportVisible: false,
      // tableWidth: 1000,
      radio: 'some',
      total: 50,
      current: 1,
      pageSize: 20,
      record: {},
      filteredInfo: {},
      selectedRows: [],
      selectedRowKeys: [],
      data: [],
      btnColumns: [],
      missionId: '',
      filterParams: {
        name: {
          url,
          customParmas: this.customParmas,
          param: 'name',
          callback: (result) => {
            const data = [];
            result.data.list.forEach((item) => {
              data.push(item.name);
            });
            return data;
          }
        },
        code: {
          url,
          customParmas: this.customParmas,
          param: 'code',
          callback: (result) => {
            const data = [];
            result.data.list.forEach((item) => {
              data.push(item.code);
            });
            return data;
          }
        },
        phone: {
          url,
          customParmas: this.customParmas,
          param: 'phone',
          callback: (result) => {
            const data = [];
            result.data.list.forEach((item) => {
              data.push(item.phone);
            });
            return data;
          }
        },
        email: {
          url,
          customParmas: this.customParmas,
          param: 'email',
          callback: (result) => {
            const data = [];
            result.data.list.forEach((item) => {
              data.push(item.email);
            });
            return data;
          }
        },
        directLeaderName: {
          url,
          customParmas: this.customParmas,
          param: 'directLeaderName',
          callback: (result) => {
            const data = [];
            result.data.list.forEach((item) => {
              if (item.directLeaderName) data.push(item.directLeaderName);
            });

            return data;
          }
        },
        grandLeaderName: {
          url,
          customParmas: this.customParmas,
          param: 'grandLeaderName',
          callback: (result) => {
            const data = [];
            result.data.list.forEach((item) => {
              if (item.grandLeaderName) data.push(item.grandLeaderName);
            });
            return data;
          }
        }
      },
      columns: [
        {
          title: intl.formatMessage(commonMessage.headImg),
          dataIndex: 'avatarUrl',
          width: 60,
          render: (r, record = {}) => {
            const { gender } = record;
            const { headIcon = {} } = this.state;
            let imgUrl = '';
            const tit = intl.formatMessage(commonMessage.headImg);
            if (['male', '男', 'sys_gender/male'].includes(gender)) imgUrl = IconMale;
            if (['female', '女', 'sys_gender/female'].includes(gender)) imgUrl = IconFemale;
            if (headIcon[r]) imgUrl = `data:image/png;base64,${headIcon[r]}`;
            return <div className={styles.head_icon}><img alt={tit} name="headpic" src={imgUrl || IconMale} /></div>;
          }
        },
        {
          title: intl.formatMessage(commonMessage.name),
          dataIndex: 'name',
          width: 120,
          filterType: 'search'
        },
        {
          title: intl.formatMessage(commonMessage.code),
          dataIndex: 'code',
          filterType: 'search',
          width: 120
        },
        {
          title: intl.formatMessage(commonMessage.phone),
          dataIndex: 'phone',
          filterType: 'search',
          width: 150
        },
        {
          title: intl.formatMessage(commonMessage.departmentPath),
          dataIndex: 'departmentFullPath',
          width: 200,
          render: (text) => {
            return this.transformPath(text);
          }
        },
        {
          title: intl.formatMessage(commonMessage.postPath),
          dataIndex: 'positionFullPath',
          width: 200,
          render: (text) => {
            return this.transformPath(text);
          }
        },
        {
          title: intl.formatMessage(commonMessage.email),
          dataIndex: 'email',
          filterType: 'search',
          hide: true,
          width: 180
        },
        {
          title: intl.formatMessage(commonMessage.sex),
          dataIndex: 'gender',
          width: 80,
          filterType: 'checkbox',
          filterOptions: [{
            label: intl.formatMessage(commonMessage.male),
            value: 'male'
          }, {
            label: intl.formatMessage(commonMessage.female),
            value: 'female'
          }],
          hide: true,
          render: null
        },
        {
          title: intl.formatMessage(commonMessage.description),
          dataIndex: 'description',
          filterType: 'search',
          width: 300,
          hide: true
        },
        {
          title: intl.formatMessage(commonMessage.status),
          dataIndex: 'status',
          hide: true,
          width: 100
        },
        {
          title: intl.formatMessage(commonMessage.entryDate),
          dataIndex: 'entryDate',
          hide: true,
          width: 100
        },
        {
          title: intl.formatMessage(commonMessage.directLeader),
          dataIndex: 'directLeaderName',
          hide: true,
          filterType: 'search',
          width: 200
        },
        {
          title: intl.formatMessage(commonMessage.septumLeader),
          dataIndex: 'grandLeaderName',
          hide: true,
          filterType: 'search',
          width: 200
        },
        {
          title: intl.formatMessage(commonMessage.technicalTitle),
          dataIndex: 'title',
          hide: true,
          width: 100
        },
        {
          title: intl.formatMessage(commonMessage.qualification),
          dataIndex: 'qualification',
          hide: true,
          width: 100
        },
        {
          title: intl.formatMessage(commonMessage.education),
          dataIndex: 'education',
          hide: true,
          width: 100
        },
        {
          title: intl.formatMessage(commonMessage.major),
          dataIndex: 'major',
          hide: true,
          width: 100
        },
        {
          title: intl.formatMessage(commonMessage.IDCard),
          dataIndex: 'idNumber',
          hide: true,
          width: 100
        },
        {
          title: intl.formatMessage(commonMessage.operate),
          dataIndex: 'operation',
          type: 'operation',
          width: 180,
          render: (text, record) => (
            <span>
              {
                props.selectMenu !== 'post' ? (
                  <span>
                    <a
                      onClick={() => {
                        this.edit(record);
                      }}
                    >
                      {intl.formatMessage(commonMessage.check)}
                    </a>
                  </span>
                ) : null
              }
              {
                props.selectMenu !== 'post' && buttonAuthority.includes('deletePerson') ? (
                  <Divider type="vertical" />
                ) : null
              }
              <Authority permissionList={buttonAuthority} permissionId="offPosition">
                {
                  (record.positionFullPath.length > 1 && props.selectMenu === 'post') ? (
                    <span>
                      <a
                        onClick={() => {
                          this.setState({
                            outVisible: true,
                            record
                          });
                        }}
                      >
                        {intl.formatMessage(commonMessage.transferOut)}
                      </a>
                    </span>
                  ) : null
                }
              </Authority>
              {
                props.selectMenu === 'post' && buttonAuthority.includes('offPosition') && buttonAuthority.includes('updatePerson') ? (
                  <Divider type="vertical" />
                ) : null
              }
              <Authority permissionList={buttonAuthority} permissionId="updatePerson">
                {
                  props.selectMenu === 'post' ? (
                    <span>
                      <a
                        onClick={() => {
                          this.edit(record);
                        }}
                      >
                        {intl.formatMessage(commonMessage.edit)}
                      </a>
                    </span>
                  ) : null
                }
              </Authority>
              {
                props.selectMenu === 'post' && buttonAuthority.includes('updatePerson') && buttonAuthority.includes('deletePerson') ? (
                  <Divider type="vertical" />
                ) : null
              }
              <Authority permissionList={buttonAuthority} permissionId="deletePerson">
                <a onClick={() => { this.delete(record); }}>
                  {intl.formatMessage(commonMessage.delete)}
                </a>
              </Authority>
            </span>
          )
        }
      ]
    };
  }

  componentWillMount() {
    // const { columns } = this.state;
    this.fiterColumn();
    this.initTable(this.props);
    this.initBtn(this.props);
    // const tableWidth = columns.filter((item) => !item.hide).map((item) => item.width).reduce((a, b) => a + b);
    // this.setState({
    //   tableWidth: tableWidth + 60
    // });
    getSysCode({ entityCode: 'sys_gender' }).then((res) => {
      this.state.columns.find((item) => item.dataIndex === 'gender').filterOptions = res.data.list.map((x) => {
        return {
          label: x.displayName,
          value: x.code
        };
      });
    });
  }

  componentDidMount() {
    if (this.table && this.table.changeOperationFixedStatus) {
      this.table.changeOperationFixedStatus();
    }
  }

  fiterColumn = () => {
    const { columns } = this.state;
    columns.forEach((item) => {
      item.filteredValue = this.state.filteredInfo[item.dataIndex] || [];
    });
  }

  componentWillReceiveProps(nextProps) {
    if (
      (this.props.chooseData.id !== nextProps.chooseData.id)
      || (this.props.chooseData.keyword !== nextProps.chooseData.keyword)
    ) {
      this.setState({
        current: 1,
        filteredInfo: {},
        selectedRows: [],
        selectedRowKeys: []
      }, () => {
        this.fiterColumn();
        this.initTable(nextProps);
        this.initBtn(nextProps);
      });
    }
  }

  initBtn = (props) => {
    const { intl, chooseData, selectMenu, buttonAuthority } = props;
    const _self = this;
    this.setState({
      btnColumns: selectMenu === 'post' ? [
        {
          key: 'add',
          content: intl.formatMessage(commonMessage.add),
          authority: buttonAuthority.includes('addPerson'),
          disabled: chooseData.id === chooseData.companyId,
          callback: this.add
        },
        {
          key: 'transferIn',
          content: intl.formatMessage(commonMessage.transferIn),
          authority: buttonAuthority.includes('transferPosition'),
          disabled: chooseData.id === chooseData.companyId,
          callback: this.postModal
        },
        {
          key: 'exportMenu',
          content: intl.formatMessage(commonMessage.export),
          callback: this.openExport
        },
        {
          key: 'delete',
          disabled: _self.state.selectedRows.length === 0,
          authority: buttonAuthority.includes('deletePerson'),
          callback: () => {
            this.delete(null);
          }
        },
        {
          key: 'importMenu',
          menu: [
            {
              key: 'import',
              authority: buttonAuthority.includes('importPerson'),
              importParams: {
                name: 'file',
                accept: '.xlsx',
                action: uploadUrl({ companyId: this.props.chooseData.companyId, type: 'Person' }),
                showUploadList: false,
                headers: {
                  Authorization: `Bearer ${localStorage.getItem('ticket')}`
                },
                onChange(info) {
                  if (info.file.status !== 'uploading') {
                    console.log(info.file, info.fileList);
                  }
                  if (info.file.status === 'done') {
                    _self.setState({
                      missionId: info.file.response.data.id,
                      opState: 'importing'
                      // spin: true,
                      // spinTip: intl.formatMessage(commonMessage.importing)
                    }, () => {
                      _self.props.spinRender(true, intl.formatMessage(commonMessage.importing));
                      _self.importInterval();
                    });
                  } else if (info.file.status === 'error') {
                    message.error(info.file.response.message);
                  }
                }
              },
              content: intl.formatMessage(commonMessage.import)
            },
            {
              key: 'down',
              exportTemplate: true,
              templateUrl: '',
              authority: buttonAuthority.includes('importPerson'),
              content: intl.formatMessage(commonMessage.downModel),
              callback: () => {
                template().then(({ headers, data }) => {
                  this.downFile(headers, data);
                });
              }
            }
          ],
          callback: (param) => {
            if (param.callback) {
              param.callback();
            }
          }
        }
      ] : [
        {
          key: 'exportMenu',
          content: intl.formatMessage(commonMessage.export),
          callback: this.openExport
        },
        {
          key: 'delete',
          disabled: _self.state.selectedRows.length === 0,
          authority: buttonAuthority.includes('deletePerson'),
          callback: () => {
            this.delete(null);
          }
        }
      ]
    });
  }

  initTable = (props, inputSearch = {}) => {
    // const { id } = this.props;
    let initFunction = null;
    let search = {};
    const { chooseData, selectMenu } = props;
    const { current, pageSize } = this.state;
    if (selectMenu === 'post') {
      initFunction = getPostPerson;
      search = {
        companyId: chooseData.companyId,
        positionId: chooseData.id,
        keyword: chooseData.keyword,
        current,
        pageSize,
        ...inputSearch
      };
    } else if (selectMenu === 'department') {
      initFunction = getDepartmentPerson;
      search = {
        companyId: chooseData.companyId,
        departmentId: chooseData.id,
        keyword: chooseData.keyword,
        current,
        pageSize,
        ...inputSearch
      };
    }
    initFunction(search).then((res) => {
      this.setState({
        data: res.data.list,
        total: res.data.pagination.total,
        current: res.data.pagination.current,
        pageSize: res.data.pagination.pageSize
      }, () => {
        this.getHeadIcon(res.data.list);
      });
    });
  }

  getHeadIcon = (list) => {
    if (!list.length) return;
    const iconsObj = list.filter((item) => item.avatarUrl);
    if (iconsObj.length) {
      getImg(iconsObj.map((i) => i.avatarUrl)).then((res) => {
        const { data } = res.data;
        this.setState({ headIcon: data });
      });
    }
  }

  customParmas = () => {
    const { selectMenu, chooseData } = this.props;
    return selectMenu === 'post'
      ? `companyId=${chooseData.companyId}&positionId=${chooseData.id}`
      : `companyId=${chooseData.companyId}&departmentId=${chooseData.id}`;
  }

  transformPath(text) {
    if (text) {
      return text.map((item) => {
        const { fullPath } = item;
        let use = '';
        const fullPathArr = fullPath.split('/');
        if (fullPathArr.length > 2) {
          use = `${fullPathArr[0]}...${fullPathArr[fullPathArr.length - 1]}`;
        } else {
          use = fullPath;
        }
        return <span title={fullPath} className={styles.morePath} key={fullPath}>{use}</span>;
      });
    }
  }

  openExport = () => {
    this.setState({
      exportVisible: true
    });
  }

  delete = (record = null) => {
    const _self = this;
    const { selectedRows, data, current } = this.state;
    const { intl } = this.props;
    confirmModal({
      title: selectedRows.length === 0
        ? intl.formatMessage(commonMessage.delSomeTip, { name: record.name }) : intl.formatMessage(commonMessage.delAllTip),
      onOk() {
        let ids = [];
        if (record) {
          ids = [record.id];
        } else {
          ids = selectedRows.map((item) => {
            return item.id;
          });
        }
        delPerson(ids.join(',')).then(() => {
          message.success(intl.formatMessage(commonMessage.delSuccess));
          if (data.length === ids.length && current !== 1) {
            _self.setState({
              current: current - 1
            }, () => {
              _self.initTable(_self.props);
              if (!record) {
                _self.setState({
                  selectedRows: []
                }, () => {
                  _self.reRenderDeleteBtn();
                });
              }
            });
          } else {
            _self.initTable(_self.props);
            if (!record) {
              _self.setState({
                selectedRows: []
              }, () => {
                _self.reRenderDeleteBtn();
              });
            }
          }
        });
      },
      onCancel() {
      }
    });
  }

  edit = (record) => {
    this.setState({
      record,
      addVisible: true,
      personStatus: 'modify'
    });
  }

  add = () => {
    this.setState({
      record: {},
      addVisible: true,
      personStatus: 'add'
    });
  }

  // 导入状态查询---定时
  importInterval = () => {
    this.importStatus();
    this.searchStatus = setInterval(this.importStatus, 1000);
  }

  // 导入状态查询
  importStatus = () => {
    const { missionId, opState } = this.state;
    const { intl } = this.props;
    importStatus(missionId).then((res) => {
      const { data: { data } } = res;
      if (data.status === 1 || data.status === 0) {
        this.props.spinRender(true, '');
      } else if (data.status === 2) {
        if (opState === 'exporting') {
          message.success('导出成功');
          this.downloadFileBolb();
        } else {
          message.success(intl.formatMessage(commonMessage.importSuccess));
          this.initTable(this.props);
        }
        this.props.spinRender(false, '');
        clearInterval(this.searchStatus);
      } else if (data.status === 3) {
        // TODO 弹出框提示
        if (data.hasErrorFile) {
          // this.setState({
          //   errVisible: true
          // });
          Modal.error({
            title: intl.formatMessage(commonMessage.importFail),
            content: (
              <p>
                {intl.formatMessage(commonMessage.importError)}
                <a onClick={this.downloadFileBolb} style={{ textDecoration: 'underline' }}>
                  {intl.formatMessage(commonMessage.download)}
                </a>
                {intl.formatMessage(commonMessage.check)}
              </p>
            )
          });
        } else {
          message.error(data.errorMessage);
        }
        this.props.spinRender(false, '');
        clearInterval(this.searchStatus);
      }
    });
  }

  // 下载文件流
  downloadFileBolb = () => {
    const { missionId } = this.state;
    downfile(missionId).then(({ headers, data }) => {
      this.downFile(headers, data);
    });
  }

  downFile = (headers, data) => {
    const [, file] = decodeURIComponent(headers['content-disposition']).split(
      'filename='
    );
    if (window.navigator.msSaveOrOpenBlob) {
      // Internet Explorer
      window.navigator.msSaveOrOpenBlob(
        new Blob([data], { type: XLSX_TYPE }),
        file
      );
    } else {
      const blob = new Blob([data], { type: XLSX_TYPE });
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

  closeModal = (refresh) => {
    if (refresh) {
      this.initTable(this.props);
    }
    this.setState({
      addVisible: false
    });
  }

  postModal = () => {
    this.setState({
      postVisible: true
    });
  }

  closePostModal = (refresh) => {
    if (refresh) {
      this.initTable(this.props);
    }
    this.setState({
      postVisible: false
    });
  }

  reRenderDeleteBtn = () => {
    const { selectedRows } = this.state;
    this.state.btnColumns.find((item) => item.key === 'delete').disabled = selectedRows.length === 0;
    this.forceUpdate();
  }

  handleOnChange = (e) => {
    this.setState({
      radio: e.target.value
    });
  }

  handleExport = () => {
    const { radio } = this.state;
    const { intl } = this.props;
    if (radio === 'some') {
      exportXls({
        all: false,
        companyId: this.props.chooseData.companyId,
        type: 'Person',
        ids: this.state.selectedRows.length > 0
          ? this.state.selectedRows.map((item) => item.id)
          : this.state.data.map((item) => item.id)
      }).then((res) => {
        this.setState({
          exportVisible: false,
          missionId: res.data.data.id,
          opState: 'exporting'
          // spin: true,
          // spinTip: intl.formatMessage(commonMessage.exporting)
        }, () => {
          this.props.spinRender(true, intl.formatMessage(commonMessage.exporting));
          this.importInterval();
        });
      });
    } else if (radio === 'all') {
      exportXls({
        type: 'Person',
        companyId: this.props.chooseData.companyId,
        all: true,
        keyword: this.props.chooseData.keyword,
        orgId: this.props.chooseData.id,
        ...this.state.exportFilter
      }).then((res) => {
        this.setState({
          exportVisible: false,
          missionId: res.data.data.id,
          opState: 'exporting'
          // spin: true,
          // spinTip: intl.formatMessage(commonMessage.exporting)
        }, () => {
          this.props.spinRender(true, intl.formatMessage(commonMessage.exporting));
          this.importInterval();
        });
      });
    }
  }

  handleExportCancle = () => {
    this.setState({
      exportVisible: false
    });
  }

  tableRows = (selectedRowKeys, selectedRows) => {
    this.setState({
      selectedRows,
      selectedRowKeys
    }, () => {
      this.reRenderDeleteBtn();
    });
  }

  updateColumns = (columns) => {
    // const tableWidth = columns.filter((item) => !item.hide).map((item) => item.width).reduce((a, b) => a + b);
    this.setState({
      columns
      // tableWidth: tableWidth + 60
    });
  }

  handleTableChange = (params) => {
    const { filters, pagination } = params;
    const { current, pageSize } = pagination;
    this.setState({
      exportFilter: filters,
      filteredInfo: filters
    }, () => {
      const search = Object.assign(this.state.exportFilter, {
        current,
        pageSize
      });
      this.initTable(this.props, search);
    });
  };

  render() {
    const {
      columns,
      addVisible,
      personStatus,
      postVisible,
      record,
      data,
      btnColumns,
      current,
      pageSize,
      exportVisible,
      outVisible,
      // tableWidth,
      filterParams,
      selectedRowKeys
    } = this.state;
    const { intl, buttonAuthority } = this.props;
    // 即没有岗位调离权限,又不具备删除权限, 又不具备编辑权限时,去掉操作列
    const flag = buttonAuthority.some((item) => item === 'deletePerson'
      || item === 'offPosition'
      || item === 'updatePerson'
    );
    if (!flag
      && columns.filter((item) => item.type === 'operation').length > 0
      && this.props.selectMenu === 'post'
    ) {
      columns.pop();
    }
    return (
      <div className={`${styles.tableBox} wrapTable`}>
        <SupTable
          ref={(ref) => { this.table = ref; }}
          operationBarTitle={this.props.chooseData.name}
          selectedRowKeys={selectedRowKeys}
          rowKey={(item) => item.id}
          onSelectItem={this.tableRows}
          btnColumns={btnColumns}
          columns={columns}
          dataSource={data}
          showSearchIcon={false}
          controlColumns
          updateColumns={this.updateColumns}
          onSearch={this.handleTableChange}
          filterParams={filterParams}
          size="middle"
          // scroll={{
          //   x: tableWidth
          // }}
          pagination={{
            total: this.state.total,
            current,
            pageSize
          }}
          onDoubleClick={(re) => {
            if (this.props.selectMenu === 'department' || (this.props.selectMenu === 'post' && buttonAuthority.includes('updatePerson'))) {
              this.edit(re);
            }
          }}
        />
        {
          addVisible ? (
            <AddPerson
              rootId={this.props.rootId}
              personStatus={personStatus}
              visible={addVisible}
              chooseData={record}
              closeModal={this.closeModal}
              treeData={this.props.chooseData}
              selectMenu={this.props.selectMenu}
            />
          ) : null
        }
        {
          postVisible ? (
            <PostTransfer
              rootId={this.props.rootId}
              treeData={this.props.chooseData}
              visible={postVisible}
              closePostModal={this.closePostModal}
            />
          ) : null
        }
        {
          outVisible ? (
            <PostTransferOut
              treeData={this.props.chooseData}
              companyId={this.props.companyId}
              visible={outVisible}
              staffData={record}
              closePostModal={() => {
                this.initTable(this.props);
                this.setState({ outVisible: false });
              }}
            />
          ) : null
        }
        <Modal
          className="exportModal"
          title={intl.formatMessage(commonMessage.export)}
          maskClosable={false}
          visible={exportVisible}
          width={400}
          bodyStyle={{
            padding: '28px 55px'
          }}
          onOk={this.handleExport}
          onCancel={this.handleExportCancle}
        >
          <Radio.Group onChange={this.handleOnChange} value={this.state.radio}>
            <Radio className={styles.radioStyle} value="some">
              {intl.formatMessage(commonMessage.exportSome)}
              {/* 当前所选 */}
            </Radio>
            <Radio className={styles.radioStyle} value="all">
              {intl.formatMessage(commonMessage.exportAll)}
              {/* 全部 */}
            </Radio>
          </Radio.Group>
        </Modal>
      </div>
    );
  }
}
