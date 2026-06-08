import React from 'react';
// import Cropper from 'react-cropper';
import { Layout, Progress, Radio, Icon, Tag, message, Modal } from 'sup-ui';
import Viewer from 'react-viewer';
import { injectIntl } from 'react-intl';
import axios from 'axios';
import PropTypes from 'prop-types';
import { throttle } from 'lodash';
// import ReactZmage from 'react-zmage';
import classnames from 'classnames';
import commonMessage from './message';
import styles from './styles.less';
import logoImg from '../../assets/img/logo.png';
import errorImg from '../../assets/img/picture-failed.png';
import THEMECONFIG from './constant.js';
import { getUpload } from '../../services/themeSetting';

const { Header, Content } = Layout;
let hostPrex = '';
if (process.env.NODE_ENV !== 'production') {
  hostPrex = 'http://10.30.44.61:9011';
}

@injectIntl
export default class ThemeOption extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      active: 0,
      initActive: 0,
      data: props.data,
      percent: 0,
      imgView: false,
      curImgsrc: logoImg
    };
    this.formatMessage = props.intl.formatMessage;
  }

  componentWillMount() {
    const { data = [], onRef } = this.props;
    if (onRef) onRef(this);
    if (data) {
      data.forEach((item, index) => {
        if (item.status === 1) {
          this.setState({ active: index, initActive: index });
        }
      });
    }
  }

  // eslint-disable-next-line camelcase
  UNSAFE_componentWillReceiveProps(nextProps) {
    const { data } = this.props;
    const { data: nextData = [] } = nextProps;
    if (data !== nextData) {
      nextData.forEach((item, index) => {
        if (item.status === 1) {
          this.setState({ active: index, initActive: index });
        }
      });
      this.setState({ data: nextData });
    }
  }

  renderChooseTheme = () => {
    const { data, initActive } = this.state;
    const { showTag } = this.props;
    return (
      <>
        {data.map((item, i) => {
          const isActive = item.status === 1;
          const curTheme = THEMECONFIG[item.theme];
          return (
            <div
              className={classnames(
                styles['theme-list'],
                isActive ? styles.active : ''
              )}
              key={item.theme}
              onClick={() => {
                // this.setState({ data[index].status: 1 });
                const curData = data.map((el, j) => {
                  if (i === j) return { ...el, status: 1 };
                  return { ...el, status: 0 };
                });
                this.setState({ data: curData, active: i });
              }}
            >
              <img
                className={styles['theme-list-img']}
                alt={this.formatMessage(commonMessage[curTheme.intl])}
                src={curTheme.img}
              />
              <div className={styles['theme-selected']}>
                {isActive && (
                  <Icon
                    className={styles['theme-selected-icon']}
                    type="tick-circle"
                    theme="filled"
                  />
                )}
              </div>
              {initActive === i && showTag && (
                <Tag className={styles['theme-selected-tag']} color="#FA6400">
                  {this.formatMessage(commonMessage.tag)}
                </Tag>
              )}
              <p className={styles['theme-list-title']}>
                {/* {curTheme.title} */}
                {this.formatMessage(commonMessage[curTheme.intl])}
                {initActive === i && !showTag
                  ? `（${this.formatMessage(commonMessage.theme_selected)}）`
                  : ''}
              </p>
            </div>
          );
        })}
      </>
    );
  };

  judgeImgSize = (file) => {
    const reader = new FileReader();
    const img = new Image();
    const { formatMessage } = this;
    reader.readAsDataURL(file);
    return new Promise((resolve) => {
      reader.onload = (res) => {
        img.src = res.target.result;
        return new Promise(() => {
          img.onload = function () {
            const limitH = 48;
            const limitW = 180;
            const { width } = this;
            const { height } = this;
            // if (width / height !== limitW / limitH) {
            if (width !== limitW || height !== limitH) {
              message.error(
                formatMessage(commonMessage.message_upload_img_error, {
                  limitW,
                  limitH
                })
                // `尺寸错误，请上传宽度为${limitW}像素且高度为${limitH}像素的图片`
              );
              resolve(false);
            }
            resolve(true);
          };
        });
      };
    });
  };

  uploadImg = async (file) => {
    const { data, active } = this.state;
    const imgType = ['jpg', 'png', 'jpeg'];
    if (!file) return;
    const { type } = file;
    if (!type || !imgType.includes(type.split('/')[1].toLowerCase())) {
      message.error(this.formatMessage(commonMessage.message_upload_warning));
      return;
    }
    // 判断图片尺寸
    if (!(await this.judgeImgSize(file))) return false;
    this.setState({ showUpload: true });
    const formData = new FormData();
    this.sourceToken = axios.CancelToken.source();
    formData.append('file', file);
    formData.append('theme', data[active].theme);
    getUpload({
      data: formData,
      cancelToken: this.sourceToken.token,
      onUploadProgress: (e) => {
        const percentage = Math.round((e.loaded * 100) / e.total) || 0;
        this.setState({ percent: percentage });
      }
    }).then((res) => {
      const { filePath } = res.data;
      if (filePath) {
        setTimeout(() => {
          // 更新数据源
          const newData = data.map((item, i) => {
            if (i === active) return { ...item, logo: filePath };
            return item;
          });
          this.setState({ showUpload: false, showMask: false, data: newData });
          message.success(
            this.formatMessage(commonMessage.message_upload_success)
          );
        }, 1000);
      } else {
        message.error(this.formatMessage(commonMessage.message_upload_error));
      }
    });
  };

  renderChooseLogo = () => {
    const { showMask, showUpload, data, active, percent } = this.state;
    const curLog = data[active];
    return (
      <div style={{ paddingBottom: '10px' }}>
        <h4 className={styles.subtitle}>
          {this.formatMessage(commonMessage.previmg_title)}
        </h4>
        <div
          style={{
            fontSize: '13px',
            background: `${THEMECONFIG[`${curLog.theme}`].menuColor}`
          }}
          className={styles['logo-upload']}
          onMouseLeave={throttle(() => {
            this.setState({ showMask: false });
          }, 300)}
        >
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              height: '100%'
            }}
            onMouseEnter={throttle(() => {
              this.setState({ showMask: true, showUpload: false });
            }, 300)}
          >
            <img
              className={classnames(showMask ? styles['logo-img'] : '')}
              style={{ height: '100%', width: '100%' }}
              src={`${hostPrex}${curLog.logo}` || logoImg}
              alt="logo"
              onError={(e) => {
                e.target.src = errorImg;
                e.target.onerror = null;
                e.target.style.width = 'auto';
                e.target.style.height = '80%';
              }}
            />
          </div>
          <div
            style={{ display: showMask ? 'block' : 'none' }}
            className={styles['logo-mask']}
          >
            {!showUpload && (
              <div className={styles['logo-btns']}>
                <span
                  className={styles['logo-btn']}
                  onClick={() => {
                    this.setState({
                      imgView: true,
                      curImgsrc: `${hostPrex}${curLog.logo}`
                    });
                  }}
                >
                  {this.formatMessage(commonMessage.previmg_btn_preview)}
                </span>
                <span className={styles['logo-btn']}>
                  <input
                    type="file"
                    name="picture"
                    id="picture"
                    title=""
                    className={styles['logo-file']}
                    onChange={(res) => {
                      const file = res.target.files[0];
                      // 上传图片
                      this.uploadImg(file);
                      // this.setState({ showUpload: true });
                    }}
                  />
                  {this.formatMessage(commonMessage.previmg_btn_upload)}
                </span>
              </div>
            )}
          </div>
          {showUpload && (
            <div className={styles['logo-progress']}>
              <Icon
                type="error-circle"
                theme="filled"
                style={{
                  fontSize: '20px',
                  color: '#fff',
                  cursor: 'pointer'
                }}
                onClick={() => {
                  if (this.sourceToken) this.sourceToken.cancel();
                  this.setState({ showUpload: false });
                }}
              />
              <Progress
                strokeWidth={3}
                strokeColor="#2E7CD9"
                showInfo={false}
                percent={percent}
                style={{ padding: '5px 15px 0' }}
              />
            </div>
          )}
        </div>
      </div>
    );
  };

  renderChooseFont = () => {
    const { checkFont } = this.props;
    const { data, active } = this.state;
    const { font } = data[active];
    let valueConfig = {};
    if (checkFont) valueConfig = { value: font };
    return (
      <Radio.Group
        onChange={(e) => {
          const { value } = e.target;
          const curData = data.map((item, i) => {
            if (active === i) return { ...item, font: value };
            return item;
          });
          this.setState({ data: curData });
        }}
        defaultValue={data[active].font}
        {...valueConfig}
      >
        <Radio style={{ marginRight: '20px' }} value={12}>
          <span style={{ fontSize: '12px' }}>
            {this.formatMessage(commonMessage.fontsize_12)}
          </span>
        </Radio>
        <Radio value={14}>
          <span style={{ fontSize: '14px' }}>
            {this.formatMessage(commonMessage.fontsize_16)}
          </span>
        </Radio>
      </Radio.Group>
    );
  };

  render() {
    const { data, imgView, curImgsrc } = this.state;
    const { config, showTag, buttons, title = '', showLoading } = this.props;
    let curThemeName = '';
    if (data) {
      const curTheme = data && data.filter((item) => item.status === 1);
      ({ intl: curThemeName } = THEMECONFIG[curTheme[0].theme] || {});
    }
    return (
      <>
        {data && (
          <>
            <Layout>
              <Header className={styles.headerBar}>{title}</Header>
              <Content style={{ background: '#fff', height: '100%' }}>
                <div className={styles.content}>
                  {config.map((item) => {
                    return (
                      <div className={styles['item-list']} key={item.title}>
                        <h3 className={styles.title}>
                          {!showTag && item.key === 'Logo'
                            ? this.formatMessage(commonMessage[curThemeName])
                              || ''
                            : ''}
                          {`${item.title || ''}`}
                        </h3>
                        <div className={styles['item-content']}>
                          {this[`renderChoose${item.key}`]()}
                        </div>
                      </div>
                    );
                  })}
                </div>
              </Content>
              <div className={styles['footer-btn']}>{buttons}</div>
            </Layout>
            <Viewer
              noNavbar
              customToolbar={(toolbar) => {
                const hideBtn = ['prev', 'next'];
                const newRes = toolbar.filter(
                  (item) => !hideBtn.includes(item.key)
                );
                return newRes;
              }}
              visible={imgView}
              onClose={() => {
                this.setState({ imgView: false });
              }}
              onMaskClick={() => {
                this.setState({ imgView: false });
              }}
              images={[{ src: curImgsrc }]}
            />
          </>
        )}
        <Modal
          centered
          width="270px"
          height="130px"
          visible={showLoading}
          closable={false}
          footer={null}
        >
          <div className={styles.loading}>
            <Icon
              style={{
                fontSize: '20px',
                color: '#0075DB',
                marginRight: '18px'
              }}
              type="loading-3-quarters"
            />
            {this.formatMessage(commonMessage.loading_message)}
          </div>
        </Modal>
      </>
    );
  }
}

ThemeOption.defaultProps = {
  showLoading: false
};

ThemeOption.propTypes = {
  showLoading: PropTypes.bool
};
