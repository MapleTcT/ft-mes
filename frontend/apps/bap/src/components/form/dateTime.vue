<template>
    <div :class="!params.readonly ? 'pr20':''">
        <iInput :params="{
            condition: params,
            clickEvent: selectDate}">
        </iInput>
        <i v-if="!params.readonly && !params.value" class="more-icon" @click="selectDate(params)"></i>
        <i v-else-if="!params.readonly && params.value" @click="deleteRefValue(params)" class="refer-delete"></i>
        <div class="pickerPop" @touchmove.prevent>
            <!-- 年月日时分选择 -->
            <mt-datetime-picker
                lockScroll="true"
                :ref="params.pickerName"
                v-model="dateVal"
                class="myPicker"
                :endDate="enddate"
                :startDate="startdate"
                :type="params.datetype"
                year-format="{value}年"
                month-format="{value}月"
                date-format="{value}日"
                hour-format="{value}时"
                minute-format="{value}分"
                second-format="{value}秒"
                @confirm="dateConfirm(params)">
            </mt-datetime-picker>
        </div>
    </div>
</template>

<script>
    import _commonJs from "@/assets/js/itemList/index.js"; //引用模块进来
    import iInput from "@/components/form/iInput";
    import AsyncValidator from "async-validator";
    import { Toast, Indicator, Popup } from "mint-ui";
    export default {
        name: "dateTime",
        props: ["params"],
        data() {
            return {
                startdate: "",
                enddate: "",
                dateVal: ""
            };
        },
        beforeMount() {
            const $this = this;

            // 格式起始和结束时间
            _commonJs.startAndEndTime($this, 1970);
        },
        methods: {
            // 打开时间选择器
            selectDate: function(item) {
                if(item.readonly){
                    return false;
                }
                // 如果已经选过日期，则再次打开时间选择器时，日期回显（不需要回显的话可以去掉 这个判断）
                if (item.value) {
                    if (item.showFormat == "YMD_H") {
                        this.dateVal = new Date(item.value.replace(/-/g,"/") + ":00");
                    } else {
                        this.dateVal = new Date(item.value.replace(/-/g,"/"));
                    }
                } else {
                    this.dateVal = new Date();
                }
                this.$refs[item.pickerName].open();
                
                //根据设置的日期和日期时间的显示格式显示
                var dateClass = this.$refs[item.pickerName].$el.getElementsByClassName("picker-slot");
                for (var i = 0; i < dateClass.length; i++) {
                    dateClass[i].style.display = "block";
                }
                if (item.datetype == "date") { //日期类型
                    if (item.showFormat == "YM") {
                        dateClass[2].style.display = "none";
                    } else if (item.showFormat == "Y") {
                        dateClass[1].style.display = "none";
                        dateClass[2].style.display = "none";
                    }
                } else if (item.datetype == "datetime") { //日期时间类型
                    if (item.showFormat == "YMD_H") {
                        dateClass[4].style.display = "none";
                    }else if (item.showFormat == "YMD") {
                        dateClass[3].style.display = "none";
                        dateClass[4].style.display = "none";
                    } else if (item.showFormat == "YM") {
                        dateClass[2].style.display = "none";
                        dateClass[3].style.display = "none";
                        dateClass[4].style.display = "none";
                    } else if (item.showFormat == "Y") {
                        dateClass[1].style.display = "none";
                        dateClass[2].style.display = "none";
                        dateClass[3].style.display = "none";
                        dateClass[4].style.display = "none";
                    }
                }
            },

            //确认选择时间
            dateConfirm: function(item) {
                // 限制时间范围
                const pickname = item.pickerName;
                const value = this.$refs[pickname].value;
                // 时间选择器确定按钮，并把时间转换成我们需要的时间格式
                if (typeof this.dateVal == "object") {
                    item.value = _commonJs.getDateFormat(item.showFormat, this.dateVal);       
                } else {
                    item.value = this.dateVal;
                }
            },

            //删除参照的值
            deleteRefValue: function(data) {
                let inputRef =  this.params.name;            
                if (typeof data.value == "string") {
                    data.valueId = "";
                    data.value = "";
                    document.getElementsByName(inputRef).value = "";
                }
                this.$forceUpdate();
            },

        },
            
        //引用组件
        components: { iInput }
    };
</script>
<!-- Add "scoped" attribute to limit CSS to this component only -->

<style lang="less" scoped>
@import "../../assets/css/editView.less";
</style>



// WEBPACK FOOTER //
// src/components/form/dateTime.vue