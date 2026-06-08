import { getQueryType } from "@/assets/js/util/common.js";
import _commonJs from "@/assets/js/itemList/index.js";
import { Indicator } from "mint-ui";

export default {
  created() {
    this.$on("createRef", (name, ref) => {
      if (!this.labelRef) this.labelRef = {};
      this.labelRef[name] = ref;
    });
  },
  mounted() {
    // 注册全局方法
    this.exportFunc(`setValueByName`, this.setValueByName); // 修改值
    this.exportFunc(`getValueByName`, this.getValueByName); // 获取值
    this.exportFunc(`getconditionByName`, this.getconditionByName); // 获取condition
    this.exportFunc(`addBtn`, this.addBtn); // 添加按钮
    this.exportFunc(`refreshFormData`, this.refreshFormData); // 刷新 form数据
    this.exportFunc(`refreshDatagridData`, this.refreshDatagridData); // 刷新 datagrid数据
    this.exportFunc(`openRefSelecFunc`, this.openRefSelecFunc); // 参照方法
    this.exportFunc(`updateRefer`, this.updateRefer);
    this.exportFunc(`deleteRow`, this.deleteRow); //删行
    this.exportFunc(`addRow`, this.addRow); //增行
    this.exportFunc(`getDatagridRowNum`, this.getDatagridRowNum); //获取表格的行数
    this.exportFunc(`getRefConditionByName`, this.getRefConditionByName); //获取参照参数
    this.exportFunc(`getRefParamByName`, this.getRefParamByName); //获取参照参数
    this.exportFunc(`setRefParamByName`, this.setRefParamByName); //设置参照参数
    this.exportFunc(`hideHeadRowByName`, this.hideHeadRowByName); //隐藏表头一行
    this.exportFunc(`showHeadRowByName`, this.showHeadRowByName); //显示表头一行
    this.exportFunc(`setReadOnlyByName`, this.setReadOnlyByName); //设置字段是否只读
    this.exportFunc(`setControlStyleByName`, this.setControlStyleByName); //设置控件的样式
    this.exportFunc(`updateAppendix`, this.updateAppendix); //显示附件预览界面
  },
  methods: {
    /**
     * 修改值通过name参数
     * @param {string} name key值
     * @param {string} value 显示值
     * @param {string} valueId 保存提交值
     */
    setValueByName: function(name, value, valueId) {
      if (name) {
        let condition = this.$refs.editForm.getFiledsByName(name);
        condition.value = value;
        condition.valueId = valueId;
      }
    },

    /**
     * 获取value值通过name参数
     * @param {string} name
     * 1.表头字段：name
     * 2.表体字段：datagridcode_row_name
     * @return {string} value值
     */
    getValueByName: function(name) {
      if (name) {
        const condition = this.$refs.editForm.getFiledsByName(name);
        return condition.value;
      }
    },

    /**
     * 获取condition值
     * @param {string} name key值 datagridcode_row_key
     * @return {array} condition值
     */
    getconditionByName: function(name) {
      if (name) {
        const condition = this.$refs.editForm.getFiledsByName(name);
        return condition;
      }
    },

    /**
     * 添加自定义按钮
     * @param {function} func 自定义点击事件
     * @param {string} className class名称
     * @param {string} html 自定义html
     */
    addBtn: function(func, className, html) {
      // 渲染按钮
      this.btnParam.isShow = true;
      this.btnParam.html = html ? html : "";
      this.btnParam.className = className ? "" : "m-btn";
      // $this.btnParam.tit = tit;

      if (func && typeof func == "function") {
        this.btnParam.func = func;

        // 参照回调例子
        // $this.btnParam.func = function(){
        //   vue.openRefSelecFunc({
        //     url: '/vue/itemList.html#/',
        //     multable: 0,
        //   }, function(list){
        //     console.log(list);
        //     let url = `/wupin/wpApply/wpApply/edit__mobile__.action?entityCode=wupin_1.0.0.00_wpApply&id=${list.id}&__pc__=VGFza0V2ZW50XzE1MGQ2ZmxfcmVmNzEyX3JlZjI3NXwzNDM_&dataJson=1`
        //     vue.refreshFormData(url);
        //     vue.refreshDatagridData(`/wupin/wpApply/wpApply/data-dg1563325923861.action?datagridCode=wupin_1.0.0.00_wpApply_edit__mobile__dg1563325923861&wpApply.id=1194&rt=json`);
        //   });
        // }
      }
    },

    // 传入接口重新渲染表单数据
    refreshFormData: function(url, isReferCopy, id) {
      this.$refs.editForm.getFormData(url, isReferCopy, id);
    },

    // 传入接口重新渲染datagrid
    refreshDatagridData: function(url, isReferCopy, dtCode) {
      const dt = this.$refs.editForm.getDataGrid(dtCode);
      dt.getDataGridData(url, "", isReferCopy);
      // this.$refs.editForm.getDatagridByLayout(url, "", isReferCopy, dtCode);
    },

    /**
     * 打开参照
     * @params data 数据格式
     * {
     *   url: "",
     *   multable: 0, 单选，多选
     *   referenceview:{
     *      code: "" ,
     *      iscrosscompany: "", 跨集团
     *   }
     * }
     */
    openRefSelecFunc: function(data, callbackFunc, goBack) {
      const $this = this;
      // 获取参照参数，跨公司 回填到url
      let url = data.url;
      if (!url && url == "") {
        this.myToast(this.$t("notice.invalid.address"));
        return false;
      }

      if (!data.referenceview) {
        data.referenceview = {
          code: new Date().getTime().toString(),
          iscrosscompany: ""
        };
      }

      $this.showRefer = true;
      $this.referConfig.url = _commonJs.resolveReferUrl(data.url, data);
      // $this.$children[0].isReadOnly = true;

      // $this.$children[0].$forceUpdate();
      Indicator.open({
        text: this.$t("notice.loading"),
        //文字
        spinnerType: "fading-circle"
        //样式
      });
      $this.$nextTick(function() {
        // 回调方法
        let iframe = document.getElementById("newPage");
        iframe.onload = function() {
          Indicator.close();
          if (iframe) {
            // 去掉原生头部
            //   try {
            //       window.mobilejs.open_dialog();
            //   } catch (err) {}
            // 注册方法
            // 选择回调方法
            let code = "";
            if (data.referenceview.code)
              code = data.referenceview.code.replace(/\./gi, "_");
            iframe.contentWindow[`_callBack_ec_${code}`] = function(list) {
              // 恢复原生头部
              //   try {
              //       window.mobilejs.close_dialog();
              //   } catch (err) {}

              setTimeout(function() {
                $this.showRefer = false;
              }, 200);

              if (list && list.length > 0) {
                list = list[0];
              }

              // 回调赋值
              if (callbackFunc && typeof callbackFunc == "function") {
                callbackFunc(list);
              }
            };

            // 返回按钮方法
            iframe.contentWindow[`_close_ec_${code}`] = function() {
              // 关闭iframe
              setTimeout(function() {
                $this.showRefer = false;
              }, 200);

              //返回回调
              if (goBack && typeof goBack == "function") {
                goBack();
              }
              // 恢复原生头部
              //   try {
              //       window.mobilejs.close_dialog();
              //   } catch (err) {}
            };
          }
        };
      });
    },

    updateRefer: function(flag, url) {
      this.showRefer = flag;
      if (url) this.referConfig.url = url;
    },

    /**
     * 删行
     * @param {string} datagridCode 表格的datagridCode
     * @param {int} index 行号，index从0开始
     * @param {function} beforeCallbackFunc 执行增行之前的方法
     * @param {function} callbackFunc 执行增行之后的方法
     */
    deleteRow: function(
      datagridCode,
      index = 0,
      beforeCallbackFunc,
      callbackFunc
    ) {
      const datagrid = this.$refs.editForm.getDataGrid(datagridCode);
      if (beforeCallbackFunc) {
        beforeCallbackFunc();
      }
      datagrid.delDataGrid(index);
      this.$nextTick(() => {
        if (callbackFunc) {
          callbackFunc();
        }
      });
    },

    /**
     * 增行
     * @param {string} datagridCode 表格的datagridCode
     * @param {function} beforeCallbackFunc 执行增行之前的方法
     * @param {function} callbackFunc 执行增行之后的方法
     */
    addRow: function(datagridCode, beforeCallbackFunc, callbackFunc) {
      const datagrid = this.$refs.editForm.getDataGrid(datagridCode);
      if (beforeCallbackFunc) {
        beforeCallbackFunc();
      }
      datagrid.addDataGrid();
      this.$nextTick(function() {
        if (callbackFunc) {
          callbackFunc();
        }
      });
    },

    /**
     * 获取表格的行数
     * @param {string} datagridCode 表格的datagridCode
     * @return {num} 行数
     */
    getDatagridRowNum: function(datagridCode) {
      let editform = this.$refs.editForm;
      // let layout = editform.layout;
      let num;
      const datagrid = editform.getDataGrid(datagridCode);
      return datagrid.dataSource.length;
    },

    /**
     * 获取参照参数
     * @param {string} name key值 datagridcode_row_key
     * @return {string} refCondition值
     */
    getRefConditionByName: function(name) {
      if (name) {
        const condition = this.$refs.editForm.getFiledsByName(name);
        return condition.refCondition;
      }
    },

    /**
     * 获取参照参数包含API设置的
     * @param {string} name key值 datagridcode_row_key
     * @return {string} refCondition值
     */
    getRefParamByName: function(name) {
      if (name) {
        const item = this.$refs.editForm.getFiledsByName(name);
        const { refCondition } = item;
        let refCon = {};
        console.log(item);
        if (refCondition) {
          const refConf = new Function(refCondition)();
          const encodeUrl = encodeURI(refConf); // 参数转码处理
          refCon = vue.GetRequest(`?${encodeUrl}`);
        }
        if (!item.refConditionAPI) item.refConditionAPI = {};
        const res = Object.assign({}, refCon, item.refConditionAPI);
        return res;
      }
    },

    /**
     * 设置参照参数
     * @param {string} name key值 datagridcode_row_key
     * @param {object} data 设置的参数对象值{key:value}
     */
    setRefParamByName: function(name, data) {
      if (name) {
        const item = this.$refs.editForm.getFiledsByName(name);
        if (!item.refConditionAPI) item.refConditionAPI = {};
        item.refConditionAPI = Object.assign({}, item.refConditionAPI, data);
        return item.refConditionAPI;
      }
    },

    /**
     * 隐藏表头一行
     * @param {string} name key值 datagridcode_row_key
     * @return {string} refCondition值
     */
    hideHeadRowByName: function(name) {
      const obj = this.getconditionByName(name);
      const labelObj = this.labelRef[`${name}_label`];
      if (obj) {
        obj.isHidden = true;
        if (labelObj) labelObj.isHidden = true;
      }
    },

    /**
     * 显示表头一行
     * @param {string} name key值 datagridcode_row_key
     * @return {string} refCondition值
     */
    showHeadRowByName: function(name) {
      const obj = this.getconditionByName(name);
      const labelObj = this.labelRef[`${name}_label`];
      if (obj) {
        obj.isHidden = false;
        if (labelObj) labelObj.isHidden = false;
      }
    },

    /**
     * 通过name设置字段只读
     * @param {string} name 1.表头字段：key值；2.表体字段：datagridcode_row_key
     * @param {boolean} isReadOnly 设置字段只读或者可编辑
     */
    setReadOnlyByName: function(name, isReadOnly) {
      if (name) {
        let condition = this.$refs.editForm.getFiledsByName(name);
        if (isReadOnly === true) {
          condition.readonly = true;
          condition.placeholder = "";
        } else if (isReadOnly === false) {
          var queryType = getQueryType(condition);
          condition.readonly = false;
          if (
            queryType == "date" ||
            queryType == "select" ||
            queryType == "choose"
          ) {
            condition.placeholder = this.$t("placeholder.choose.text");
          } else if (queryType == "text" || queryType == "longtext") {
            condition.placeholder = this.$t("placeholder.enter.text");
          }
        }
      }
    },

    /**
     * 设置控件的样式
     * @param {string} name 1.表头字段：key值；2.表体字段：datagridcode_row_key
     * @param {string} style 控件的自定义样式,直接写css样式
     */
    setControlStyleByName: function(name, style = "") {
      const obj = this.getconditionByName(name);
      if (obj && style) {
        obj.cssstyle = style;
      }
    },

    updateAppendix: function(flag, url, id, name = "") {
      this.showAppendix = flag;
      if (url) this.referConfig.url = url;
      this.appendixId = id;
      this.fileName = name;
    }
  }
};



// WEBPACK FOOTER //
// ./src/pages/edit/api.js