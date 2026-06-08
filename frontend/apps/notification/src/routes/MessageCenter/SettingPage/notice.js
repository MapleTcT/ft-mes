import React from 'react';
import _ from 'lodash';
import { Layout, Menu } from 'sup-ui';
import {
  Switch,
  Link
} from 'react-router-dom';
import { getNotice } from 'root/services/messageCenter';
import styles from './styles.less';

const { Content, Sider } = Layout;
export default class Setting extends React.Component {
  constructor() {
    super();
    this.state = {
      defaultOpen: '',
      menuKey: '',
      menu: []
    };
  }

  componentWillMount() {
    const { pathname } = this.props.location;
    const { routes } = this.props;
    getNotice().then((res) => {
      this.setState({
        defaultOpen: _.get(routes.find((item) => item.path === pathname), 'name', ''),
        menu: res.data.list.map((x) => {
          return {
            name: x.name,
            id: x.id,
            key: x.protocol,
            contentType: x.contentType,
            systemConfigAppCode: x.systemConfigAppCode,
            systemConfigCode: x.systemConfigCode,
            path: _.get(routes.find((item) => item.name === x.protocol), 'path', '/Setting/notice/common'),
            component: _.get(routes.find((item) => item.name === x.protocol),
              'component',
              _.get(routes.find((item) => item.name === 'common'), 'component', '')
            )
          };
        })
      });
    });
  }

  onSelect = (selectKeys) => {
    this.setState({
      menuKey: selectKeys.key
    });
  }

  render() {
    const { RouteWithSubRoutes } = this.props;
    const { menu } = this.state;
    return (
      <Layout className={styles.notice}>
        <Sider
          width={180}
          style={{
            background: '#fff',
            borderRight: '1px solid #e6eaee'
          }}
        >
          {
            menu.length > 0 ? (
              <Menu
                mode="inline"
                defaultSelectedKeys={[this.state.defaultOpen]}
                style={{ height: '100%', borderRight: 0 }}
                onSelect={this.onSelect}
                className="noticeMenu"
              >
                {
                  menu.map((x) => {
                    return (
                      <Menu.Item key={x.key} className={styles.setMenuItem}>
                        <Link to={x.path}>{x.name}</Link>
                      </Menu.Item>
                    );
                  })
                }
              </Menu>
            ) : null
          }
        </Sider>
        <Layout style={{ position: 'relative' }}>
          <Content
            className={styles.setContent}
          >
            <Switch>
              {
                menu.length > 0 ? menu.filter((route) => route.key === this.state.menuKey).map((route) => {
                  const id = route.key;
                  return (
                    <RouteWithSubRoutes key={route.name} protocolId={id} data={route} {...route} />
                  );
                }) : null
              }
            </Switch>
          </Content>
        </Layout>
      </Layout>
    );
  }
}
