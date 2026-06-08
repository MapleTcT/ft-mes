import { Indicator } from "mint-ui";
import _commonJs from "@/assets/js/itemList/index.js";

export default {
  mounted() {
    // 注册全局方法
    this.exportFunc(`getItemListByTableNo`, this.getItemListByTableNo); //获取单据的信息
    this.exportFunc(`getItemListData`, this.getItemListData); //获取列表数据
    this.exportFunc(`getItemListNum`, this.getItemListNum); //获取列表的单据个数
    this.exportFunc(`getItemListResult`, this.getItemListResult); //获取列表单据信息
    this.exportFunc(`setItemValueByName`, this.setItemValueByName); //给高级查询条件赋值
    this.exportFunc(`getSearchData`, this.getSearchData); //列表视图查询接口
    this.exportFunc(`setItemListStyle`, this.setItemListStyle);
    this.exportFunc(`setClickEvt`, this.setClickEvt);
    this.exportFunc(`loadMore`, this.loadMore); // 下拉刷新
    this.exportFunc(`setMultipleSelect`,this.setMultipleSelect);// 设置多选
    this.exportFunc(`getSelected`,this.getSelected);// 获取多选数据
    this.exportFunc(`openRefSelecFunc`,this.openRefSelecFunc);// 获取多选数据
  },
  methods: {
    /**
     * 根据单据编号获取单据的信息
     * @param {string} tableNo 单据编号
     * @return {array} 单据信息值
     */
    getItemListByTableNo: function(tableNo) {
      let itemlist = [];
      let listData = this.listData;
      for (let i = 0; i < listData.length; i++) {
        if (listData[i].tableNo === tableNo) {
          itemlist.push(listData[i]);
          break;
        }
      }
      return itemlist;
    },

    /**
     * 获取列表的数据信息,区分代办查询和全部查询
     * @param {string} type
     * @param {function} callback
     * @param {boolean} isAsync  同步异步处理
     * 1.type为query时，全部查询
     * 2.type为pending时，代办查询
     * totalCount单据个数, totalResult单据信息
     */
    getItemListData: function(
      type = "query",
      callback = () => {},
      isAsync = false
    ) {
      let url;
      if (window.pageConfig) {
        url = window.pageConfig.interfaceApi.sourceData + type;
      }
      const backRes = $.ajax({
        url:
          url.indexOf("http://") > -1
            ? url
            : `${$.ajaxSettings.baseURL || ""}${url}`,
        async: isAsync,
        type: "post",
        dataType: "json",
        headers: {
          Accept: "application/json; charset=utf-8",
          "Content-type": "application/json;charset=UTF-8"
        },
        data: JSON.stringify({
          classifyCodes: "",
          customCondition: {},
          pageNo: 1,
          pageSize: 65535,
          paging: true
        }),
        success: res => {
          console.log(res);
          callback(res);
        },
        error: res => {
          console.log(res);
          callback(res);
        }
      });
      if (backRes && backRes.response) {
        return JSON.parse(backRes.response);
      }
      return backRes;
    },

    /**
     * 获取列表的单据个数,区分代办查询和全部查询
     * @param {string} type
     * 1.type为query时，全部查询
     * 2.type为pending时，代办查询
     * @return {num} itemListNum单据个数
     */
    getItemListNum: function(type) {
      const res = this.getItemListData(type);
      if (res) return res.data.totalCount;
      return "";
    },

    /**
     * 获取列表全部的单据数据信息,区分代办查询和全部查询
     * @param {string} type
     * 1.type为query时，全部查询
     * 2.type为pending时，代办查询
     * @return {array} itemListResult单据信息
     */
    getItemListResult: function(type) {
      const res = this.getItemListData(type);
      if (res) return res.data.result;
      return "";
    },

    /**
     * 给高级查询条件赋值
     * @param {string} name 高级查询字段的data-ref的值
     * @param {string} value 显示值
     */
    setItemValueByName: function(name, value) {
      let searchInput = this.$refs.searchBar.$refs.sidePanel.$refs;
      if (name && searchInput[name]) {
        let condition = searchInput[name][0].params;
        condition.value = value;
      }
    },

    /**
     * 设置样式API
     * @param {string} namekey 列表字段值
     * @param {string} style 样式 暂支持 textAlign isHidden
     * @param {string} type 是否是label
     */
    setItemListStyle: function(namekey, style = {}, type) {
      const { layout = {} } = this;
      const { textAlign, ...rest } = style;
      const { sort = [] } = layout;
      sort.forEach((list = []) => {
        list.forEach((el = {}) => {
          const { element = {}, showType } = el;
          const { key } = element;
          if (key && key === namekey) {
            if (showType && showType === type) {
              element.textalign = textAlign;
            } else if (!type) {
              element.textalign = textAlign;
            }
          }
        });
      });
    },

    // 点击事件
    setClickEvt: function(func) {
      if (func) {
        this.APIclickEvt = func;
      }
    },

    /**
     *列表视图查询接口，与APP端配合，实现返回列表视图只刷新待办的功能
     */
    getSearchData: function() {
      let queryType = window.localStorage.getItem("queryType"); //区分快速查询和高级查询
      let searchType = window.localStorage.getItem("searchType"); //区分快速查询的全部查询和待办查询
      var searchBar = this.$refs.searchBar;
      if (queryType && queryType == "advancedQuery") {
        //高级查询
        let sidePanel = this.$refs.searchBar.$refs.sidePanel;
        sidePanel.getSearchData();
      } else {
        //快速查询
        searchBar.getSearchData({
          dataId: searchType
        });
      }
    },

    // 上拉加载数据
    loadMore: function() {
      this.topStatus = "loading";
      this.$refs.loadmore.translate = 50;
      const el = document.querySelector(".page-loadmore-wrapper");
      if (el) el.scrollTop = 0;
      this.loadTop();
      // 隐藏遮罩层
      try {
        this.$refs.searchBar.$refs.footerBar.$refs.headBtn.popupVisible = false;
        this.$refs.headbar.$refs.headBtn.popupVisible = false;
      } catch (error) {
      }
    },
    // 设置多选
    setMultipleSelect: function(flag = true){
      this.isListMulti = flag;
    },

    // 获取多选数据
    getSelected: function(){
      return this.checklist;
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
      // 获取参照参数，跨公司 回填到url
      let url = data.url;
      const viewPanel = this.$refs.searchBar.$refs.sidePanel;
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
      
      viewPanel.showRefer = true;
      viewPanel.referConfig.url = _commonJs.resolveReferUrl(data.url, data);
      Indicator.open({
        text: this.$t("notice.loading"),
        //文字
        spinnerType: "fading-circle"
        //样式
      });
      viewPanel.$nextTick(()=> {
        // 回调方法
        let iframe = document.getElementById("newPage");
        iframe.onload = function() {
          Indicator.close();
          if (iframe) {
            // 注册方法
            // 选择回调方法
            let code = "";
            if (data.referenceview.code)
              code = data.referenceview.code.replace(/\./gi, "_");
            iframe.contentWindow[`_callBack_ec_${code}`] = function(list) {
              setTimeout(function() {
                viewPanel.showRefer = false;
              }, 200);

              // 回调赋值
              if (callbackFunc && typeof callbackFunc == "function") {
                callbackFunc(list);
              }
            };

            // 返回按钮方法
            iframe.contentWindow[`_close_ec_${code}`] = function() {
              // 关闭iframe
              setTimeout(function() {
                viewPanel.showRefer = false;
              }, 200);

              //返回回调
              if (goBack && typeof goBack == "function") {
                goBack();
              }
            };
          }
        };
      });
    },
  },

};



// WEBPACK FOOTER //
// ./src/pages/itemList/api.js