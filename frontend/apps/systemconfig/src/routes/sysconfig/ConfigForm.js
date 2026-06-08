import React from 'react';
import { Layout } from 'sup-ui';
import WrappedFormBuilderForm from './FormBuilder';

import style from './style.less';

const { Content, Header } = Layout;

export default class ConfigForm extends React.PureComponent {
  render() {
    const { config, name } = this.props;
    return (
      <>
        <Header className={style.header}>{name}</Header>
        <Content className={style.content}>
          {config ? (
            <div className={style.contentComp}>
              {config.length ? <WrappedFormBuilderForm {...this.props} /> : null}
            </div>
          ) : null}
        </Content>
      </>
    );
  }
}
