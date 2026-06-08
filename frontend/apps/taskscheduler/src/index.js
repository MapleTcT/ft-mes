import React from 'react';
import { render } from 'react-dom';
import '@babel/polyfill';
import {
  HashRouter,
  Route
} from 'react-keeper';
import { LocaleProvider } from 'sup-ui';
import { IntlProvider } from 'react-intl';
import { language, componentLanguage } from './language';

// import SupTheme from './theme/index.js';
import './index.less';
import 'sup-ui/dist/sup-ui.less';
import 'sup-rc-table/dist/index.less';
import 'sup-rc-i18n/dist/index.less';
import 'sup-rc-resize/dist/index.css';
import 'sup-rc-tree/dist/index.less';
import 'sup-rc-search/dist/index.less';

// import routes from './router';
import changeTheme from './theme/changeTheme.js';

import Scheduler from './routes/taskScheduler/index';
import TaskLog from './routes/taskScheduler/taskLog';
// import './mock/user';
if (process.env.NODE_ENV !== 'production') {
  // eslint-disable-next-line global-require
  // require('./mock/taskScheduler');
}

changeTheme(); // 切换主题

// function RouteWithSubRoutes(route) {
//   return (
//     <Route
//       path={route.path}
//       render={(props) => (
//         <route.component
//           {...props}
//           routes={route.routes}
//           store={route.store}
//           RouteWithSubRoutes={RouteWithSubRoutes}
//         />
//       )}
//     />
//   );
// }

const App = () => (
  <IntlProvider {...language} defaultLocale="zh-cn">
    <LocaleProvider locale={componentLanguage}>
      <div style={{ height: '100%' }}>
        <Route miss index cache path="/" component={Scheduler} />
        <Route path="/tasklog" component={TaskLog} />
      </div>
    </LocaleProvider>
  </IntlProvider>
);

render(<HashRouter><App /></HashRouter>, document.getElementById('root'));

if (module.hot) {
  module.hot.accept('./router', () => {
    render(<HashRouter><App /></HashRouter>, document.getElementById('root'));
  });
}
