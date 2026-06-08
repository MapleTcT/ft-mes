import React from 'react';
import 'braft-editor/dist/index.css';
import _ from 'lodash';
import { Modal } from 'sup-ui';
import { injectIntl } from 'react-intl';
import BraftEditor from 'braft-editor';
import { getDetail } from 'root/services/messageCenter';
import commonMessage from 'root/common/messages';
import './styles.less';

@injectIntl
export default class Content extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      content: '',
      url: ''
    };
  }

  componentDidMount() {
    const { id, shardingTime, template, noticeProtocolId } = this.props.recordData;
    if (template) {
      this.setState({
        content: ['email', 'stationLetter'].includes(this.props.recordData.protocol_code) ? _.get(JSON.parse(template || {}), 'text', '') : template
      });
      return;
    }
    getDetail({ messageId: id, shardingTime, protocolId: noticeProtocolId }).then((res) => {
      let str = _.get(res, 'data.data.content', '');
      const url = _.get(res, 'data.data.url', '');
      if (window.ActiveXObject || 'ActiveXObject' in window) {
        str = str.replace(/a>/g, 'span>').replace(/<a/g, '<span');
      }
      this.setState({
        content: str,
        url
      });
    });
  }

  onCancel = () => {
    this.props.closeContent();
  }

  render() {
    const { visible, intl } = this.props;
    const { content, url } = this.state;
    return (
      <Modal
        title={intl.formatMessage(commonMessage.content)}
        visible={visible}
        destroyOnClose
        width={706}
        footer={null}
        onCancel={this.onCancel}
        maskClosable={false}
        bodyStyle={{
          padding: 0
        }}
      >
        <div className="contentDetail">
          {
            url ? (
              <iframe
                frameBorder="0"
                style={{ width: '706px', height: '400px' }}
                // src={this.getUrl()}
                src={url}
                title="iframe"
              />
            ) : (
              <div
                style={{
                  width: 706,
                  height: 360,
                  overflowY: 'auto'
                }}
              >
                <BraftEditor
                  readOnly
                  value={BraftEditor.createEditorState(content)}
                  contentStyle={{ height: '100%', padding: 20, fontSize: 12 }}
                  controls={[]}
                />
              </div>
            )
          }
        </div>
      </Modal>
    );
  }
}
