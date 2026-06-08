import React from 'react';
import { Layout, Icon, Menu } from 'sup-ui';
import {
  Switch,
  Link
} from 'react-router-dom';
import { injectIntl } from 'react-intl';
import commonMessage from 'root/common/messages';
import styles from './styles.less';

const { Header, Content, Sider } = Layout;
@injectIntl
export default class Setting extends React.Component {
  constructor(props) {
    super(props);
    const { intl } = props;
    this.state = {
      menu: [
        {
          key: 'notice',
          name: intl.formatMessage(commonMessage.notice),
          path: '/Setting/notice'
        },
        {
          key: 'content',
          name: intl.formatMessage(commonMessage.contentTemplate),
          path: '/Setting/content'
        },
        {
          key: 'theme',
          name: intl.formatMessage(commonMessage.themeSet),
          path: '/Setting/theme'
        }
        // {
        //   key: 'self',
        //   name: '个性设置',
        //   path: '/Setting/self'
        // }
      ]
    };
  }

  componentWillMount() {
    const { location } = this.props;
    const pathArray = location.pathname.split('/');
    this.setState({
      defaultOpen: pathArray[pathArray.length - 1]
    });
  }

  render() {
    const { routes, RouteWithSubRoutes, intl } = this.props;
    return (
      <Layout style={{ height: '100%' }}>
        <Header className={styles.setHeader}>
          <Icon
            type="back"
            className={styles.back}
            onClick={() => {
              window.location.hash = '#/messageCenter';
            }}
          />
          <span className={styles.setTitle}>{intl.formatMessage(commonMessage.setting)}</span>
        </Header>
        <Layout>
          <Sider width={200} className={[styles.setMenu, 'indexMenu']}>
            <Menu
              mode="inline"
              defaultSelectedKeys={[this.state.defaultOpen]}
              className={styles.setMenuBox}
            >
              {
                this.state.menu.map((x) => {
                  return (
                    <Menu.Item key={x.key} className={styles.setMenuItem}>
                      <Link to={x.path}>{x.name}</Link>
                    </Menu.Item>
                  );
                })
              }
            </Menu>
          </Sider>
          <Layout>
            <Content
              className={styles.setContent}
            >
              <Switch>
                {routes.map((route) => {
                  return (
                    <RouteWithSubRoutes key={route.name} {...route} RouteWithSubRoutes={RouteWithSubRoutes} />
                  );
                })}
              </Switch>
            </Content>
          </Layout>
        </Layout>
      </Layout>
    );
  }
}
