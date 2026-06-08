<template>
  <div
    class="appendix"
    v-if="isShowAppendix"
  >
    <header
      class="m_head"
      :style="{background: bgColor}"
    >
      <div class="m_hbox">
        <span
          class="head_arrow"
          @click="backHistory"
        >
          <a
            href="javascript:;"
            class="m_arrow"
          ></a>
        </span>
        <h2 class="m_title">{{$t('headBar.attach.text')}}</h2>
      </div>
    </header>
    <div class="m_center">
      <ul
        class="m_list"
        v-if="appendixList && appendixList.length > 0"
      >
        <li
          class="m-flex bt"
          v-for="(appendixlist, index) in appendixList"
          :id="'appendix_'+appendixlist.id"
          @click="openDropdown(appendixlist, $event)"
        >
          <fileSlide
            :key="appendixlist.id"
            :button="getDelBtn(appendixlist, index)"
          >
            <div class="m-flex">
              <div
                class="appendix-icons"
                :class="getAppendixType(appendixlist.name)"
              ></div>
              <div class="appendix-info">
                <div class="appendix-name">{{appendixlist.name}}</div>
                <div
                  class="appendix-data"
                  :class="phoneType == 'android'?'appendix-android':'appendix-ios'"
                >
                  <!-- <span class="upload-people">{{appendixlist.createStaff.name}}</span> -->
                  <span>{{formatTime("YMD_HM", appendixlist.createTime)}}</span>
                  <span>{{appendixlist.sizeDis}}</span>
                  <span class="upload-browseTimes">{{$t('headBar.attach.browseTimes')}}{{getBrowseTimes(appendixlist)}}</span>
                </div>
              </div>
            </div>
          </fileSlide>
          <!-- <div class="appendix-info">
            <div class="appendix-name">{{getAppendixName(appendixlist.path)}}</div>
            <div
              class="appendix-data"
              :class="phoneType == 'android'?'appendix-android':'appendix-ios'"
            >
              <span class="upload-people">{{appendixlist.createStaff.name}}</span>
              <span>{{formatTime("YMD_HM", appendixlist.createTime)}}</span>
              <span>{{appendixlist.sizeDis}}</span>
              <span>下载数：{{appendixlist.downloadTimes}}</span>
            </div>
          </div> -->
          <div class="appendix-btn">
            <ul class="appendix-btn-list">
              <li
                v-if="appendixlist.isFileView"
                @click="previewAppendix(appendixlist, $event)"
              >
                <i
                  class="appendix-btn-icon"
                  :class="judgePreviewAppendix(appendixlist.name) ? 'preview-icon' : 'preview-icon-no'"
                ></i>
                <span :class="judgePreviewAppendix(appendixlist.name) ? 'appendix-btn-text' : 'appendix-btn-text-no'">{{$t('button.preview.text')}}</span>
              </li>
              <li @click="downloadAppendix(appendixlist, $event)">
                <i class="appendix-btn-icon download-icon"></i>
                <span class="appendix-btn-text">{{$t('button.preview.download')}}</span>
              </li>
              <li
                v-show="viewType=='EDIT' || (viewType=='VIEW' && isShowAddAppendix)"
                @click="deleteAppendix(appendixlist.id, index)"
              >
                <i class="appendix-btn-icon delete-icon"></i>
                <span class="appendix-btn-text">{{$t('button.delete.text')}}</span>
              </li>
            </ul>
          </div>
        </li>
      </ul>
      <div
        v-else
        class="no-appendix"
      >
        {{$t("headBar.attach.noUpload")}}
      </div>
      <!-- 添加附件 -->
      <div
        v-show="viewType=='EDIT' || (viewType=='VIEW' && isShowAddAppendix)"
        class="appendix-add"
      >
        <div
          class="appendix-add-icon"
          :class="{ 'appendix-add-icon-width': isAndroidLessVersionFive }"
        >
          <input
            type="file"
            @change="addAppendix"
          >
          <a class="appendix-add-icons"></a>
          <a
            class="appendix-add-text"
            :class="{ 'appendix-add-text-top': isAndroidLessVersionFive }"
          >{{$t('headBar.attach.add')}}</a>
        </div>
      </div>
      <ul
        class="m_add_list"
        v-if="addAppendixList && addAppendixList.length > 0"
      >
        <li
          class="m-flex bt"
          v-for="(addappendixlist, index) in addAppendixList"
        >
          <div class="add-appendix">
            <div
              class="add-appendix-name"
              :name="addappendixlist.name"
            >{{index+1}}. {{addappendixlist.name}}</div>
            <i
              class="add-appendix-delete"
              @click="deleteAddAppendixList(index, $event)"
            ></i>
          </div>

        </li>
      </ul>
    </div>
  </div>
