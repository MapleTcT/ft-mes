import React, { Component } from 'react';
import _ from 'lodash';
import classNames from 'classnames';
// eslint-disable-next-line
import { Button, Tooltip, Select, Popover, Tabs, Dropdown, Menu, Icon, AutoComplete, Input, Row, Col } from 'sup-ui';
import Modal from './Modal/CommonModal';
import { fontSizeItems, fontFamilyItems } from '../utils/constants.js';
// import commonMessage from './commonMessages';
import ColorPicker from './ColorPicker';
import styles from './Reporter.less';
import { reportBar } from './ReportMenuBarConfig';
import message from './messages';

const barIconList = ['merge', 'autoWordWrap', 'formatCells', 'basicFunc',
  'filter', 'sort', 'cellSetting', 'rowCol', 'frozenPane', 'analyze', 'insertPicture', 'bcQRCode', 'bcEan13'];
const { Option } = Select;
// const { TabPane } = Tabs;
const { SubMenu } = Menu;

class ReportMenuBar extends Component {
  constructor(props) {
    super(props);
    const borderArr = [
      [
        { name: 'outside', icon: 'iconOutsideBorder', title: props.intl.formatMessage(message.OutsideBorder) },
        { name: 'inside', icon: 'iconInsideBorder', title: props.intl.formatMessage(message.InsideBorder) },
        { name: 'all', icon: 'iconAllBorder', title: props.intl.formatMessage(message.AllBorder) }
      ],
      [
        { name: 'left', icon: 'iconLeftBorder', title: props.intl.formatMessage(message.LeftBorder) },
        { name: 'innerVertical', icon: 'iconInnerVertical', title: props.intl.formatMessage(message.InnerVertical) },
        { name: 'right', icon: 'iconRightBorder', title: props.intl.formatMessage(message.RightBorder) }
      ],
      [
        { name: 'top', icon: 'iconTopBorder', title: props.intl.formatMessage(message.TopBorder) },
        { name: 'innerHorizontal', icon: 'iconInnerHorizontal', title: props.intl.formatMessage(message.InnerHorizontal) },
        { name: 'bottom', icon: 'iconBottomBorder', title: props.intl.formatMessage(message.BottomBorder) }
      ],
      [
        { name: 'diagonalUp', icon: 'iconDiagonalUp', title: props.intl.formatMessage(message.DiagonalUp) },
        { name: 'diagonalDown', icon: 'iconDiagonalDown', title: props.intl.formatMessage(message.DiagonalDown) },
        { name: 'none', icon: 'iconNoneBorder', title: props.intl.formatMessage(message.NoneBorder) }
      ]
    ];
    this.borderContent = (
      <div style={{ cursor: 'pointer' }}>
        {
          _.map(borderArr, (row, index) => {
            return (
              <div key={index}>
                {
                  _.map(row, (item, i) => {
                    return (
                      <Tooltip placement="bottom" title={item.title} arrowPointAtCente mouseLeaveDelay={0} key={i}>
                        <i className={`${styles[item.icon]} ${styles.borderIcon}`} onClick={this.setBorder.bind(this, item.name)} />
                      </Tooltip>
                    );
                  })
                }
              </div>
            );
          })
        }
      </div>
    );
    this.state = {
      fontSize: '11',
      fontFamily: 'Calibri',
      underline: false,
      fontColor: 'rgb(0, 0, 0)',
      backColor: 'rgb(255, 255, 255)',
      fontWeight: 'normal',
      fontStyle: 'normal',
      hAlign: 'general',
      vAlign: 'center',
      wordWrap: false
    };
  }

  componentDidUpdate() {
    this.updateTransaction();
  }

  setBorder = (borderType) => {
    this.props.basicOperate({ opt: 'setBorderLines', options: { borderType } });
  }

  getOptions = (array) => {
    return array.map((item) => {
      return <Option key={item} value={item}>{item}</Option>;
    });
  }

  setStateValue = ({
    fontSize = '11',
    fontFamily = 'Calibri',
    textDecoration = {},
    fontColor = 'rgb(0, 0, 0)',
    backColor = 'rgb(255, 255, 255)',
    fontWeight = 'normal',
    fontStyle = 'normal',
    hAlign = 'general',
    vAlign = 'center',
    wordWrap = false
  }) => {
    this.setState({
      fontSize,
      fontFamily,
      underline: textDecoration.underline,
      fontColor,
      backColor,
      fontWeight,
      fontStyle,
      hAlign,
      vAlign,
      wordWrap
    });
  }

