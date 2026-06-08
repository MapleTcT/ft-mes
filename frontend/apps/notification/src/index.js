import React from 'react';
import { render } from 'react-dom';
import '@babel/polyfill';
import { HashRouter as Router, Switch, Route } from 'react-router-dom';

import { SupIntlProvider } from 'sup-rc-utility';
import './index.css';
import 'sup-ui/dist/sup-ui.less';
import 'sup-rc-table/dist/index.less';
import 'sup-rc-reference/dist/index.less';
import 'sup-rc-resize/dist/index.css';
import 'sup-rc-tree/dist/index.less';
import routes from './router';
import changeTheme from './theme/changeTheme.js';

// import './mock/user';
// import './mock/messageCenter';

changeTheme(); // 切换主题

function RouteWithSubRoutes(route) {
  return (
    <Route
      path={route.path}
      render={(props) => (
        <route.component
          {...props}
          data={route.data}
          routes={route.routes}
          id={route.protocolId}
          store={route.store}
          RouteWithSubRoutes={RouteWithSubRoutes}
        />
      )}
    />
  );
}

const App = () => (
  // <IntlProvider {...language} defaultLocale="zh-cn">
  //   <LocaleProvider locale={componentLanguage}>
  //     <Router>
  //       <Switch>
  //         <Route exact path="/">
  //           <Redirect to="/messageCenter" />
  //         </Route>
  //         {routes.map((route) => (
  //           <RouteWithSubRoutes key={route.name} {...route} />
  //         ))}
  //       </Switch>
  //     </Router>
  //   </LocaleProvider>
  // </IntlProvider>
  <SupIntlProvider moduleCode="notificationAdmin" __dev__>
    <Router>
      <Switch>
        {routes.map((route, i) => (
          <RouteWithSubRoutes key={`route_${i + 1}`} {...route} />
        ))}
      </Switch>
    </Router>
  </SupIntlProvider>
);
render(<App />, document.getElementById('root'));

if (module.hot) {
  module.hot.accept('./router', () => {
    render(<App />, document.getElementById('root'));
  });
}