</template>
<script>
import { Toast, MessageBox, Indicator } from "mint-ui";
import _commonJs from "@/assets/js/itemList/index.js"; //引用模块进来
import { publicGetAxios, getIsHideWebTitle, addTicket } from '@/assets/js/util/common.js';
import fileSlide from '../cellComp/upload/fileSlide';
import interfaceURL from '../../assets/js/util/interface.js';

export default {
  name: 'Appendix',
  data() {
    return {
      viewType: "", //视图类型
      isShowAppendix: false, //是否显示附件列表界面
      appendixList: [], //附件信息列表
      phoneType: "",
      url: "",
      bgColor: "",
      appendixFile: {}, //保存的附件信息
      addAppendixList: [], //添加的附件信息
      appendixinfo: [],
      fileTypeList: [],
      filePathList: [],
      status: '',
      isAndroidLessVersionFive: false, // 安卓手机系统是否小于等于5.0
      isShowAddAppendix: true
    }
  },

  //数据监听
  watch: {
    'isShowAppendix': {
      deep: true,
      handler: function (newVal, oldVal) {//解决ios穿透的问题
        if (newVal) { //显示
          this.status = this.$root.$children[0].$refs.editForm.status;
          if (this.status == 99) { //单据生效状态，不能添加和删除附件
            this.isShowAddAppendix = false;
          }
          this.closeTouch();
        } else {
          this.openTouch();
        }
      }
    }
  },
  beforeMount() {
    if (window.pageConfig) {
      this.bgColor = window.pageConfig.bgColor ? window.pageConfig.bgColor : ''; // 背景色
    }
  },
  mounted() {
    if (window.pageConfig && window.pageConfig.viewType) { //页面名称
      this.viewType = window.pageConfig.viewType;
    }

    let urlParams = this.GetRequest(window.location.href);

    if (this.viewType == "VIEW") {
      if (!urlParams.pendingId) { //没有待办的时候，不能添加和删除附件
        this.isShowAddAppendix = false;
      }
    }

    if (!getIsHideWebTitle()) {
      const maskDom = document.querySelector('.mint-indicator-mask');
      if (maskDom) maskDom.style.top = '1.33333333rem';
    }

    this.phoneType = _commonJs.judgePhoneType(); //判断设备类型，android或者ios

    this.isAndroidLessVersionFive = _commonJs.isLessEqualAndroidVersion(5); // 获取当前安卓手机版本号小于等于5的进行样式处理
  },
  methods: {
    // 返回上一个页面
    backHistory: function () {
      this.isShowAppendix = false; //关闭附件界面
      Indicator.close();
    },

    //获取附件名称
    getAppendixName: function (appendixname) {
      var appendixName = _commonJs.encodeHtml(appendixname);
      return appendixName;
    },

    //获取附件类型，并根据附件类型设置class样式
    getAppendixType: function (appendixName) {
      return _commonJs.getAppendixTypes(appendixName);
    },

    //点击附件信息打开下拉框
    openDropdown: function (list, event) {
      // _commonJs.openAppendixDropdown(id, event, 2);
      // 打开预览或下载
      if (list.isFileView) {
        this.previewAppendix(list, event);
      } else {
        this.downloadAppendix(list, event);
      }
    },

    // 上传
    async upLoadFile(formData, url, callback) {
      const ticket = addTicket();
      let res = await this.$axios
        .post(url, formData, {
          headers: {
            "Content-Type": "multipart/form-data",
            ...(ticket ? { Authorization: ticket } : {})
          }
        }).catch(error => {
          let data = error.response.data;
          let errorMsg = data.msg;
          Indicator.close();
          Toast({
            message: errorMsg,
            iconClass: "mintui mintui-error"
          });
        });
      if (callback && typeof callback == "function") {
        callback(res);
      }
    },

    //添加附件
    addAppendix: function () {
      const $this = this;
      var obj = event.target;
      var reads = new FileReader();
      var file = obj.files[0];
      if (_commonJs.isRepeatFile(file, $this.appendixList) || _commonJs.isRepeatFile(file, $this.addAppendixList)) {
        let phoneType = _commonJs.judgePhoneType();
        if (phoneType == "ios" && file.type.indexOf("image") > -1) { //解决ios下拍照上传的图片名称一样的问题
          let repeatFileName1 = _commonJs.repeatFileName(file, $this.appendixList);
          let repeatFileName2 = _commonJs.repeatFileName(file, $this.addAppendixList);
          if (file.name == repeatFileName1 || file.name == repeatFileName2) {
            var fileName = file.name;
            fileName = new Date().getTime().toString() + "_" + fileName;
            let newFile = new window.File([file], fileName, { type: file.type }); //强制修改file的name
            file = newFile; //将新获取的newFile赋值给file
          } else {
            Toast({
              message: this.$t("cellComp.upload.msg.exist"),
              iconClass: "mintui mintui-error"
            });
            return false;
          }
        } else {
          Toast({
            message: this.$t("cellComp.upload.msg.exist"),
            iconClass: "mintui mintui-error"
          });
          return false;
        }
      }
      let formData = new FormData();
      formData.append("files", file);
      Indicator.open({
        text: this.$t('cellComp.upload.msg.uploading'), //文字
        spinnerType: "triple-bounce" //样式
      });
      let postUrl = interfaceURL.uploadFile;
      $this.upLoadFile(formData, postUrl, (res) => {
        if (res && res.data) {
          Indicator.close();
          let viewCode = window.pageConfig && window.pageConfig.viewCode;
          let workflowEnabled = window.pageConfig && window.pageConfig.workflowEnabled;
          let modelCode = window.pageConfig && window.pageConfig.modelCode;
          let data = res.data.data;

          $this.fileTypeList.push(null);
          $this.filePathList.push(data.path);
          $this.appendixFile[`${viewCode}_form.fileType`] = $this.fileTypeList;
          $this.appendixFile[`${viewCode}_form.filePath`] = $this.filePathList;
          $this.appendixFile[`${viewCode}_form.type`] = _commonJs.getUploadType(workflowEnabled, modelCode);
          $this.addAppendixList.push({ "name": file.name });
          obj.value = null;//清空选择
        }
      });
    },

    //删除添加的附件
    deleteAddAppendixList: function (index, fileName) {
      let $this = this;
      let attachmentName = fileName;
      $this.addAppendixList.splice(index, 1);
      let viewCode = window.pageConfig && window.pageConfig.viewCode;
      if ($this.filePathList && $this.filePathList.length > 0) {
        $this.filePathList.forEach((filePath, index) => {
          var name = filePath.slice(filePath.lastIndexOf("\\") + 1);
          if (name == attachmentName) {
            if ($this.filePathList && $this.filePathList.length == 1) {
              $this.filePathList.splice(index, 1);
              $this.fileTypeList.splice(index, 1);
              Object.keys($this.appendixFile).forEach(key => {
                delete $this.appendixFile[key];
              });
            } else {
              $this.filePathList.splice(index, 1);
              $this.fileTypeList.splice(index, 1);
              $this.appendixFile[`${viewCode}_form.fileType`].splice(index, 1);
              $this.appendixFile[`${viewCode}_form.filePath`].splice(index, 1);
            }
          }
        });
      }
    },

    //判断附件是否支持预览
    judgePreviewAppendix: function (appendixName) {
      return _commonJs.judgePreviewAppendixs(appendixName);
    },

    //预览附件
    previewAppendix: function (appendixlist, event) {
      _commonJs.previewAppendixs(this, appendixlist, event);
    },

    //下载附件
    downloadAppendix: function (obj, event) {
      _commonJs.downloadAppendixs(obj, event);
    },

    //删除附件
    deleteAppendix: function (id, index) {
      const { baseServiceApi } = window.pageConfig || {};
      publicGetAxios({
        url: baseServiceApi.deleteFile + `?id=${id}`,
        type: "delete"
      }, (result) => {
        let data = result.data.dealSuccess;
        if (data) {
          Toast({
            message: this.$t('message.operate.success'),
            iconClass: "mintui mintui-success"
          });
        }
        this.appendixList.splice(index, 1);
        this.$forceUpdate();
      });
    },

    // 时间格式化
    formatTime: function (fmt, date) {
      return _commonJs.getDateFormat(fmt, new Date(date));
    },

    closeTouch: function () {
      document.getElementsByClassName("m_centerBox")[0].style.overflow = "hidden";
    },

    openTouch: function () {
      document.getElementsByClassName("m_centerBox")[0].style.overflow = "auto";
    },

    getDelBtn: function (appendixlist, index) {
      let btns = [];
      const { viewType, isShowAddAppendix } = this;
      if (viewType == 'EDIT' || (viewType == 'VIEW' && isShowAddAppendix)) {
        btns = [{ type: 'delete', event: (e) => { this.deleteAppendix(appendixlist.id, index) }, txt: this.$t('button.delete.text'), width: 120 }];
      }
      return btns;
    },

    // 获取浏览次数
    getBrowseTimes: function (data) {
      const { isFileView, previewTimes = 0, downloadTimes = 0 } = data;
      return isFileView ? previewTimes + downloadTimes : downloadTimes;
    }
  },
  components: {
    fileSlide
  }
}
</script>
<style lang="less" scoped>
@import "./Appendix.less";
</style>



// WEBPACK FOOTER //
// src/components/HeadBar/Appendix.vue