
<template>
  <div class="html-link">
    <!-- <div class="html-content" v-html="content"></div> -->
    <!-- <div v-html="html"></div> -->
    <iframe id="newPage" ref="iframe" :src="url" frameborder="0" scrolling="no" marginwidth="0" marginheight="0" vspace="0" hspace="0" allowtransparency="true" allowfullscreen="true"></iframe>
  </div>
</template>
<script type="text/babel">
export default {
  name: "htmlPanel",
  data() {
    return {
      content: "",
      url:"",
      html: ""
    };
  },
  props: ["params"],
  mounted() {
    const $this = this;
    $this.url = $this.params.url;
    // $this.load($this.url);
    // $this.getData({
    //   url: $this.params.url
    // },function(res){
    //   console.log(res)
    //   $this.html = res;
    // })
  },
  methods: {
    load (url) {
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
            this.html = '加载失败'
          })
        }
      }
  }
};
</script>
<style scoped>
</style>



// WEBPACK FOOTER //
// src/components/htmlPanel.vue