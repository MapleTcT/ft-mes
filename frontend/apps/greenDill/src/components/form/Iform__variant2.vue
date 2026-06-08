<template>
  <form
    enctype="multipart/form-data"
    ref="form"
    method="post"
    name="form"
    v-on:submit.prevent="checkForm"
  >
    <slot></slot>
  </form>
</template>

<script>
import _commonJs from "@/assets/js/itemList/index.js"; //引用模块进来
import { Toast, Indicator, MessageBox } from "mint-ui";
import { publicGetAxios, tryCatch } from '../../assets/js/util/common';
import { mergeWith, union, isArray } from 'lodash';
import { isInteger, isLong, isDecimal } from '@/assets/js/util/validate.js';

let fieldsData = {};

export default {
  name: "iForm",
  data() {
    return {
      fields: [],
      dataGrid: [],
      formUrl: "",
      fieldsData: {}
    };
  },
  props: {
    params: {
      type: Object,
      default: {}
    }
  },
  provide() {
    return {
      form: this
    };
  },
  methods: {

    /** 
     * 取消 保存 提交数据
     * @param {object} params 提交的配置项
    */
    submitData: function (params = {}) {
      const $this = this;
      const { operateType, status, isValidate } = params;
      if (operateType == "cancel") { // 取消
        MessageBox({
          title: this.$t('message.notice.title'),
          message: this.$t('message.modify.confirm'),
          showCancelButton: true
        }).then(action => {
          if (action == "confirm") {
            // 调原生方法返回上一级
            setTimeout(function () {
              try {
                window.mobilejs.close();
              } catch (err) {
                try {
                  window.history.back();
                } catch (err) {

                }
              }
            }, 1000);
          }
        });
        return;
      }

      // 保存数据不校验非空, 校验格式, 作废单据不校验, isValidate为true的时候都校验
      $this.validate((valid) => {
        if (valid) {
          $this.getFormData(params);
          console.log("提交成功");
        } else {
          console.log("校验失败");
        }
      }, operateType, status, isValidate);

    },

    /** 
     * 组织表单参数
     * @param {object} params 提交的配置项
    */
    getFormData(params = {}) {
      const $this = this;
      const { onSaveEvent = '' } = this.params;
      const { submitUrl, setType = () => { }, id: billId, pendingId, deploymentId } = this.params;
      const { operateType, status, saveCommentParams, workParams } = params;
      let isAndroidLessVersion = _commonJs.isLessEqualAndroidVersion(7);
      let viewCode = window.pageConfig && window.pageConfig.viewCode;
      const { isMain } = window.pageConfig;
      // let userid = $this.userId; //user.id参数
      // let staffId = this.params.getUserInfo().staffId;
      if (isMain !== false && !billId) {
        const position = this.params.getUserInfo() || {};
        const { positions = {} } = position;
        const { id: positionId, layRec } = positions;
        if (positionId && layRec) {
          this.setFiledsData(`${window.pageConfig.modelAlias}.positionLayRec`, layRec);
          this.setFiledsData(`${window.pageConfig.modelAlias}.createPositionId`, positionId);
        }
      }

      setType({ operateType: operateType, workFlowVarStatus: status });
      let requestParams = $this.GetRequest(window.location.href);

      this.setFiledsData("viewCode", viewCode);

      this.setFiledsData("operateType", operateType);

      //超级编辑单据
      if (requestParams && requestParams.superEdit) {
        this.setFiledsData("superEdit", true);
        this.setFiledsData("operateType", "save");
      }

      deploymentId && this.setFiledsData("deploymentId", deploymentId);

      //非新增单据需传pendingId，参数pendingId从地址栏获取
      billId && pendingId && this.setFiledsData("pendingId", pendingId);

      this.$parent.$nextTick(() => {
        // loading加载
        Indicator.open({
          text: this.$t('notice.loading'),
          //文字
          spinnerType: 'fading-circle',
          //样式
        });

        //自定义保存onSave
        console.log($("#operateType").val());
        if (tryCatch(new Function(onSaveEvent)) === false) {
          setTimeout(function () {
            Indicator.close();
          }, 1000);
          return false;
        };

        // 获取表单参数
        this.fields.forEach((item, i) => {
          $this.getAllFiledsData(item);
        });

        //组织图片和附件参数
        if ($this.getPicAppendixData() == false) {
          return false;
        }

        // 组织工作流参数
        this.getWorkParams(params);

        // 组织complex参数
        this.getComplexParams();

        //组织datagrid参数
        $this.submitDataGridData();
        // 提交数据
        this.submitRequest(submitUrl, fieldsData, params, this.params);
      });
    },

    /** 
     * 提交数据
     * @param {string} url 接口地址
     * @param {object} tempData 提交的数据
     * @param {object} params 
    */
    submitRequest: function (url, tempData, params = {}, urlParams = {}) {
      publicGetAxios({
        url: url,
        data: tempData,
        type: 'post'
      }, (response) => {
        Indicator.close();
        if (response.data) {
          Toast({
            message: this.$t('message.operate.success'),
            iconClass: 'mintui mintui-success'
          });
          let openUrlFunc;
          if (params.operateType == "save") {
            // 刷新当前页
            openUrlFunc = function () {
              let { href } = window.location;
              if (!urlParams.id) { // 新增
                href = href.replace(`deploymentId=${urlParams.deploymentId}`, `pendingId=${response.data.pendingId}`);
                href = `${href}&id=${response.data.id}&tableInfoId=${response.data.tableInfoId}`;
              }
              window.location.href = href;
              // window.open(href, '_self');
            }

          } else {
            // 调原生方法返回上一级
            openUrlFunc = function () {
              // window.history.back();
              try {
                window.mobilejs.close();
              } catch (err) {
                try {
                  window.history.back();
                } catch (err) {

                }
              }
            }

          }
          setTimeout(function () {
            openUrlFunc();
          }, 1000);

        } else {
          Toast({
            message: this.$t('message.operate.fail'),
            iconClass: 'mintui mintui-error'
          });
        }
      }).catch((err) => {
        let data = err.response.data;
        if (data && data.code !== 200) {
          let errorMsg = data.message;
          errorMsg = _commonJs.encodeHtml(errorMsg);
          Indicator.close();
          Toast({
            message: errorMsg,
            iconClass: "mintui mintui-error"
          });
          return false;
        }
      })
    },

    // 组织datagrid参数
    submitDataGridData: function () {
      const $this = this;
      let layout = $this.layout;
      let dtAry = {
        dgList: {},
        dgDeletedIds: {}
      },
        dtCode;
      const { dataGrid = [] } = this;
      let viewType = window.pageConfig && window.pageConfig.viewType;
      for (let i = 0; i < dataGrid.length; i++) {
        const element = dataGrid[i];
        const res = element.getInterfaceParam();
        if (viewType == "VIEW") { //查看视图不保存表格数据
          res.dgList = null;
          res.dgDeletedIds = null;
        }
        dtAry.dgList = Object.assign({}, dtAry.dgList, res.dgList);
        dtAry.dgDeletedIds = Object.assign({}, dtAry.dgDeletedIds, res.dgDeletedIds);
      }
      fieldsData = mergeWith(fieldsData, dtAry);
      // return dtAry;
    },

    // 组织工作流参数
    getWorkParams: function (params) {
      const { workParams, status, saveCommentParams } = params;
      const taskDescription = this.$children[0].$refs.layout_tab_0[0].taskDescription;
      //保存的处理意见
      if (saveCommentParams) {
        this.setFiledsData("workFlowVar.comment", saveCommentParams.workFlowVar.comment);
        saveCommentParams.workFlowVar.activityType &&
          this.setFiledsData("workFlowVar.activityType", saveCommentParams.workFlowVar.activityType);
        saveCommentParams.workFlowVar.activityName &&
          this.setFiledsData("workFlowVar.activityName", saveCommentParams.workFlowVar.activityName);
        this.setFiledsData("activityName", saveCommentParams.workFlowVar.activityName);
      }
      taskDescription && this.setFiledsData("taskDescription", taskDescription);
      // 工作流参数
      if (workParams) {
        const { assignUser } = workParams.workFlowVar;
        this.setFiledsData(
          "workFlowVar.outcomeMapJson",
          workParams.workFlowVar.outcomeMapJson
        );

        this.setFiledsData(
          "workFlowVar.comment",
          workParams.workFlowVar.comment
        );

        this.setFiledsData(
          "workFlowVar.outcome",
          workParams.workFlowVar.outcome
        );

        workParams.workFlowVar.outcomeType && this.setFiledsData(
          "workFlowVar.outcomeType",
          workParams.workFlowVar.outcomeType
        );

        workParams.workFlowVar.activityName && this.setFiledsData(
          "workFlowVar.activityName",
          workParams.workFlowVar.activityName
        );

        workParams.workFlowVar.activityType && this.setFiledsData(
          "workFlowVar.activityType",
          workParams.workFlowVar.activityType
        );

        workParams.workFlowVar.countersignUsers && this.setFiledsData(
          "workFlowVar.countersignUsers",
          workParams.workFlowVar.countersignUsers
        );

        this.setFiledsData(
          "activityName",
          workParams.workFlowVar.activityName
        );

        this.setFiledsData("workFlowVarStatus", status);

        if (assignUser) {
          this.setFiledsData(`assignStaffSelect_${workParams.workFlowVar.outcome}MultiIDs`, assignUser);
          this.setFiledsData(`assignStaffSelect_${workParams.workFlowVar.outcome}AddIds`, assignUser);
        }
      }
    },

    // 组织complex参数
    getComplexParams: function () {
      let alias = window.pageConfig && window.pageConfig.modelAlias;
      let complexNameTit = `${alias}.extraCol`; // complex参数
      if (sessionStorage.getItem(complexNameTit)) {
        let complexData = sessionStorage.getItem(complexNameTit);
        complexData = JSON.parse(complexData);
        let complexHml = `<extra-data>`;
        for (let i = 0; i < complexData.length; i++) {
          const element = complexData[i];
          if (element.name) {
            let value = isArray(element.value) ? element.value.join(',') : element.value;
            if (element.id) {
              value = isArray(element.id) ? element.id.join(',') : element.id;
            }
            complexHml += `<${element.name}><![CDATA[${value}]]></${element.name}>`;
          }
        }
        complexHml += `</extra-data>`;
        this.setFiledsData(complexNameTit, complexHml);
        sessionStorage.setItem(complexNameTit, '');
      }
    },

    resetFields() {
      this.fields.forEach(field => field.resetField());
    },

    validate(cb, type, status, isValidate) {
      const $this = this;
      const { fields = [] } = this;
      $this.operateType = type;
      $this.status = status;
      return new Promise(resolve => {
        let valid = true,
          count = 0;
        fields.forEach((item = [], i) => {
          valid = true;
          count = 0;
          if (item.length > 0) {
            item.forEach((field, j) => {
              field && field.validate(error => {
                if (error) valid = false;
                if ((i === fields.length - 1 && ++count === item.length) || !valid) {
                  resolve(!valid);
                  if (typeof cb === "function") cb(valid);
                  throw new error();
                }
              }, $this.operateType, $this.status, isValidate);
            })
          } else if (item.length === 0 && i === fields.length - 1) {
            resolve(!valid);
            if (typeof cb === "function") cb(valid);
            throw new error();
          }
        });
      });
    },

    // 组织fileds的所有值
    getAllFiledsData(fields = []) {
      const $this = this;
      // let fields = $this.fields;
      if (fields && fields.length > 0) {
        // this.setFiledsData("viewCode", "wupin_1.0.0.00_wupin_edit__mobile__");

        for (let i = 0; i < fields.length; i++) {
          const element = fields[i];
          let condition = element.getCondition();
          let fieldVal = condition.formVal;
          if (condition.datagridCode || condition.type === 'attachment') { // datagrid
            continue;
          }

          // complex
          if (condition.complex) {
            let nameTit = `${window.pageConfig.modelAlias}.extraCol`;
            let complex = sessionStorage.getItem(nameTit);
            let complexVal = condition.valueId || fieldVal || '';
            if (condition.datetype == "date" || condition.datetype == "datetime" || condition.type === 'date')
              complexVal = _commonJs.timeStrToTimeStamp(complexVal, condition.showFormat);
            let complexTemp = { name: condition.name, value: complexVal };
            if (complex) {
              complex = JSON.parse(complex);
              complex.push(complexTemp);
              sessionStorage.setItem(nameTit, JSON.stringify(complex));
            } else {
              // this.setFiledsData(nameTit, JSON.stringify([complexTemp]));
              sessionStorage.setItem(nameTit, JSON.stringify([complexTemp]));
            }
            continue;
          }

          // 多选控件参数组织
          if (condition.columnType == "MULTSELECT") {
            const { multiIds = [], deleteIds = [], AddIds = [] } = $this.getMultiSelectData(condition) || {};
            this.setFiledsData(`${condition.name}MultiIDs`, multiIds.join(","));
            this.setFiledsData(`${condition.name}DeleteIds`, deleteIds.join(","));
            this.setFiledsData(`${condition.name}AddIds`, AddIds.join(","));
            continue;
          }

          // 参照
          if (condition.isPrefer && condition.columnType != "SYSTEMCODE") {
            let index = condition.name.lastIndexOf(".");
            let idName = condition.name.substring(0, index);
            if (condition.iscustom) {
              this.setFiledsData(idName, condition.valueId);
            } else {
              idName = `${idName}.id`;
              this.setFiledsData(idName, condition.valueId);
              this.setFiledsData(condition.name, fieldVal);
            }
            continue;
          }

          //时间和日期
          if (condition.datetype == "date" || condition.datetype == "datetime" || condition.type === 'date') {
            this.setFiledsData(condition.name, fieldVal ? _commonJs.timeStrToTimeStamp(fieldVal, condition.showFormat) : "");
            continue;
          }

          // systemCode
          if (condition && condition.columnType == "SYSTEMCODE") {
            if (condition.multable) { //系统编码多选
              this.setFiledsData(`${condition.name}`, condition.valueId ? condition.valueId : "");
            } else if (!condition.multable) { //系统编码单选
              // 自定义字段
              if (condition.iscustom) {
                this.setFiledsData(`${condition.name}`, condition.valueId ? condition.valueId : "");
              } else {
                this.setFiledsData(`${condition.name}.id`, condition.valueId ? condition.valueId : "");
              }

            }
            continue;
          }

          // 格式化小数
          if (condition.precision || condition.columnType == "DECIMAL") {
            let decimal = fieldVal;
            if (isDecimal(decimal)) {
              if (condition.showFormat == "PERCENT") { //百分比显示
                decimal = Number(decimal / 100).toString();
              } else {
                decimal = Number(decimal)
                  .toFixed(Number(condition.precision))
                  .toString();
              }
              // condition.value = decimal.toString();
            } else {
              decimal = "";
            }
            // condition.value = decimal;
            this.setFiledsData(`${condition.name}`, decimal);
            continue;
          }

          // 整型和长整型类型
          if (condition && condition.columnType == "INTEGER" || condition.columnType == "LONG") {
            let intValue = fieldVal;
            if (isInteger(intValue) || isLong(intValue)) {
              if (condition.showFormat == "PERCENT") { //百分比显示
                intValue = Number(intValue / 100).toString();
              } else {
                intValue = Number(intValue).toString();
              }
            } else {
              intValue = "";
            }
            // condition.value = intValue;
            this.setFiledsData(`${condition.name}`, intValue);
            continue;
          }

          // 布尔型
          if (condition && condition.columnType == "BOOLEAN") {
            if (condition.valueId || typeof condition.valueId == "boolean") {
              this.setFiledsData(condition.name, condition.valueId);
            } else {
              if (fieldVal === this.$t('option.boolean.yes')) {
                condition.valueId = true;
              } else if (fieldVal === this.$t('option.boolean.no')) {
                condition.valueId = false;
              } else {
                condition.valueId = "";
              }
              this.setFiledsData(condition.name, condition.valueId);
            }
          } else {
            condition && this.setFiledsData(condition.name, fieldVal);
          }
        }
      }
    },

    // 组织多选参数
    getMultiSelectData: function (data) {
      // if (!data.oldMultiLists || !data.newMultiLists) {
      //   return;
      // }
      const { oldMultiLists = {}, newMultiLists = {} } = data;
      let oldData = oldMultiLists.ids;
      let newData = newMultiLists.ids;
      let deleteIds = [],
        AddIds = [],
        multiIds = [];
      oldData = oldData ? oldData : [];
      newData = newData ? newData : [];
      multiIds = newData ? newData : [];

      deleteIds = oldData.filter(el => { return !newData.includes(el) });
      AddIds = newData;
      if (oldData.length) {
        AddIds = newData.filter(el => { return !oldData.includes(el) });
      }

      return {
        multiIds,
        deleteIds,
        AddIds
      }
    },

    //组织图片和附件
    getPicAppendixData: function () {
      let $this = this;
      let staffId = $this.params.getUserInfo().staffId;
      let appendix = $this.$root.$children[0].$refs.headbar.$refs.headBtn.$refs.appendix;
      let appendixFile = appendix && appendix.appendixFile;
      let uploadFileFormMap = [];
      let isRepeated = false; //附件是否重复
      if (vue.attachmentFile && JSON.stringify(vue.attachmentFile) !== "{}") { //附件字段
        var attachmentFile = [];
        Object.keys(vue.attachmentFile).forEach(attachment => {
          if (attachment.indexOf("filePath") > -1) {
            attachmentFile = vue.attachmentFile[attachment];
          }
        });
        //附件字段与附件页面的附件进行比较
        for (let i = 0; i < attachmentFile.length; i++) {
          if (isRepeated) {
            break;
          }
          var name = attachmentFile[i].slice(attachmentFile[i].lastIndexOf("\\") + 1);
          if (vue.appendixList && vue.appendixList.length > 0) {
            for (let j = 0; j < vue.appendixList.length; j++) {
              var appendixName = vue.appendixList[j].name;
              if (name == appendixName) {
                Toast({
                  message: this.$t('cellComp.upload.msg.exist'),
                  iconClass: "mintui mintui-error"
                });
                isRepeated = true;
                break;
              }
            }
          }
        }
        if (isRepeated) {
          return false;
        }
        uploadFileFormMap.push(vue.attachmentFile);
      }
      if (isRepeated) {
        return false;
      }
      if (appendixFile && JSON.stringify(appendixFile) !== "{}") { //头部附件页面的附件
        uploadFileFormMap.push(appendixFile);
      }
      if (vue.pictureFile && JSON.stringify(vue.pictureFile) !== "{}") { //图片
        uploadFileFormMap.push(vue.pictureFile);
      }

      this.setFiledsData("uploadFileFormMap", JSON.stringify(uploadFileFormMap)); //添加附件和图片

      if (
        (appendixFile && JSON.stringify(appendixFile) !== "{}") ||
        (vue.attachmentFile && JSON.stringify(vue.attachmentFile) !== "{}") ||
        (vue.pictureFile && JSON.stringify(vue.pictureFile) !== "{}")
      ) {
        this.setFiledsData("files_staffId", staffId);
      }
      let ids2del = "";
      if (vue.pictureFileDel || vue.attachmentFileDel) {
        ids2del = _commonJs.mergedArray(vue.pictureFileDel && vue.pictureFileDel.ids2del, vue.attachmentFileDel && vue.attachmentFileDel.ids2del);
        ids2del = ids2del ? ids2del.join(",") : "";
      }
      this.setFiledsData("ids2del", ids2del); //删除的图片和附件
    },

    // 删除区域
    deleteFileds: function (name) {
      const $this = this;
      if ($this.fields) {
        $this.fields.forEach((item = [], i) => {
          item.forEach((field, j) => {
            if (field && field.$refs[name]) {
              item.splice(j, 1);
            }
          })
        });
      }
    },

    // 保存提交的值
    setFiledsData: (key, value) => {
      const tempdata = {};
      if (
        value !== "" &&
        !isNaN(Number(value)) &&
        key.indexOf(".id") == -1 &&
        key.indexOf("dgDeletedIds") == -1 &&
        key.indexOf("dgList") == -1 &&
        key.indexOf("deploymentId") == -1 &&
        key.indexOf("pendingId") == -1 &&
        key.indexOf("uploadFileFormMap") == -1 &&
        key.indexOf("files_staffId") == -1 &&
        key.indexOf("ids2del") == -1 &&
        key.indexOf(".positionLayRec") == -1 &&
        key.indexOf(".createPositionId") == -1
      ) {
        let numberType = ["INTEGER", "LONG", "DECIMAL", "MONEY", "DATE", "DATETIME"];
        let columnType = vue.getconditionByName(key) && vue.getconditionByName(key).columnType;
        if (numberType.indexOf(columnType) > -1) {
          value = Number(value);
        }
      }
      if (key.indexOf(".id") > -1) {
        if (value !== "" && !isNaN(Number(value))) {
          value = Number(value);
        }
      }
      if (value === "null") {
        value = null;
      } else if (value === "true") {
        value = true;
      } else if (value === "false") {
        value = false;
      }

      // 格式化数组
      if (value && Object.prototype.toString.call(value) === "[object Array]") {
        value = value.join(",");
      }

      if (key.indexOf("uploadFileFormMap") > -1) { //附件字段
        value = JSON.parse(value);
      }

      // jsonData[key] = value;
      if (key && key.indexOf('.') > -1) {
        const arr = key.split('.');
        let temp = '';
        for (let i = 0; i < arr.length; i++) {
          const nameKey = arr[i];
          temp = `${temp}.${nameKey}`;
          try {
            if (i !== arr.length - 1) {
              eval(`if(!tempdata${temp}) tempdata${temp} = {}`);
            } else {
              eval(`tempdata${temp} = value`);
            }
          } catch (error) {
            eval(`tempdata${temp}={}`);
            console.log(error);
          }
        }
      } else {
        fieldsData[key] = value;
      }
      fieldsData = mergeWith(fieldsData, tempdata);
    },

    getFields() {
      const curFields = [];
      this.fields.forEach((item = []) => {
        item.forEach((field) => {
          curFields.push(field);
        })
      })
      return curFields;
    }
  },

  created() {
    this.$on("form-add", field => {
      if (field) {
        const { tabIndex = 0 } = field.params.condition;
        if (!this.fields[tabIndex - 1]) this.fields[tabIndex - 1] = [];
        this.fields[tabIndex - 1].push(field);
      }
    });

    this.$on("form-dataGird", field => {
      if (field) this.dataGrid.push(field);
      console.log(this.dataGrid)
    });

    this.$on('deleteFileds', (name = '') => {
      this.deleteFileds(name);
    });

    this.$on('getFields', () => {
      this.getFields();
    });

    this.$on('setFiledsData', (key, value) => {
      this.setFiledsData(key, value);
    })
  }
};
</script>


// WEBPACK FOOTER //
// src/components/form/Iform.vue