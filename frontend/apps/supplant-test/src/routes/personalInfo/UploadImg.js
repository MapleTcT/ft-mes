import React from 'react';
import { injectIntl } from 'react-intl';
import axios from 'axios';
import { Upload, Icon, Modal, message } from 'sup-ui';
import ImgCrop from 'sup-img-crop';
import styles from './style.less';
import commonMessage from './messages';
import { getImg } from '../../services/changePass';
import defaultImg from '../../assets/img/mrtx.png'

@injectIntl
export default class UploadImg extends React.PureComponent {
  constructor(props) {
    super(props);
    this.state = {
      fileList: props.fileList || [],
      uploading: false
    };
  }

  componentWillMount() {
    const { value } = this.props;
    if (value) {
      const name = value.substring(value.lastIndexOf('/') + 1);
      getImg([value]).then((res) => {
        const { data } = res.data || {};
        if (!data[value]) return '';
        return `data:image/png;base64,${data[value]}`;
      }).then((data) => {
        if (!data) this.setState({ fileList: [{ name, uid: 1, status: 'error' }] });
        this.setState({ previewImage: data, fileList: [{ name, uid: -1, url: data }] });
      });
    } else {
      const name = defaultImg.substring(defaultImg.lastIndexOf('/') + 1);
      this.setState({ previewImage: defaultImg, fileList: [{name, uid: -1, url: defaultImg}]})
    }
  }

  /* eslint-disable */
  getBase64 = (file) => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.readAsDataURL(file);
      reader.onload = () => resolve(reader.result);
      reader.onerror = (error) => reject(error);
    });
  }

  handleChange = (obj) => {
    const { fileList, event } = obj;
    const { onChange } = this.props;
    console.log(fileList, obj);
    if (fileList.length) {
      let curList = fileList[0];
      const { status, uid, name, response = {} } = curList;
      switch (status) {
        case 'error':
          curList = { uid, name, status: 'error' };
          message.error(response.message);
          break;
        case 'uploading':
          if (event) curList.percent = event.percent;
          this.setState({ uploading: true });
          break;
        case 'done': case 'success':
          onChange(response.data || '');
          this.setState({ uploading: false });
          break;

        default:
          break;
      }
      this.setState({ fileList: [curList] });
    }
  };

  handlePreview = async (file) => {
    if (!file.url && !file.preview) {
      file.preview = await this.getBase64(file.originFileObj);
    }

    this.setState({
      previewImage: file.url || file.preview,
      previewVisible: true,
      previewTitle: file.name || file.url.substring(file.url.lastIndexOf('/') + 1)
    });
  };

  beforeUpload = (file) => {
    const maxLt = file.size / 1024 / 1024 <= 2;
    if (!maxLt) {
      message.error(this.props.intl.formatMessage(commonMessage.imgSizelimit));
    }
    return maxLt;
  }

  setCurFileList = (params = {}) => {
    const curList = this.state.fileList[0] || {};
    return [{ ...curList, ...params }];
  }

  render() {
    const { width, height, onChange } = this.props;
    const { fileList, previewVisible, previewTitle, previewImage, uploading } = this.state;
    return (
      <div style={{ width: `${width}px`, height: `${height}px`, position: 'relative' }}>
        <ImgCrop rotate aspect={width / height}>
          <Upload
            accept=".bmp,.jpg,.png"
            // action="/inter-api/organization/v1/persons/image"
            // headers={{ Authorization: `Bearer ${localStorage.getItem('ticket')}` }}
            customRequest={(res) => {
              try {
                const { file } = res;
                // 判断图片尺寸
                if (!(this.beforeUpload(file))) return false;
                const formData = new FormData();
                this.sourceToken = axios.CancelToken.source();
                formData.append('file', file);
                uploadImg({
                  data: formData,
                  cancelToken: this.sourceToken.token,
                  onUploadProgress: (e) => {
                    const percentage = Math.round((e.loaded * 100) / e.total) || 0;
                    this.handleChange({ event: { percent: percentage }, fileList: this.state.fileList });
                  }
                }).then((r) => {
                  getImg([r.data.data]).then(({ data: { data: base64 } }) => {
                    this.handleChange({ fileList: this.setCurFileList({ response: r.data, status: 'done', thumbUrl: `data:image/png;base64,${base64[r.data.data]}` }) });
                  });
                  this.setState({ uploading: false });
                }).catch((error = {}) => {
                  if (error.data && this.state.fileList.length) {
                    this.handleChange({ fileList: this.setCurFileList({ status: 'error', response: error.data || {} }) });
                  }
                });
              } catch (error) {

              }
            }}
            beforeUpload={this.beforeUpload}
            maxCount={1}
            disabled={true}
            showUploadList={fileList.length}
            listType="picture-card"
            fileList={fileList}
            onPreview={this.handlePreview}
            onChange={this.handleChange}
            className={`sup-upload ${height < 90 ? 'small' : ''}`}
            iconRender={() => { return <Icon type="delete" />; }}
            onProgress={(e) => {
              this.handleChange({
                event: e,
                fileList: this.state.fileList
              });
            }}
            style={{ background: '#e9edf1', border: 'none' }}
            onRemove={() => {
              this.setState({ fileList: [] });
              onChange('');
            }}
          >
            {fileList.length ? null : <Icon type="plus" style={{ fontSize: '40px', color: '#fff' }} />}
          </Upload>
        </ImgCrop>
        {
          uploading && (
            <Icon
              onClick={() => {
                if (this.sourceToken) this.sourceToken.cancel();
                this.setState({ fileList: [], uploading: false });
              }}
              className={styles['upload-close-icon']}
              type="error-circle"
              theme="filled"
              style={{ fontSize: '16px' }}
            />
          )
        }
        <Modal
          visible={previewVisible}
          title={previewTitle}
          footer={null}
          onCancel={() => { this.setState({ previewVisible: false }); }}
        >
          <img alt="example" style={{ width: '100%' }} src={previewImage} />
        </Modal>
      </div>
    );
  }
}

UploadImg.defaultProps = {
  width: 96,
  height: 96,
  onChange: () => { }
};
