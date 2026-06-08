import React from 'react';
import { Layout } from 'sup-ui';
import { injectIntl } from 'react-intl';
import SupResize from 'sup-rc-resize';
import SupIcon from 'sup-rc-icon';
import EsignSider from './Sider';
import EsignContent from './Content';
import style from './style.less';
import 'sup-rc-resize/dist/index.css';
import messages from './messages';

const { Header } = Layout;
class eSign extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      activeId: null
    };
  }

  handleBtnDetail = (id, name) => {
    this.setState({
      activeId: id,
      activeName: name
    });
  }

  render() {
    const { intl } = this.props;
    const { activeId, activeName } = this.state;
    return (
      <Layout style={{ height: '100%', background: '#fff' }}>
        <Header className={style.head}>{intl.formatMessage(messages.eSignBtnManage)}</Header>
        <SupResize
          min={220}
          max={320}
          style={{ height: 'calc(100% - 56px)' }}
          direction="col"
        >
          <EsignSider
            btnDetail={this.handleBtnDetail}
          />
          {
            activeId ? (
              <EsignContent
                activeId={activeId}
                activeName={activeName}
                // style={{ height: 'calc(100% - 56px)'}}
              />
            ) : (
              <Layout style={{ height: '100%' }}>
                <div className={style.emptyContent}>
                  <SupIcon className={style.backIcon} type="iconpoint" />
                  {intl.formatMessage(messages.chooseLeftItem)}
                </div>
              </Layout>
            )
          }
        </SupResize>
      </Layout>
    );
  }
}

export default injectIntl(eSign);
