import React from 'react';
import { injectIntl } from 'react-intl';
import { Layout } from 'sup-ui';

const { Sider } = Layout;

class SiderTree extends React.Component {
  render() {
    return <Sider theme="light">sider</Sider>;
  }
}

export default injectIntl(SiderTree);
