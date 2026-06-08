<template>
  <div class="m_goodsBoxWrap">
    <div class="page-loadmore">
      <!-- 上拉 下拉 -->
      <div
        class="page-loadmore-wrapper needsclick"
        ref="wrapper"
        :style="{ height: wrapperHeight + 'px' }"
      >
        <mt-loadmore
          :class="'needsclick'"
          :top-method="loadTop"
          @translate-change="translateChange"
          @top-status-change="handleTopChange"
          :bottom-method="loadBottom"
          @bottom-status-change="handleBottomChange"
          :bottom-all-loaded="allLoaded"
          ref="loadmore"
        >
          <div
            class="m_goodsBox needsclick"
            v-for="(list, index) in listata"
            v-on:click="checkPeopleNum(index)"
          >

            <!-- 內容部分 -->
            <div
              class="m_staffBox"
              v-if="listType=='flow'"
            >
              <div
                class="ms_check"
                v-if="isSingleCheck == '0'"
              >
                <div class="ckb_wrap">
                  <i :class="list.isChecked ? 'inp_ckb_checkbox checked': 'inp_ckb_checkbox'"></i>
                  <!-- <span class="inp_ckb"></span>
                  <input class="inp_ckb" type="checkbox" v-model="list.isChecked">
                  <label for="checkList"></label> -->
                </div>
              </div>
              <div class="ms_info">
                <img
                  class="faceimg"
                  src="@/assets/images/figure01.png"
                  alt=""
                >
                <div class="info_box">
                  <span class="name">{{list.name}}</span>
                  <br>
                  <span class="subinfo">{{list.code}}</span>
                </div>
                <div class="info_box fr">
                  <span class="position">
                    {{showMainPosition(list, 'position')}}
                  </span>
                  <br>
                  <span class="subinfo">{{showMainPosition(list,'department')}}</span>
                </div>
              </div>
            </div>
            <!-- 內容部分 -->
          </div>
          <div
            slot="top"
            class="mint-loadmore-top"
          >
            <span
              v-show="topStatus !== 'loading'"
              :class="{ 'is-rotate': topStatus === 'drop' }"
            >↓</span>
            <span v-show="topStatus === 'loading'">
              <mt-spinner
                type="fading-circle"
                color="#294fbc"
              ></mt-spinner>
            </span>
          </div>
          <div
            slot="bottom"
            class="mint-loadmore-bottom"
          >
            <span
              v-show="bottomStatus !== 'loading'"
              :class="{ 'is-rotate': bottomStatus === 'drop' }"
            >↑</span>
            <span v-show="bottomStatus === 'loading'">
              <mt-spinner
                type="fading-circle"
                color="#294fbc"
              ></mt-spinner>
            </span>
            <span v-show="allLoaded == true">{{$t('message.loadmore.nodata')}}</span>
          </div>
        </mt-loadmore>
      </div>
      <!-- 上拉 下拉 -->
    </div>
  </div>
</template>

