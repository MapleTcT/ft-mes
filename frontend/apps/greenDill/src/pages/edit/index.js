// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
// import Zepto from 'zepto-node';
import App from "./editView";
// import router from '../../router';
// import '../../router/index.js';
import { renderComp } from "../render.js";
import { getAuthentication } from "@/assets/js/util/common.js";

if (process.env.NODE_ENV !== "production") {
  window.pageConfig = {
	"prefix": "/msService",// url前缀
			"viewCode": "zq0210_1.0.0_bd0210_bd0210edit__mobile__", //  视图编码
			"entityCode":"zq0210_1.0.0_bd0210",// 实体编码
			"menuCode": "zq0210_1.0.0_bd0210_bd0210edit",
			"hasAttachment":"true",
			"viewName": "bd0210edit__mobile__",// 视图名称
			"viewTitle": "表单0210编辑视图",//视图标题
			"viewType": "EDIT",// 视图类型
			"modelCode": "zq0210_1.0.0_bd0210_Bd0210zhu",// 模型编码
			"isMain":true,// 主模型
			"permissionCode":null, // 视图权限操作码（主要参照视图使用）
			"showType": "SINGLE",// 视图展示类型
			"modelAlias": "bd0210zhu",// 模型别名
			"workflowEnabled": true,// 启用工作流
			"listButtons":{
			},
			"interfaceApi": {
			    "downloadFile":"/zq0210/baseService/workbench/download",//  下载附件接口
				"PreviewAttachments":"/zq0210/baseService/workbench/preview",//附件预览
                "PreviewPicture":"/zq0210/baseService/workbench/preview/picture",//图片预览
                "authenticationMessage":"/msService/zq0210/baseService/workbench/download/getAuthenticationMessage",//附件下载预览鉴权信息
				"sourceData": "/msService/zq0210/bd0210/bd0210zhu/data", //  表单数据
                "datagridData": "/msService/zq0210/bd0210/bd0210zhu/data-", //  表格数据源
                "submit": "/msService/zq0210/bd0210/bd0210zhu/bd0210edit__mobile__/submit", //  表单提交
                "save": "/msService/zq0210/bd0210/bd0210zhu/bd0210edit__mobile__/save", //  表单保存
                "dealInfoList": "/msService/zq0210/bd0210/bd0210zhu/dealInfo-list", //  意见处理
                "editStates": "/msService/zq0210/bd0210/bd0210zhu/editStates", //  配置信息
				"referenceCopy":"/msService/zq0210/bd0210/bd0210zhu/bd0210czst",//参考复制视图
				"userRef":"/greenDill/mobile-static/user.html",//工作流选择人员
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
	}
}

getAuthentication()

/* eslint-disable no-new */
renderComp({App});



// WEBPACK FOOTER //
// ./src/pages/edit/index.js