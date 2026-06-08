// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import App from "./itemList";
import { renderComp, i18n } from "../render.js";

if (process.env.NODE_ENV !== "production") {
  window.pageConfig = {
    "prefix": "/msService",// url前缀
    "viewCode": "zq0210_1.0.0_bd0210_bd0210list01__mobile__", //  视图编码
    "entityCode":"zq0210_1.0.0_bd0210",// 实体编码
    "menuCode": "zq0210_1.0.0_bd0210_bd0210list01",
    "hasAttachment":"true",
    "viewName": "bd0210list01__mobile__",// 视图名称
    "viewTitle": "表单列表视图01",//视图标题
    "viewType": "LIST",// 视图类型
    "modelCode": "zq0210_1.0.0_bd0210_Bd0210zhu",// 模型编码
    "isMain":true,// 主模型
    "permissionCode":null, // 视图权限操作码（主要参照视图使用）
    "showType": "SINGLE",// 视图展示类型
    "modelAlias": "bd0210zhu",// 模型别名
    "workflowEnabled": true,// 启用工作流
    "listButtons":{
      "ADD":"zq0210_1.0.0_bd0210_bd0210list01_bd0210list01_add_add_zq0210_1.0.0_bd0210_bd0210list01",
      "DELETE":"zq0210_1.0.0_bd0210_bd0210list01_bd0210list01_delete_del_zq0210_1.0.0_bd0210_bd0210list01",
    },
    "interfaceApi": {
        "downloadFile":"/zq0210/baseService/workbench/download",//  下载附件接口
      "PreviewAttachments":"/zq0210/baseService/workbench/preview",//附件预览
              "PreviewPicture":"/zq0210/baseService/workbench/preview/picture",//图片预览
              "authenticationMessage":"/msService/zq0210/baseService/workbench/download/getAuthenticationMessage",//附件下载预览鉴权信息
      "mainView": "/msService/zq0210/bd0210/bd0210zhu/bd0210view", //  主查看视图
      "downloadXls": "/msService/zq0210/bd0210/bd0210zhu/downloadXls", //  下载导入模板
              "importMainXls": "/msService/zq0210/bd0210/bd0210zhu/importMainXls", //  导入Excel
              "deleteData": "/msService/zq0210/bd0210/bd0210zhu/delete",// 删除数据
              "sourceData": "/msService/zq0210/bd0210/bd0210zhu/bd0210list01__mobile__-", //  列表数据
      "echartsData": "/msService/zq0210/bd0210/bd0210zhu/echartsData", //  图表数据源
    },
    "baseServiceApi": {
              "flowRoot": "/msService/baseService/workflow/flowRoot", //  路由数据
              "layoutJson": "/msService/baseService/view/layoutJson", //  布局数据
              "uploadList": "/inter-api/file-server/v1/file/upload-list", //  附件列表
              "systemCodeJson": "/inter-api/systemcode/v1/systemCodeJson", //  系统编码
              "uploadFile": "/inter-api/file-server/v1/file/upload/file", //  附件上传
              "deleteFile": "/inter-api/file-server/v1/file/delete", //  附件删除
              "mneClient": "/msService/baseService/workbench/common/mneClient", //  助记码
          },
    // prefix: "/msService", // url前缀
    // viewCode: "zq0210_1.0.0_jichu0210_jcliest__mobile__", //  视图编码
    // entityCode: "zq0210_1.0.0_jichu0210", // 实体编码
    // menuCode: "zq0210_1.0.0_jichu0210_jcliest",
    // hasAttachment: "false",
    // viewName: "jcliest__mobile__", // 视图名称
    // viewTitle: "基础列表视图", //视图标题
    // viewType: "LIST", // 视图类型
    // modelCode: "zq0210_1.0.0_jichu0210_Jczhumx", // 模型编码
    // isMain: true, // 主模型
    // permissionCode: null, // 视图权限操作码（主要参照视图使用）
    // showType: "SINGLE", // 视图展示类型
    // modelAlias: "jczhumx", // 模型别名
    // workflowEnabled: false, // 启用工作流
    // listButtons: {
    //   ADD:
    //     "zq0210_1.0.0_jichu0210_jcliest_jcliest_add_add_zq0210_1.0.0_jichu0210_jcliest",
    //   DELETE:
    //     "zq0210_1.0.0_jichu0210_jcliest_jcliest_delete_del_zq0210_1.0.0_jichu0210_jcliest"
    // },
    // interfaceApi: {
    //   PreviewAttachments: "/zq0210/baseService/workbench/preview", //附件预览
    //   PreviewPicture: "/zq0210/baseService/workbench/preview/picture", //图片预览
    //   mainView: "/msService/zq0210/jichu0210/jczhumx/jcview", //  主查看视图
    //   downloadXls: "/zq0210/jichu0210/jczhumx/downloadXls", //  下载导入模板
    //   importMainXls: "/zq0210/jichu0210/jczhumx/importMainXls", //  导入Excel
    //   deleteData: "/zq0210/jichu0210/jczhumx/delete", // 删除数据
    //   sourceData: "/zq0210/jichu0210/jczhumx/jcliest__mobile__-", //  列表数据
    //   echartsData: "/zq0210/jichu0210/jczhumx/echartsData" //  图表数据源
    // },
    // baseServiceApi: {
    //   flowRoot: "/baseService/workflow/flowRoot", //  路由数据
    //   layoutJson: "/baseService/view/layoutJson", //  布局数据
    //   uploadList: "/baseService/workbench/upload-list", //  附件列表
    //   systemCodeJson: "/baseService/systemCode/systemCodeJson", //  系统编码
    //   uploadFile: "/baseService/workbench/uploadFile", //  附件上传
    //   deleteFile: "/baseService/workbench/deleteFile", //  附件删除
    //   mneClient: "/baseService/workbench/common/mneClient" //  助记码
    // }
  };
}

/* eslint-disable no-new */
renderComp({App});



// WEBPACK FOOTER //
// ./src/pages/itemList/index.js