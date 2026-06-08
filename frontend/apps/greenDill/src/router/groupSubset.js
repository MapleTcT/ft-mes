import Vue from 'vue';
import Router from 'vue-router';
// import mainView from '@/pages/mainView';
// import pageMenu from '@/pages/pageMenu';
// import itemList from '@/pages/itemist/itemList';
// import editView from '@/pages/editView';
import groupSubset from '@/components/refer/groupSubset';
import selectContainer from '@/components/refer/selectContainer';
import memberSubset from '@/components/refer/memberSubset';
import searchResult from '@/components/refer/searchResult';
import selectDetail from '@/components/refer/selectDetail';
Vue.use(Router)

export default new Router({
  routes: [
    // {
    //     path: '/',
    //     name: 'pageMenu',
    //     component: pageMenu
    // },
    // {
    //     path: '/',
    //     name: 'mainView',
    //     component: mainView
    // },
    // {
    //     path: '/mainView',
    //     name: 'mainView',
    //     component: mainView
    // },
    // {
    //     path: '/itemList',
    //     name: 'itemList',
    //     component: itemList,
    //     meta: { keepAlive: true }
    // },
    // {
    //     path: '/editView',
    //     name: 'editView',
    //     component: editView
    // },
    {
      path: '/groupSubset',
      name: '/groupSubset',
      component: groupSubset
    },
    {
      path: '/memberSubset',
      name: '/memberSubset',
      component: memberSubset
    },
    {
      path: '/selectContainer',
      name: '/selectContainer',
      component: selectContainer
    },
    {
      path: '/searchResult',
      name: '/searchResult',
      component: searchResult
    },
    {
      path: '/selectDetail',
      name: '/selectDetail',
      component: selectDetail
    }
  ]
})


// WEBPACK FOOTER //
// ./src/router/groupSubset.js