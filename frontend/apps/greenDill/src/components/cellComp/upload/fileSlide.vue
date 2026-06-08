<template>
  <div class="file-slide-bg">
    <div
      :style="{left:slideLeft}"
      class="file-moveSlide"
      v-on:touchstart="moveSlideStart($event)"
      v-on:touchmove="moveSlideMove($event)"
      v-on:touchend="moveSlideEnd($event)"
    >
      <slot></slot>
    </div>
    <template v-for="(btn,i) in button">
      <div
        v-on:click="($event)=>{$event.stopPropagation();btn.event(btn.fileName)}"
        class="btn-list"
      >
        <div
          :style="{width: `${btn.width / 75}rem`}"
          :class="`delete-icon ${btn.type}`"
        >{{btn.txt}}</div>
      </div>
    </template>

  </div>
</template>

<script>
import Vue from 'vue';
import _commonJs from "@/assets/js/itemList/index.js"; //引用模块进来

export default {
  name: "fileSlide",
  data: function () {
    return {
      start: {
        x: 0, y: 0
      },
      swiping: false,
      slideLeft: 0
    };
  },
  props: {
    button: {
      type: Array,
    },
  },
  mounted() {
  },
  methods: {

    moveSlideStart: function (e) {
      this.drag = true;
      const evt = e.changedTouches ? e.changedTouches[0] : e;
      this.start.x = evt.pageX;
      this.start.y = evt.pageY;
    },

    moveSlideMove: function (e) {
      e.stopPropagation();
      let swiping;
      if (!this.drag || this.button.length === 0) return false;
      const evt = e.changedTouches ? e.changedTouches[0] : e;
      const offsetTop = evt.pageY - this.start.y;
      const offsetLeft = evt.pageX - this.start.x;
      const y = Math.abs(offsetTop);
      const x = Math.abs(offsetLeft);
      swiping = !(x < 5 || (x >= 5 && y >= x * 1.73));
      if (!swiping) return;
      e.preventDefault();
      let mouseX = offsetLeft;
      mouseX = mouseX / 75;
      if (mouseX > 0) {
        this.slideLeft = '0px';
        this.swiping = false;
        return;
      } else {
        this.slideLeft = `${mouseX}rem`;
      }

      this.offsetLeft = offsetLeft;
      this.swiping = true;
    },

    moveSlideEnd: function (e) {
      e.stopPropagation();
      if (!this.swiping) return;
      this.drag = false;
      const { button } = this;
      let curPos = 0;
      let btnWidth = 0;
      if (button.length === 0) return false;
      button.forEach((btn) => {
        if (btn.width) {
          btnWidth += Number(btn.width);
        }
      });
      const rem = btnWidth / 75;
      // 超过就显示全部
      if (this.offsetLeft < 0) {
        curPos = `-${rem}`
      }
      this.slideLeft = `${curPos}rem`;
    }
  }
};
</script>

<!-- 样式加载 -->
<style lang="less" scoped>
@import "./index.less";
</style>




// WEBPACK FOOTER //
// src/components/cellComp/upload/fileSlide.vue