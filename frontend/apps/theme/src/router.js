import dynamicRoute from 'root/utils/dynamicRoute';

const routes = [
  {
    path: '/themeManage',
    component: dynamicRoute({
      component: () => import('./routes/themeManage/index')
    })
  },
  {
    path: '/themeSetting',
    component: dynamicRoute({
      component: () => import('./routes/themeSetting/index')
    })
  },
  {
    path: '/',
    component: dynamicRoute({
      component: () => import('./routes/themeManage/index')
    })
  }
];

if (process.env.NODE_ENV !== 'production') {
  routes.push({
    path: '/',
    component: dynamicRoute({
      component: () => import('./layouts/header/header')
      // models: [() => import('root/store/mobxStore')]
    }),
    routes: [
      {
        path: '/demo',
        component: dynamicRoute({
          component: () => import('./routes/demo')
        })
      },
      {
        path: '/demos',
        component: dynamicRoute({
          component: () => import('./routes/demo')
        })
      },
      {
        path: '/hooks',
        component: dynamicRoute({
          component: () => import('./routes/demo/hooks')
        })
      },
      {
        path: '/welcome',
        component: dynamicRoute({
          component: () => import('./routes/demo/welcome')
        })
      },
      {
        path: '/mobx',
        component: dynamicRoute({
          component: () => import('./routes/demo/mobx'),
          models: [() => import('root/store/mobxStore')]
        })
      },
      {
        path: '/mock',
        component: dynamicRoute({
          component: () => import('./routes/demo/mock'),
          models: [() => import('root/store/async')]
        })
      },
      {
        path: '/intl',
        component: dynamicRoute({
          component: () => import('./routes/demo/intl')
        })
      },
      {
        path: '/table',
        component: dynamicRoute({
          component: () => import('./routes/demo/table')
        })
      },
      {
        path: '/form',
        component: dynamicRoute({
          component: () => import('./routes/demo/form')
        })
      },
      {
        path: '/modal',
        component: dynamicRoute({
          component: () => import('./routes/demo/modal')
        })
      },
      {
        path: '/list',
        component: dynamicRoute({
          component: () => import('./routes/demo/list')
        })
      }
    ]
  });
}

export default routes;