  getCurrentFontSizePos = (opt, value) => {
    let fontSizePos = fontSizeItems.indexOf(value);
    if (fontSizePos === -1) {
      const { length } = fontSizeItems;
      if (Number(value) > Number(fontSizeItems[length - 1])) {
        fontSizePos = length;
      } else if (Number(value) < Number(fontSizeItems[0])) {
        fontSizePos = -1;
      } else {
        const ceilingSize = fontSizeItems.find((item) => Number(item) > Number(value));
        fontSizePos = ceilingSize === undefined ? length - 1 : fontSizeItems.indexOf(ceilingSize);
        if (opt === 'up' && fontSizePos) fontSizePos -= 1;
      }
    }

    return fontSizePos;
  }

  updateTransaction = () => {
    this.canUndo(this.props.spread);
    this.canRedo(this.props.spread);
  }

  updateStatus = (info) => {
    this.setStateValue({
      ...info.fontStyles,
      ...info.alignment,
      backColor: info.bgColor
    });
  }

  changeStyles = ({ state, prop, opt }, value) => {
    if (state === 'fontSize') {
      this.setState({
        showFontSizeItems: false
      });
      if (opt) {
        const fontSizePos = this.getCurrentFontSizePos(opt, this.state.fontSize);

        if ((opt === 'up' && fontSizePos + 1 >= fontSizeItems.length) || (opt === 'down' && fontSizePos <= 0)) return;
        value = opt === 'up' ? fontSizeItems[fontSizePos + 1] : fontSizeItems[fontSizePos - 1];
      } else if (Number(value) > 409 || Number(value) < 0) {
        Modal.warning({ content: this.props.intl.formatMessage(message.WarningRule) });
        return;
      }
    }
    const stateObj = {};
    stateObj[state] = value;
    this.setState(stateObj);

    if (!value) return;
    this.props.basicOperate({ opt: 'setFontStyle', options: { prop, value } });
  }

  changeAlign = ({ prop, value }) => {
    const stateObj = {};
    stateObj[prop] = value;
    this.setState(stateObj);
    this.props.basicOperate({ opt: 'setAlignment', options: { prop, value } });
  }

  changeColor = (prop, value) => {
    this.props.basicOperate({ opt: 'setColor', options: { prop, value } });
  }

  canUndo = (spread) => {
    const undo = (spread && spread.undoManager().canUndo()) || false;
    if (this.state.undo !== undo) {
      this.setState({ undo });
    }
  }

  canRedo = (spread) => {
    const redo = (spread && spread.undoManager().canRedo()) || false;
    if (this.state.redo !== redo) {
      this.setState({ redo });
    }
  }

  // 类似粘贴操作模块  starting
  menuOpt = (opt, e) => {
    switch (opt) {
      case 'clear': this.props.basicOperate({ opt: 'doClear', options: { type: e.key } }); break;
      case 'paste': this.props.basicOperate({ opt: 'paste', type: e.key }); break;
      case 'frozenPane': this.props.frozenPane(e.key); break;
      case 'sort': this.props.basicOperate({ opt: 'sort', options: { type: e.key } }); break;
      case 'row':
      case 'col':
      case 'rowCol': this.props.setRowColOptions(e.key); break;
      case 'basicFunc': this.props.basicOperate({ opt: 'basicFunc', options: { func: e.key } }); break;
      case 'formatCells': this.props.formatCells(e.keyPath); break;
      case 'analyze': this.props.showAnalyzeModal(true); break;
      case 'filter': this.props.basicOperate({ opt: 'filter', options: { type: e.key } }); break;
      default: break;
    }
  }

  dispatchBarEvent = (eventType, prop) => {
    switch (eventType) {
      case 'cellSetting': this.props.basicOperate({ opt: 'cellSettings' }); break;
      case 'merge': this.props.basicOperate({ opt: 'onMerge' }); break;
      // case 'filter': this.props.basicOperate({ opt: 'showFilter' }); break;
      case 'wordWrap': this.props.basicOperate({ opt: 'wordWrap' }); break;
      case 'brush': this.props.format({ opt: 'click' }); break;
      case 'doubleClickBrush': this.props.format({ opt: 'double' }); break;
      case 'onOpen': this.props.onOpen(prop); break;
      case 'onExport': this.props.onExport(); break;
      case 'onSave': this.props.onSave(); break;
      case 'onPrint': this.props.onPrint(); break;
      case 'undo': this.props.basicOperate({ opt: 'undo' }); break;
      case 'redo': this.props.basicOperate({ opt: 'redo' }); break;
      case 'cut': this.props.basicOperate({ opt: 'cut' }); break;
      case 'copy': this.props.basicOperate({ opt: 'copy' }); break;
      case 'setFontStyle': this.props.basicOperate({ opt: 'setFontStyle', options: { prop } }); break;
      case 'textDecoration': this.props.basicOperate({ opt: 'setTextDecoration' }); break;
      case 'changeStyles': this.changeStyles({ state: 'fontSize', prop: 'font-size', opt: prop }); break;
      case 'unfoldDataSourceSet': this.props.unfoldDataSourceSet(); break;
      case 'onExit': this.props.showOrHideModal({ closeDesignerVisible: true }); break;
      case 'addChart': this.props.addChart(prop); break;
      case 'onOpenChartModal': this.props.onOpenChartModal(); break;
      case 'frozenPaneCheck': this.props.frozenPaneCheck(); break;
      case 'onOpenQrBarCode': this.props.onOpenBarCodeByType(true); break;
      case 'onOpenBarCode': this.props.onOpenBarCodeByType(false); break;
      default: break;
    }
  }

