<template>
  <div class="html-link">
    <!-- <div class="html-content" v-html="content"></div> -->
    <!-- <div v-html="html"></div> -->

    <!--附件预览的头部-->
    <header
      v-if="isAppendixPreview"
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
        <h2 class="m_title">{{$t('cellComp.htmlPanel.attach.preview')}}</h2>
        <span
          v-on:click="downloadFile($event)"
          class="head-icon-download"
        ></span>
      </div>
    </header>

    <iframe
      id="newPage"
      :class="{'appendix-preview': isAppendixPreview}"
      ref="iframe"
      :src="url"
      frameborder="0"
      :scrolling="isAppendixPreview ? 'auto' : 'no'"
      marginwidth="0"
      marginheight="0"
      vspace="0"
      hspace="0"
      allowtransparency="true"
      allowfullscreen="true"
    ></iframe>
  </div>
</template>

<script type="text/babel">
import { Indicator } from "mint-ui";
import commonJS from '@/assets/js/itemList/index.js'

export default {
  name: "htmlPanel",
  data() {
    return {
      content: "",
      url: "",
      html: "",
      isAppendixPreview: false,
      bgColor: "",
    };
  },
  props: ["params"],
  mounted() {
    const $this = this;
    $this.url = $this.params.url;

    $this.isAppendixPreview = $this.params.isAppendix;

    if (window.pageConfig) {
      $this.bgColor = window.pageConfig.bgColor ? window.pageConfig.bgColor : ''; // 背景色
    }

    // 附件预览-加载中会遮挡头部
    if (this.isAppendixPreview) {
      document.querySelector('.mint-indicator-mask').style.top = '1.33333333rem';
    }

    // $this.load($this.url);
    // $this.getData({
    //   url: $this.params.url
    // },function(res){
    //   console.log(res)
    //   $this.html = res;
    // })
  },
  beforeDestroy() {
    const { $refs: refs = {} } = this;
    const { iframe } = refs;
    if (iframe) {
      iframe.src = '';
      iframe.parentNode.removeChild(iframe);
    }
  },
  methods: {
    load(url) {
      if (url && url.length > 0) {
        // 加载中
        this.loading = true
        let param = {
          accept: 'text/html, text/plain'
        }
        this.$axios.get(url, param).then((response) => {
          this.loading = false
          // 处理HTML显示
          this.html = response.data
        }).catch(() => {
          this.loading = false
          this.html = this.$t('cellComp.htmlPanel.attach.loadfail')
        })
      }
    },

    // 返回上一个页面
    backHistory: function () {
      setTimeout(function () {
        Indicator.close();
        vue.updateAppendix(false);
      }, 200);
    },

    // 下载附件
    downloadFile: function (e) {
      commonJS.downloadAppendixs(this.params, e);
    }
  }
};
</script>

<style lang="less" scoped>
@import "./htmlPanel.less";
</style>


// WEBPACK FOOTER //
// src/components/cellComp/htmlPanel/index.vue