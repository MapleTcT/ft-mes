<template>
  <div>
    <!--图片-->
    <template v-if="isPicture">
      <div
        class="m_attachment"
        :name="params.condition.name"
      >
        <viewer
          :images="imgList"
          class="ma_item pickbox"
          v-for="(item, index) in imgList"
          :key="index"
        >
          <img
            class="selimg"
            :src="item"
            alt=""
          >
          <a
            v-if="!params.condition.readonly"
            class="i_cancel"
            @click="deleteImg(index)"
          ><i>×</i></a>
        </viewer>
        <div
          class="ma_add"
          v-if="!params.condition.readonly"
        >
          <input
            type="file"
            accept="image/*"
            :name="params.name"
            @change="bindEvents.onChange && bindEvents.onChange(), getPickResult($event)"
            mutiple
          >
          <a class="btn_add"></a>
        </div>
      </div>
    </template>

    <!--附件-->
    <template v-if="isAppendix">
      <!--表体附件-->
      <div
        class="m_wrap"
        v-if="params.condition.datagridCode"
      >
        <div class="m_attachment">
          <div>
            <div
              class="m_appendix pickbox"
              v-for="(attachmentlist, index) in attachmentList"
              :id="`appendix_${attachmentlist.id}`"
              @click="openDropdown(attachmentlist.id, $event, attachmentlist)"
            >
              <div
                class="m_appendix_image fl"
                :class="getAppendixType(attachmentlist.name)"
              >
              </div>
              <div class="m_appendix_box">
                <span class="fb_name_datagrid">{{attachmentlist.name}}</span>
              </div>
              <!-- 删除附件 -->
              <a
                v-show="!params.condition.readonly"
                class="i_cancel fr"
                @click="deleteFile(index, $event)"
              ><i>×</i>
              </a>
            </div>
          </div>
          <div
            v-show="!params.condition.readonly"
            class="ma_add"
          >
            {{params.events}}
            <input
              type="file"
              :name="params.name"
              :ref="params.condition.name"
              @change="bindEvents.onChange && bindEvents.onChange(), getPickFileResult($event)"
            >
            <a class="btn_add"></a>
          </div>
        </div>
      </div>

      <!--表头附件-->
      <div
        class="m_wrap"
        v-else
      >
        <div class="m_attachment">
          <div>
            <div
              class="m_appendix pickbox"
              v-for="(filelist, index) in fileList"
              :id="'appendix_'+index"
              @click="openDropdown(index, $event, filelist)"
              v-if="isShowAppendix(filelist, params)"
            >
              <div
                class="m_appendix_image fl"
                :class="getAppendixType(filelist.name)"
              >
              </div>
              <div class="m_appendix_box">
                <span class="fb_name">{{filelist.name}}</span>
                <i class="fb_size">{{filelist.size || filelist.sizeDis}}</i>
              </div>
              <!-- 删除附件 -->
              <a
                v-show="!params.condition.readonly"
                class="i_cancel fr"
                @click="deleteFile(index, $event)"
              ><i>×</i>
              </a>
              <!-- 预览和下载附件 -->
              <div
                class="m_appendix_btn"
                v-if="filelist && filelist.id"
              >
                <ul class="m_appendix_btn_list">
                  <li
                    v-if="filelist && params.isFileView"
                    @click="previewAppendix(filelist, $event)"
                  >
                    <i
                      class="m_appendix_btn_icon"
                      :class="judgePreviewAppendix(filelist.name) ? 'preview-icon' : 'preview-icon-no'"
                    ></i>
                    <span :class="judgePreviewAppendix(filelist.name) ? 'm_appendix_btn_text' : 'm_appendix_btn_text_no'">{{$t('cellComp.upload.preview')}}</span>
                  </li>
                  <li @click="downloadAppendix(filelist, $event)">
                    <i class="m_appendix_btn_icon download-icon"></i>
                    <span class="m_appendix_btn_text">{{$t('cellComp.upload.download')}}</span>
                  </li>
                </ul>
              </div>
            </div>
          </div>
          <div
            v-show="!params.condition.readonly"
            class="ma_add"
          >
            {{params.events}}
            <input
              type="file"
              :name="params.name"
              @change="bindEvents.onChange && bindEvents.onChange(), getPickFileResult($event)"
            >
            <a class="btn_add"></a>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script>