<script>
import { publicGetAxios, getCustomCondition } from '@/assets/js/util/common.js';
import lodash from 'lodash';
export default {
  name: "groupSlideList",
  data() {
    return {
      listType: "flow",
      formName: "",
      listata: "",
      allLoaded: false,
      bottomStatus: "",
      wrapperHeight: 0,
      topStatus: "",
      translate: 0,
      moveTranslate: 0,
      sidePanelVisible: false,
      onloadFun: null,
      loadLink: "",
      pageNum: {
        "current": 1,
        "pageSize": 20
      },
      isSingleCheck: 0,
      totalPages: "",
      curChooseList: []
    };
  },
  props: ["propBind"],
  mounted() {
    // let type = this.$route.query && this.$route.query.type;
    let name = this.$route.query && this.$route.query.name;
    let temp = this.propBind;
    this.loadLink = temp && temp.listUrl ? temp.listUrl : "";
    this.formName = name;
    this.pageNum["current"] = 1;
    // this.curChooseList = temp && temp.curChooseList ? temp.curChooseList: [];
    this.curChooseList = this.$store.state.curChooseList;
    this.isSingleCheck =
      temp && (temp.isSingleCheck || temp.isSingleCheck == 0)
        ? temp.isSingleCheck
        : "";
    this.getLoadData = lodash.debounce(this.getLoadData, 500);
    if (this.loadLink) {
      this.getLoadData({ url: this.loadLink, isRefresh: true });
    }
  },
  methods: {
    /**
     * 请求数据
     * url {string} 请求地址
     * isChecked {string} 是否全选
     * isRefresh {bool} 是否刷新数据
     * isDownLoad {bool} 是否是下拉
     */
    getLoadData: function (params) {
      let dataItem = this.pageNum;
      let isCheckedFlag = false;
      this.loadLink = params.url;
      const refCondition = getCustomCondition(true);
      if (params.isChecked == "1" || params.isChecked == "0") {
        // dataItem = {};
        isCheckedFlag = true;
      }
      try {
        publicGetAxios(
          {
            url: params.url,
            type: "get",
            data: { ...dataItem, ...refCondition }
          },
          (res = {}) => {
            //表单数据
            const { list, pagination = {} } = res;
            const { total = 20 } = pagination;
            if (list) {
              this.totalPages = Math.ceil(total / 20);;
              // this.allLoaded = true;
              let rs = list;
              // let temp = []
              for (let i = 0; i < rs.length; i++) {
                const element = rs[i];
                if (params.isChecked == "1") {
                  // 全选
                  element.isChecked = true;
                  this.curChooseList.push(element);
                } else if (params.isChecked == "0") {
                  //全不选
                  element.isChecked = false;
                  this.curChooseList = this.filterArr(this.curChooseList, [
                    element
                  ]);
                  // this.curChooseList.push(element);
                }
                // temp.push(element);
              }

              // 全选 全不选时取全部数据后重新分割
              if (isCheckedFlag) {
                rs = rs.splice(0, 20);

              }

              if (params.isDownLoad) {
                // 下拉
                rs = this.listata.concat(rs);
              }

              rs = this.redrawSelectData(this.curChooseList, rs);
              if (params.isRefresh) {
                // 刷新数据
                this.listata = rs;
              }
            }
            // this.allLoaded = false;
            this.updateNum();
            this.updatePanel();
            this.$nextTick(function () {
              this.$refs.wrapper.addEventListener("touchstart", function (
                event
              ) {
                event.target.classList.add("needsclick");
              });
            });
          }
        );
      } catch (error) {
        this.updatePanel();
      }
    },

    // 显示主岗
    showMainPosition: function (list, type) {
      const { positionFullPath, departmentFullPath } = list;
      const mainPosi = positionFullPath.filter((d) => d.mainPosition);
      const positionName = (mainPosi[0] && mainPosi[0].fullPath)
        || positionFullPath[0].fullPath
        || '';
      const maiDeprt = departmentFullPath.filter((d) => d.currentPosition);
      const departmentName = maiDeprt[0] ? maiDeprt[0].fullPath
        || departmentFullPath[0].fullPath
        || '' : "";
      if (type === 'position')
        return positionName.length > 10 ? positionName.substring(0, 9) + '...' : positionName;
      if (type === 'department')
        return departmentName;
    },

    // 与当前数据比较
    updatePanel: function () {
      this.$nextTick(function () {
        //渲染完成后执行
        try {
          let temp = this.propBind;
          let footerH = temp && temp.footerH ? temp.footerH : 0;
          this.wrapperHeight =
            document.documentElement.clientHeight -
            this.$refs.wrapper.getBoundingClientRect().top -
            footerH -
            10;
          if (this.onloadFun) eval(this.onloadFun);
        } catch (error) { }
      });
    },
    handleBottomChange: function (status) {
      this.bottomStatus = status;
    },
    //下拉、上拉加载逻辑
    loadTop: function () {
      setTimeout(() => {
        //这里执行刷新列表逻辑
        let type = this.listType;
        var url = this.loadLink;
        this.pageNum["current"] = 1;
        this.pageNum["pageSize"] = 20;
        this.getLoadData({ url: url, isRefresh: true });
        this.$refs.loadmore.onTopLoaded();
      }, 1500);
    },
    handleTopChange: function (status) {
      this.moveTranslate = 1;
      this.topStatus = status;
    },
    translateChange: function (translate) {
      const translateNum = +translate;
      this.translate = translateNum.toFixed(2);
      this.moveTranslate = (1 + translateNum / 70).toFixed(2);
    },
    loadBottom: function () {
      setTimeout(() => {
        //这里执行加载更多逻辑
        let type = this.listType;
        var url = this.loadLink;
        this.pageNum["pageSize"] = 20;
        if (this.pageNum["current"] == this.totalPages) {
          this.$refs.loadmore.onBottomLoaded();
          return;
        } else if (this.pageNum["current"] < this.totalPages) {
          this.pageNum["current"]++;
        }

        this.getLoadData({ url: url, isRefresh: true, isDownLoad: true });
        this.$refs.loadmore.onBottomLoaded();
      }, 1500);
    },
    // 选择人数
    checkPeopleNum: function (index) {
      var k = this.listata[index];
      if (this.isSingleCheck == "0") {
        // 多选
        this.multiSelect(index);
      } else {
        // 单选
        this.singleSelect(k);
      }
    },
    // 多选
    multiSelect: function (index) {
      let k = this.listata[index];
      if (k.isChecked) {
        k.isChecked = false;
        let temp = this.curChooseList;
        for (let i = 0; i < temp.length; i++) {
          const element = temp[i];
          if (element.id == k.id) {
            temp.splice(i, 1);
          }
        }
      } else {
        k.isChecked = true;
        this.curChooseList.push(k);
      }
      this.updateNum();
      this.$set(this.listata, index, k); //更新数据
    },
    // 单选
    singleSelect: function (obj) {
      this.curChooseList = obj;
      // this.$store.commit("updateData",{name: 'curChooseList', value: this.curChooseList});
      this.$emit("updateChoooseList", this.curChooseList); // 更新选中列表
    },
    // 更新人数
    updateNum: function () {
      // 数组去重
      this.curChooseList = this.uniqArr(this.curChooseList);
      // this.curChooseList = this.$store.state.curChooseList;
      this.$store.commit("updateData", {
        name: "curChooseList",
        value: this.curChooseList
      });
    }
  }
};
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less" scoped>
@import "../../assets/css/memberListMoudule.less";
</style>


// WEBPACK FOOTER //
// src/components/refer/groupSlideList.vue