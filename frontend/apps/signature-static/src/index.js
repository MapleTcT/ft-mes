import React from 'react';
import { render } from 'react-dom';
import '@babel/polyfill';
import {
  HashRouter as Router,
  Switch,
  Route,
  Redirect
} from 'react-router-dom';
import { LocaleProvider } from 'sup-ui';
import { IntlProvider } from 'react-intl';

import { language, componentLanguage } from './language';

// import SupTheme from './theme/index.js';
import './index.less';
import 'sup-ui/dist/sup-ui.less';
import 'sup-rc-table/dist/index.less';
import 'sup-rc-reference/dist/index.less';
import 'sup-rc-resize/dist/index.css';
import 'sup-rc-tree/dist/index.less';

import routes from './router';
import changeTheme from './theme/changeTheme.js';

// import './mock/user';
if (process.env.NODE_ENV !== 'production') {
  // eslint-disable-next-line global-require
  // require('./mock/esign');
}

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
          <Route exact path="/">
            <Redirect to="/esign" />
          </Route>
          {routes.map((route, i) => (
            <RouteWithSubRoutes key={`route_${i + 1}`} {...route} />
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