import Vue from 'vue';
import Viewer from 'v-viewer';
import 'viewerjs/dist/viewer.css';
import _commonJs from "@/assets/js/itemList/index.js"; //引用模块进来
import { Toast, Indicator } from "mint-ui";
import { publicGetAxios, addTicket } from '@/assets/js/util/common.js';
import Emitter from "@/assets/js/emitter.js";
import AsyncValidator from "async-validator";
import { cloneDeep } from 'lodash';
Vue.use(Viewer)
Viewer.setDefaults({
  Options: {
    'inline': true,
    'button': true,
    'navbar': true,
    'title': true,
    'toolbar': true,
    'tooltip': true,
    'movable': true,
    'zoomable': true,
    'rotatable': true,
    'scalable': true,
    'transition': true,
    'fullscreen': true,
    'keyboard': false,
    'url': 'data-source'
  }
});
export default {
  name: "upload",
  data: function () {
    return {
      isPicture: false, //是否为图片
      isAppendix: false, //是否为附件
      imgList: [], //图片列表
      fileList: [], //附件列表
      postUrl: "", //需要上传到的地址
      imageUrl: "", //img绑定的src地址
      pictureFile: {},
      appendixList: [],
      bindEvents: {},
      attachmentList: [] //表体的附件
    };
  },
  props: ['params', "imgValue", "condition", "newName", "rowIndex"],
  mixins: [Emitter],
  beforeUpdate() {
    this.initNewName();
  },
  beforeMount() {
    this.initNewName();
  },
  mounted() {
    this.dispatch("iForm", "form-add", this);
    const { isPicture, isAppendix, fileList, postUrl } = this.params || {};
    this.isPicture = isPicture;
    this.isAppendix = isAppendix;
    this.imgList = this.imgValue ? [this.imgValue] : [];
    this.fileList = cloneDeep(fileList);
    this.postUrl = postUrl;
    this.bindEvents = {
      ...this.params.condition.events
    }
    if (this.isAppendix) {
      this.getAppendixList(); //获取附件
    }
    if (this.params.condition) {
      this.params.condition.fileList = this.fileList;
      this.params.condition.imgList = this.imgList;
    }

    if (this.params.condition && this.params.condition.datagridCode) {
      if (this.params.condition.value && this.params.condition.valueId) {
        this.params.condition.value.forEach((item, i) => {
          this.attachmentList.push({ name: item, id: this.params.condition.valueId[i] })
        });
      }
    }
  },
  methods: {
    //选择图片
    getPickResult: function (event) {
      var obj = event.target;
      var reads = new FileReader();
      var f = obj.files[0];
      reads.readAsDataURL(f);

      // TODO 限制文件大小
      // let fileSize = f.size / 1024 /1024; // 单位M
      // if(fileSize > 2){
      //     return false;
      // }

      let formData = new FormData();
      formData.append("files", f);

      reads.onload = (e) => {
        this.imgList = [e.target.result];
      };


      this.upLoadFile(formData, this.postUrl, (res) => {
        // 上传图片
        let upLoadUrl = res.data;
        obj.value = null;
        this.$emit('updatePicture', event, upLoadUrl, this.params.condition, 'upload');

      });

    },
    //删除图片
    deleteImg: function (index) {
      this.bindEvents && this.bindEvents.onChange && this.bindEvents.onChange();
      var data = this.imgList;
      data.splice(index, 1);
      this.$emit('updatePicture', event, '', this.params.condition, 'delete');
    },

    //获取附件
    getAppendixList: function () {
      if (vue.appendixList && vue.appendixList.length > 0) {
        this.appendixList = vue.appendixList;
        this.fileList = [];
        for (let i = 0; i < this.appendixList.length; i++) {
          if (this.appendixList[i].propertyCode == this.params.propertyCode) {
            this.fileList.push(this.appendixList[i]);
          }
        }
      }
    },

    //是否显示附件字段信息
    isShowAppendix: function (filelist, params) {
      let isShow = false;
      if (filelist.propertyCode == params.propertyCode) {
        isShow = true;
      }
      if (filelist) {
        isShow = true;
      }
      return isShow;
    },

    //选择附件
    getPickFileResult: function (event) {
      var obj = event.target;
      var reads = new FileReader();
      var file = obj.files[0];
      let isRepeatFile;
      const { datagridCode } = this.params.condition || {};
      if (datagridCode) { //表格的附件
        isRepeatFile = _commonJs.isRepeatFile(file, this.attachmentList);
      } else { //表头的附件
        isRepeatFile = _commonJs.isRepeatFile(file, this.fileList);
      }
      if (isRepeatFile) {
        let phoneType = _commonJs.judgePhoneType();
        if (phoneType == "ios" && file.type.indexOf("image") > -1) { //解决ios下拍照上传的图片名称一样的问题
          let repeatFileName = _commonJs.repeatFileName(file, this.fileList);
          if (file.name == repeatFileName) {
            var fileName = file.name;
            fileName = new Date().getTime().toString() + "_" + fileName;
            let newFile = new window.File([file], fileName, { type: file.type }); //强制修改file的name
            file = newFile; //将新获取的newFile赋值给file
          } else {
            Toast({
              message: this.$t('cellComp.upload.msg.exist'),
              iconClass: "mintui mintui-error"
            });
            return false;
          }
        } else {
          Toast({
            message: this.$t('cellComp.upload.msg.exist'),
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
      this.upLoadFile(formData, this.postUrl, (res) => {
        Indicator.close();
        let resData = res.data;
        // var type = fileName.slice(fileName.lastIndexOf(".") + 1).toLowerCase();

        if (datagridCode) { //表格的附件
          // $this.attachmentList = [];
          this.attachmentList.push({ "name": file.name, "size": (file.size / 1024).toFixed(0) + "kb" });
        } else { //表头的附件
          this.fileList.push({ "name": file.name, "size": (file.size / 1024).toFixed(0) + "kb" });
        }

        obj.value = null;//清空选择
        this.$emit('updateAppendix', event, resData, this.params.condition, 'upload');
      });
    },

    //删除附件
    deleteFile: function (index) {
      this.bindEvents && this.bindEvents.onChange && this.bindEvents.onChange();
      let deleteFileInfo;
      var data;
      if (this.params.condition && this.params.condition.datagridCode) { //表体的附件
        deleteFileInfo = this.attachmentList[index];
        data = this.attachmentList;
      } else { //表头的附件
        deleteFileInfo = this.fileList[index];
        data = this.fileList;
      }
      data.splice(index, 1);
      this.$emit('updateAppendix', event, deleteFileInfo, this.params.condition, 'delete', index);
      event.stopPropagation();
    },

    //点击附件信息打开下拉框
    openDropdown: function (index, event, filelist) {
      if (!filelist.id) {
        return false;
      }
      if (this.params.isFileView) {
        // 预览
        this.previewAppendix(filelist, event);
      } else {
        // 下载
        this.downloadAppendix(filelist.id, event);
      }
      // _commonJs.openAppendixDropdown(index, event, 3);
    },

    /**
     * 校验表单数据
     * @param callback 回调函数
     */
    validate(cb, type, status, isValidate) {
      this.operateType = type;
      this.status = status;
      let { condition = {} } = this.params;
      const { regionType, isEditable, name, rules, valueId, showFormat, datagridCode, columnType, oldMultiLists = {}, newMultiLists = {} } = condition;
      // 使用 async-validator
      const validator = new AsyncValidator({ [name]: rules });
      const noValidateStatus = ['cancel', 'reject'];
      let value = condition.fileList;
      if (datagridCode) value = condition.value;
      let model = { [name]: value };
      validator.validate(
        model,
        { keys: [name], first: true, firstFields: true, debug: true },
        (errors, type) => {
          this.isShowMes = errors ? true : false;
          this.message = errors ? errors[0].message : "";

          if (
            this.operateType == "save" &&
            !isValidate &&
            this.message &&
            this.message.indexOf(this.$t('validate.error.empty')) > -1
          ) {  // 保存不验证非空, 验证格式
            this.message = "";
          } else if (
            this.operateType == "submit" &&
            noValidateStatus.includes(this.status) &&
            this.message
          ) { // 驳回 作废不验证
            this.message = "";
          } else if (this.isShowMes) {
            if (
              (condition.type == "date" || condition.type == "select") &&
              this.message &&
              this.message.indexOf(this.$t('validate.error.empty')) > -1
            ) { //日期类型和type为select类型字段为空不校验
              if (this.operateType == "submit") {
                this.myToast(this.message);
                this.scrollToError(this);
              } else if (!this.workflowEnabled && this.operateType == "save") { //基础单据保存，需要校验非空
                this.myToast(this.message);
                this.scrollToError(this);
              } else {
                this.message = "";
              }
            } else if (regionType === "DATAGRID" && isEditable === false) { // 表格不可编辑不校验
              this.message = "";
            } else {
              this.myToast(this.message);
              this.scrollToError(this);
            }
          }
          if (cb && typeof cb == "function") cb(this.message);
        }
      );
    },

    //获取DOM元素到页面顶部的距离
    getElementToPageTop: function (el) {
      let actualTop = el.offsetTop;
      let current = el.offsetParent;
      while (current !== null) {
        actualTop += current.offsetTop;
        current = current.offsetParent;
      }
      return actualTop;
    },

    //滚动条滑动到错误字段位置
    scrollToError: function (el) {
      var data = this.params.condition;
      // 页签滚动
      this.getCompByName('tabComp').switchTab(data.tabIndex, () => {
        var top = this.getElementToPageTop(el.$el);
        var elOffsetTop = el.$el.offsetTop;
        if (document.querySelector(".m_head")) {
          var headerHeight = document.querySelector(".m_head").scrollHeight;
          document.querySelector(".m_centerBox").scrollTop =
            top - headerHeight - elOffsetTop;
        } else {
          document.querySelector(".m_centerBox").scrollTop = top - elOffsetTop;
        }
      });
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

    //获取附件类型，并根据附件类型设置class样式
    getAppendixType: function (appendixName) {
      return _commonJs.getAppendixTypes(appendixName);
    },

    // 获取condition
    getCondition: function () {
      // return this.$refs.input;
      return this.params.condition;
    },

    // 上传
    async upLoadFile(formData, url, callback) {
      const ticket = addTicket();
      let res = await this.$axios
        .post(url, formData, {
          headers: {
            "Content-Type": "multipart/form-data", //hearder 很重要，Content-Type 要写对
            ...(ticket ? { Authorization: ticket } : {})
          }
        }).catch(error => {
          let data = error.response.data;
          let errorMsg = data.message;
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
    initNewName: function () {
      if (this.newName) {
        this.params.newName = this.newName;
      }
      if (this.rowIndex) {
        this.params.index = this.rowIndex;
      }
    }
  }
};
</script>

<!-- 样式加载 -->
<style lang="less" scoped>
@import "./index.less";
.m_box .m_wrap {
  padding: 0;
}
</style>
<style lang="css">
.viewer-footer {
  display: none;
}
</style>




// WEBPACK FOOTER //
// src/components/cellComp/upload/index.vue