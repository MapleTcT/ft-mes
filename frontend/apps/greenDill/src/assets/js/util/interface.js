const { prefix = "msService" } = window.pageconfig || {};

const defaultInterface = {
  [`${prefix}`]: {
    countFile: "/baseService/workbench/countFile", // 获取文件预览数
    // checkPreviewStatus: `/baseService/workbench/checkPreviewStatus`, // 是否可以预览
    checkUserPower: "/baseService/userPermission/checkUserPower", // 权限
    openPending: "/baseService/user/open-pending" // 显示详情页面
  },
  "inter-api": {
    uploadFile: "/file-server/v1/file/upload/file", // 附件上传
    valueTreeList: "/systemcode/v1/valueTreeList", // 树形系统编码
    PreviewPicture: "/file-server/v1/file/auth/overview/pic", // 图片预览
    downloadFile: "/file-server/v1/file/download", // 附件下载
    company: `/organization/v1/company/common/get`,
    department: `/organization/v1/department/common/get`,
    position: `/organization/v1/position/common/get`,
    staff: `/organization/v1/staff/common/get`,
    user: `/auth/v1/user/common/get`,
    checkPreviewStatus: "/file-server/v1/file/auth/overview/file", // 附件预览
    getCurrentLoginInfo: "/auth/v1/getCurrentLoginInfo", // 获取当前登陆人信息
  }
};

export default (() => {
  let baseUrl = {};
  Object.keys(defaultInterface).forEach(fixKey => {
    Object.keys(defaultInterface[fixKey]).forEach(key => {
      baseUrl[key] = `/${fixKey}${defaultInterface[fixKey][key]}`;
    });
  });
  return baseUrl;
})();



// WEBPACK FOOTER //
// ./src/assets/js/util/interface.js