  renderColorPicker = (type) => {
    const item = reportBar(this.props.intl)[type];
    return (
      <Col span={12} key={type}>
        <Tooltip key={type} placement="bottom" title={item.barTitle} arrowPointAtCente mouseLeaveDelay={0}>
          <div
            className={styles.menuBarColorPicker}
            onClick={(e) => this[type].triggerClick(e)}
          >
            <i className={`${styles[item.barIcon]} ${styles.iconMini}`} />
            <ColorPicker
              ifTriangle={false}
              name={type}
              ref={(color) => { this[type] = color; }}
              borderRadius={0}
              colorWidth={20}
              colorHeight={4}
              edit={this.changeColor.bind(this, item.eventProp)}
              value={this.state[item.barState]}
            />
          </div>
        </Tooltip>
      </Col>
    );
  }

  renderBasicItem = (type) => {
    const item = reportBar(this.props.intl)[type];
    const disabled = item.disabledState ? !this.state[item.disabledState] : false;
    const isAlignItem = ['vAlign', 'hAlign'].includes(item.selectedState);
    const style = item.useAwesomeIcon ? `fa ${item.barIcon}` : `${styles[item.barIcon]} ${styles[item.barIconStyle]}`;
    let btnStyle = `${styles.flexRow} ${styles.flexCenter}`;
    if (item.selectedState) {
      let selected = this.state[item.selectedState];
      if (item.defaultState) {
        selected = isAlignItem
          ? this.state[item.selectedState] === item.defaultState
          : this.state[item.selectedState] !== item.defaultState;
      }
      btnStyle = selected ? styles.selectedBgColor : '';
    }

    return item.useSpan ? (
      <Button
        key={type}
        className={btnStyle}
        style={item.btnStyle}
        onClick={this.dispatchBarEvent.bind(this, item.barEvent)}
      >
        <i className={style} />
        <span>{item.barTitle}</span>
      </Button>
    ) : (
      <Tooltip key={type} placement="bottom" title={item.barTitle} arrowPointAtCente mouseLeaveDelay={0}>
        <Button
          className={btnStyle}
          style={item.btnStyle}
          disabled={disabled}
          onClick={() => {
            if (isAlignItem) {
              this.changeAlign({ prop: item.selectedState, value: item.defaultState });
            } else {
              this.dispatchBarEvent(item.barEvent, item.eventProp);
            }
          }}
        >
          {
            item.isSupIcon ? (
              <Icon type={item.barIcon} className={styles.barIconStyle} />
            ) : (
              <i className={style} />
            )
          }
        </Button>
      </Tooltip>
    );
  }

  renderBarItem = (type) => {
    const item = reportBar(this.props.intl)[type];
    const selectedState = item.selectedState && this.state[item.selectedState];
    const selectedProps = item.selectedProps && this.props[item.selectedProps];
    const disabledProps = (item.disabledProps && this.props[item.disabledProps]) === false;
    const disabled = item.disabledState ? !this.state[item.disabledState] : false;
    const className = selectedState || selectedProps ? styles.selectedBgColor : '';
    const style = item.useAwesomeIcon ? `fa ${item.barIcon} ${styles.barIcon}` : `${styles[item.barIcon]} ${styles.icon}`;
    const content = <Button
      key={type}
      style={{ padding: '0 6px' }}
      className={`${styles.menuBarIcon} ${className}`}
      onClick={this.dispatchBarEvent.bind(this, item.barEvent, item.eventProp)}
      onDoubleClick={this.dispatchBarEvent.bind(this, item.barDoubleClickEvent)}
      disabled={disabledProps || disabled}
    >
      {
        item.barMenu ? (
          <Dropdown
            overlay={this.renderBarMenu(type, item.barMenu)}
            trigger={['click']}
          >
            <div style={{ display: 'flex', flexDirection: 'column' }}>
              <i className={style} />
              <a
                className={disabledProps ? styles.disabled : ''}
                href="#"
              >
                {item.barTitle}
                <Icon type="down" />
              </a>
            </div>
          </Dropdown>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column' }}>
            <i className={style} />
            <span>{item.barTitle}</span>
          </div>
        )
      }
    </Button>
    if (['bcEan13', 'bcQRCode'].includes(type)) {
      return (
        <Tooltip key={type} placement="bottom"
          title={
            <div>
              {item.barTitle}
              <div>{item.barTip}</div>
            </div>
          }
          arrowPointAtCente mouseLeaveDelay={0}>
          {content}
        </Tooltip>
      );
    }
    return content
  }

