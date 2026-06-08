<template>
<div>
    <!--图片-->    
    <template v-if="isPicture">
        <div class="m_attachment">
            <viewer :images="imgList" class="ma_item pickbox" v-for="(item, index) in imgList" :key="index">
                <img class="selimg" :src="item" alt="">
                <a v-if="!params.con.readonly" class="i_cancel" @click="deleteImg(index)"><i>×</i></a>
            </viewer>
            <div class="ma_add" v-if="!params.con.readonly">
                <input type="file" accept="image/*" :name="params.name" @change="getPickResult" mutiple>
                <a class="btn_add"></a>
            </div>
        </div> 
    </template>
    <!--附件-->    
    <template v-if="isAppendix">
        <div class="m_wrap">
            <span class="ml_text">{{params.label?params.label:'图片'}}</span>
            <div class="m_attachment">
                <div class="ma_fileItem pickbox" v-for="(item, index) in fileList" :key="index">
                    <img class="file_image fl" src="@/assets/images/file_image.png">
                    <div class="file_box"><span class="fb_name">{{item.name}}</span><i class="fb_size">{{item.size}}</i></div>
                    <a class="i_cancel fr" @click="deleteFile(index)"><i>×</i></a>
                </div>
                <div class="ma_add">
                    <input type="file" @change="getPickFileResult">
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
    name: "selectAppendix",
    data: function() {
        return {
            isPicture: false, //是否为图片
            isAppendix: false, //是否为附件
            imgList: [], //图片列表
            fileList:[], //附件列表
            postUrl: "", //需要上传到的地址
            imageUrl: "", //img绑定的src地址
            pictureFile:{}
        };
    },
    props: ['params', "imgValue", "con"],
    mounted() {
        let $this = this;
        if($this.params){
            $this.isPicture = $this.params.isPicture;
            $this.isAppendix = $this.params.isAppendix;
            $this.imgList = $this.imgValue?[$this.imgValue]:[];
            $this.fileList = $this.params.fileList;
            $this.postUrl = $this.params.postUrl;
        }
        
    },
    methods: {
        //选择图片
        getPickResult: function(event) {
            var obj = event.target;
            var reads = new FileReader();
            var f = obj.files[0];
            var $this = this;
            reads.readAsDataURL(f);

            // TODO 限制文件大小
            // let fileSize = f.size / 1024 /1024; // 单位M
            // if(fileSize > 2){
            //     return false;
            // }

            let formData = new FormData();
            formData.append("files", f);

            reads.onload = function(e) {
                $this.imgList = [this.result];
            };
            
            
            $this.upLoadFile(formData, $this.postUrl, function(res){
                // 上传图片
                let upLoadUrl = res.data;
                // $this.pictureFile[`${obj.name}.filePath`] = ?upLoadUrl:"";
                obj.value=null;
                // this.$emit('addPicture', url);
                $this.$emit('updatePicture', event, upLoadUrl, $this.params.con, 'upload');

            });
            
        },
        //删除图片
        deleteImg: function(index) {
            var data = this.imgList;
            data.splice(index, 1);
            this.$emit('updatePicture', event, '', this.params.con, 'delete');
        },
        //选择附件
        getPickFileResult: function(event) {
            var obj=event.target;
            var reads = new FileReader();
            var f = obj.files[0];
            var type = f.name.slice(f.name.lastIndexOf(".") + 1).toLowerCase();
            this.fileList.push({"name":f.name,"size": (f.size / 1000).toFixed(0) +"kb"}); 
            obj.value=null;//清空选择
            this.$emit('updateAppendix', this.fileList);
        },
        //删除附件
            deleteFile: function(index){
            var data = this.fileList;
            data.splice(index, 1);
            this.$emit('updateAppendix', this.fileList);
        },

        // 上传
        async upLoadFile(formData, url, callback){
            const $this = this;
            let res = await $this.$axios
            .post(url, formData, {
                headers: {
                    "Content-Type": "multipart/form-data" //hearder 很重要，Content-Type 要写对
                }
            });
            
            if(callback && typeof callback == "function"){
                callback(res);
            }

        }
    }
};
</script>

<!-- 样式加载 -->
<style lang="less" scoped>
@import "../assets/css/selectAppendix.less";
.m_box .m_wrap{
    padding: 0;
}
</style>
<style lang="css">
    .viewer-footer {
        display: none;
    }
</style>




// WEBPACK FOOTER //
// src/components/selectAppendix.vue