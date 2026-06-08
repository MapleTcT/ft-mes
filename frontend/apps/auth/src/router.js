import dynamicRoute from 'root/utils/dynamicRoute';

const routes = [
  {
    path: '/authority',
    name: 'authority',
    component: dynamicRoute({
      component: () => import('./components/Authority')
    })
  },
  {
    path: '/user',
    name: 'user',
    component: dynamicRoute({
      component: () => import('./routes/user')
    })
  },
  {
    path: '/role',
    name: 'role',
    component: dynamicRoute({
      component: () => import('./routes/role')
    })
  },
  {
    path: '/auth',
    name: 'auth',
    component: dynamicRoute({
      component: () => import('./routes/auth')
    })
  },
  {
    path: '/online',
    name: 'onlineItemMgr',
    component: dynamicRoute({
      component: () => import('./routes/online')
    })
  },
  {
    path: '/ip',
    name: 'ipBlackWhite',
    component: dynamicRoute({
      component: () => import('./routes/ip')
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
      // {
      //   path: '/sysconfig',
      //   component: dynamicRoute({
      //     component: () => import('./routes/sysconfig')
      //   })
      // },
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
  }
  );
}

export default routes;
