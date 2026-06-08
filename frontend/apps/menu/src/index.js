/*
 * @Author: DWP
 * @Date: 2020-08-12 12:04:42
 * @LastEditors: DWP
 * @LastEditTime: 2020-08-21 20:37:12
 */
import React from 'react';
import { render } from 'react-dom';
import '@babel/polyfill';
import { HashRouter as Router, Switch, Route } from 'react-router-dom';

import { LocaleProvider } from 'sup-ui';
import { IntlProvider } from 'react-intl';

import { language, componentLanguage } from './language';
import './index.css';
import 'sup-ui/dist/sup-ui.less';
import 'sup-rc-tree/dist/index.less';
import 'sup-rc-i18n/dist/index.less';
import 'sup-rc-resize/dist/index.css';
import routes from './router';
import changeTheme from './theme/changeTheme.js';

changeTheme(); // 切换主题

function RouteWithSubRoutes(route) {
  return (
    <Route
      path={route.path}
      render={(props) => (
        <route.component
          {...props}
          routes={route.routes}
          store={route.store}
          RouteWithSubRoutes={RouteWithSubRoutes}
        />
      )}
    />
  );
}

const App = () => (
  <IntlProvider {...language} defaultLocale="zh-cn">
    <LocaleProvider locale={componentLanguage}>
      <Router>
        <Switch>
          {routes.map((route, i) => (
            <RouteWithSubRoutes
              key={`route_${i + 1}`}
              {...route}
              routes={i === 0 ? routes : []}
            />
          ))}
        </Switch>
      </Router>
    </LocaleProvider>
  </IntlProvider>
);

render(<App />, document.getElementById('root'));

if (module.hot) {
  module.hot.accept('./router', () => {
    render(<App />, document.getElementById('root'));
  });
}
