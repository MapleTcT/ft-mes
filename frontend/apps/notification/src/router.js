import dynamicRoute from 'root/utils/dynamicRoute';

export default [
  {
    path: '/messageCenter',
    name: 'messageCenter',
    component: dynamicRoute({
      component: () => import('./routes/MessageCenter')
    })
  },
  {
    path: '/Setting',
    name: 'Setting',
    component: dynamicRoute({
      component: () => import('./routes/MessageCenter/Setting')
    }),
    routes: [
      {
        path: '/Setting/content',
        name: 'content',
        component: dynamicRoute({
          component: () => import('./routes/MessageCenter/SettingPage/content')
        })
      },
      {
        path: '/Setting/notice',
        name: 'notice',
        component: dynamicRoute({
          component: () => import('./routes/MessageCenter/SettingPage/notice')
        }),
        routes: [
          {
            name: 'stationLetter',
            path: '/Setting/notice/message',
            component: dynamicRoute({
              component: () => import('./routes/MessageCenter/Notification/message')
            })
          },
          {
            name: 'email',
            path: '/Setting/notice/mail',
            component: dynamicRoute({
              component: () => import('./routes/MessageCenter/Notification/mail')
            })
          },
          // {
          //   name: 'weixin',
          //   path: '/Setting/notice/weixin',
          //   component: dynamicRoute({
          //     component: () => import('./routes/MessageCenter/Notification/weixin')
          //   })
          // },
          // {
          //   name: 'dingtalk',
          //   path: '/Setting/notice/dingtalk',
          //   component: dynamicRoute({
          //     component: () => import('./routes/MessageCenter/Notification/dingtalk')
          //   })
          // },
          {
            name: 'common',
            path: '/Setting/notice/common',
            component: dynamicRoute({
              component: () => import('./routes/MessageCenter/Notification/common')
            })
          }
        ]
      },
      {
        path: '/Setting/self',
        name: 'self',
        component: dynamicRoute({
          component: () => import('./routes/MessageCenter/SettingPage/self')
        })
      },
      {
        path: '/Setting/theme',
        name: 'theme',
        component: dynamicRoute({
          component: () => import('./routes/MessageCenter/SettingPage/theme')
        })
      }
    ]
  },
  {
    path: '/selfstationletter',
    name: 'selfstationletter',
    component: dynamicRoute({
      component: () => import('./routes/MessageCenter/SelfStationLetter/index')
    })
  }
];