  renderBarMenu = (menuType, menu) => {
    return (
      <Menu onClick={this.menuOpt.bind(this, menuType)} className={styles.menuItem}>
        {this.renderBarMenuList(menu)}
      </Menu>
    );
  }

  renderBarMenuList = (menu) => {
    return _.map(menu, (menuItem) => {
      const { children, key, className, name, displayProps } = menuItem;
      return children ? (
        <SubMenu
          key={key}
          title={
            <span>
              <i className={`${className ? styles[className] : ''}  ${styles.iconMenu}`} />
              <span>
                {name}
              </span>
            </span>
          }
        >
          {this.renderBarMenuList(children)}
        </SubMenu>
      ) : (
        <Menu.Item
          key={key}
          style={{ display: !displayProps || this.props[displayProps] ? 'flex' : 'none' }}
        >
          <i className={className ? `${styles[className]} ${styles.iconSmall} ` : ''} />
          {menuItem.name}
        </Menu.Item>
      );
    });
  }
  // 类似粘贴操作模块  ending

  render() {
    const { intl } = this.props;
    return (
      <div className={styles.flexColumn}>
        <div className={styles['report-menu-tabs-box']}>
          {/* <Tabs
            type="card"
            className={styles.menubarTabs}
            style={{ height: 114, overflow: 'visible' }}
          >
            <TabPane tab={intl.formatMessage(message.Start)} key="1"> */}
          <div className={styles.menuBar}>
            <div className={styles.flexColumn}>
              <div className={styles.flexRow}>
                {
                  _.map(['open', 'export', 'print'], (item) => {
                    return this.renderBasicItem(item);
                  })
                }
              </div>
              <div className={classNames(styles.flexRow, styles['undo-redo-save-box'])}>
                {
                  _.map(['undo', 'redo', 'save'], (item) => {
                    return this.renderBasicItem(item);
                  })
                }
              </div>
            </div>
            {this.renderBarItem('paste')}
            <div className={styles.flexColumn}>
              {
                _.map(['cut', 'copy'], (item) => {
                  return this.renderBasicItem(item);
                })
              }
            </div>
            {this.renderBarItem('brush')}
            <div className={styles.flexColumn}>
              <Row justify="center" style={{ width: 280 }}>
                <Col span={12}>
                  <Select
                    style={{ width: '98%', marginRight: 3 }}
                    onChange={this.changeStyles.bind(this, { state: 'fontFamily', prop: 'font-family' })}
                    value={this.state.fontFamily}
                  >
                    {this.getOptions(fontFamilyItems)}
                  </Select>
                </Col>
                <Col span={7}>
                  <AutoComplete
                    backfill
                    dataSource={fontSizeItems}
                    value={this.state.fontSize}
                    onChange={this.changeStyles.bind(this, { state: 'fontSize', prop: 'font-size' })}
                    filterOption={() => this.state.showFontSizeItems || false}
                  >
                    <Input suffix={<Icon
                      type="down"
                      onClick={() => {
                        this.setState({
                          // eslint-disable-next-line
                          showFontSizeItems: !this.state.showFontSizeItems
                        });
                      }}
                    />}
                    />
                  </AutoComplete>
                </Col>
                <Col span={5}>
                  <div className={styles.flexRow}>
                    {
                      _.map(['sizePlus', 'sizeMinus'], (item) => {
                        return this.renderBasicItem(item);
                      })
                    }
                  </div>
                </Col>
              </Row>
              <Row style={{ width: 280 }}>
                <Col span={8}>
                  <div className={styles.flexRow}>
                    {
                      _.map(['bold', 'italic', 'underline'], (item) => {
                        return this.renderBasicItem(item);
                      })
                    }
                  </div>

                </Col>
                <Col span={16}>
                  <Row type="flex" justify="space-around" align="middle">
                    <Col span={6}>
                      <Popover content={this.borderContent}>
                        <Button style={{ paddingTop: 4 }}>
                          <i className={`${styles.iconAllBorder} ${styles.iconAlign}`} />
                        </Button>
                      </Popover>
                    </Col>
                    <Col span={12}>
                      <Row type="flex" justify="space-around">
                        {
                          _.map(['bgColor', 'fontColor'], (item) => {
                            return this.renderColorPicker(item);
                          })
                        }
                      </Row>
                    </Col>
                    <Col span={6}>
                      <Tooltip placement="bottom" title={intl.formatMessage(message.Clear)} arrowPointAtCente mouseLeaveDelay={0}>
                        <Dropdown
                          overlay={this.renderBarMenu('clear', reportBar(this.props.intl).clear.barMenu)}
                          trigger={['click']}
                        >
                          <Button style={{ padding: '0 7px' }}>
                            <i className={`${styles.iconClear} ${styles.iconMini}`} />
                            <Icon type="down" />
                          </Button>
                        </Dropdown>
                      </Tooltip>
                    </Col>
                  </Row>
                </Col>
              </Row>
            </div>
            <div className={styles.flexRow} style={{ overflow: 'auto' }}>
              <div className={styles.flexColumn}>
                <div className={styles.flexRow}>
                  {
                    _.map(['vAlignTop', 'vAlignCenter', 'vAlignBottom'], (item) => {
                      return this.renderBasicItem(item);
                    })
                  }
                </div>
                <div className={styles.flexRow}>
                  {
                    _.map(['hAlignLeft', 'hAlignCenter', 'hAlignRight'], (item) => {
                      return this.renderBasicItem(item);
                    })
                  }
                </div>
              </div>
              <div style={{ overflowX: 'auto', overflowY: 'hidden' }}>
                <Row>
                  <Col span={23}>
                    <div className={styles.flexRow}>
                      {
                        _.map(barIconList, (barItem) => {
                          return this.renderBarItem(barItem);
                        })
                      }
                    </div>
                  </Col>
                  <Col span={1} />
                </Row>
              </div>
            </div>
          </div>
          {/* </TabPane> */}
          {/* <TabPane tab={intl.formatMessage(commonMessage.insert)} key="2">
              <div className={styles.menuBar}>
                <div className={styles.flexRow}>
                  {
                    _.map(['insertChart'], (barItem) => {
                      return this.renderBarItem(barItem);
                    })
                  }
                </div>
                <div className={styles.flexColumn}>
                  <div className={styles.flexRow}>
                    {
                      _.map(['pieChart', 'lineChart'], (item) => {
                        return this.renderBasicItem(item);
                      })
                    }
                  </div>
                  <div className={classNames(styles.flexRow, styles['undo-redo-save-box'])}>
                    {
                      _.map(['columnChart', 'barClustered'], (item) => {
                        return this.renderBasicItem(item);
                      })
                    }
                  </div>
                </div>
                <div className={styles.flexRow}>
                  {
                    _.map(['row', 'col', 'insertPicture', 'insertDateSource'], (barItem) => {
                      return this.renderBarItem(barItem);
                    })
                  }
                </div>
              </div>
            </TabPane>
            <TabPane tab={intl.formatMessage(message.Folder)} key="3">
              <div className={styles.menuBar}>
                <Row>
                  <Col>
                    <div className={styles.flexRow}>
                      {
                        _.map(['openFile', 'saveFile', 'exportFile', 'printBig', 'exit'], (barItem) => {
                          return this.renderBarItem(barItem);
                        })
                      }
                    </div>
                  </Col>
                </Row>
              </div>
            </TabPane>
            <TabPane tab={intl.formatMessage(message.Edit)} key="4">
              <div className={styles.menuBar}>
                <Row>
                  <Col>
                    <div className={styles.flexRow}>
                      {
                        _.map(['undoBig', 'redoBig', 'cutBig', 'copyBig', 'paste'], (barItem) => {
                          return this.renderBarItem(barItem);
                        })
                      }
                    </div>
                  </Col>
                </Row>
              </div>
            </TabPane>
            <TabPane tab={intl.formatMessage(message.Format)} key="5">
              <div className={styles.menuBar}>
                <Row>
                  <Col>
                    <div className={styles.flexRow}>
                      {
                        _.map(['merge', 'formatCells', 'cellSetting'], (barItem) => {
                          return this.renderBarItem(barItem);
                        })
                      }
                    </div>
                  </Col>
                </Row>
              </div>
            </TabPane> */}
          {/* </Tabs> */}
        </div>
      </div>
    );
  }
}

export default ReportMenuBar;
