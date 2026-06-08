// 全局函数注册
import { Toast, Indicator } from 'mint-ui';
import _commonJs from '@/assets/js/itemList/index.js';

export default {
    install(Vue, options) {
        //请求数据
        Vue.prototype.getData = function(param, callback) {
            let config;
            // loading加载
            if (!(param.loadType && param.loadType == "dropDown")) {
                Indicator.open({
                    text: '加载中...',
                    //文字
                    spinnerType: 'fading-circle',
                    //样式
                });
            }

            if (param.type && param.type.toLocaleLowerCase() == "post") { // post 请求
                config = {
                    method: "post",
                    url: param.url,
                    data: param.data,
                    transformRequest: [function(e) {
                        // 数据转换的核心代码 json 转换为字符串
                        function setDate(e) {
                            var t, n, i, r, o, s, a, c = "";
                            for (t in e)
                                if (n = e[t], n instanceof Array)
                                    for (a = 0; a < n.length; ++a)
                                        o = n[a], i = t + "[" + a + "]", s = {}, s[i] = o, c += setDate(s) + "&";
                                else if (n instanceof Object)
                                for (r in n) o = n[r], i = t + "[" + r + "]", s = {}, s[i] = o, c += setDate(s) + "&";
                            else void 0 !== n && null !== n && (c += encodeURIComponent(t) + "=" + encodeURIComponent(n) + "&");
                            return c.length ? c.substr(0, c.length - 1) : c
                        }
                        return setDate(e);
                    }]
                }
            } else { // get 请求
                config = {
                    method: "get",
                    url: param.url,
                    params: param.data
                }
            }

            this.$axios(config).then((result) => {
                    // console.log(result);
                    if (result.status == 200 && (result.data || result.data === 0)) {
                        if (typeof callback == "function") {
                            callback(result.data);
                            var modal = document.getElementsByClassName("edit_modal");
                            if (modal[0].style.display == "none") {
                                Indicator.close();
                            } else {
                                setTimeout(() => {
                                    modal[0].style.display = "none";
                                    Indicator.close();
                                }, 1000);
                            }
                        }
                    }
                })
                .catch(function(error) {
                    console.log(error);
                    Indicator.close();
                });
        }

        // 筛选数组
        Vue.prototype.filterArr = function(arr1, arr2) {
            if (arr1 && arr2) {
                let tempArr = [];
                for (let i = 0; i < arr1.length; i++) {
                    const element = arr1[i];
                    let flag = false;
                    for (let j = 0; j < arr2.length; j++) {
                        const k = arr2[j];
                        if (element.id == k.id) {
                            flag = true;
                        }
                    }
                    if (!flag) {
                        tempArr.push(element);
                    }

                }
                return tempArr;
            }
        }

        // 数组去重
        Vue.prototype.uniqArr = function(array) {
            var temp = [];
            // var index = [];
            var l = array.length;
            for (var i = 0; i < l; i++) {
                for (var j = i + 1; j < l; j++) {
                    if (array[i].id == array[j].id) {
                        i++;
                        j = i;
                    }
                }
                temp.push(array[i]);
                // index.push(i);
            }
            return temp;
        }

        // 加载数据时对比存储数据
        Vue.prototype.redrawSelectData = function(arr1, arr2) {
            let $this = this;
            if (arr1 && arr2) {
                for (let i = 0; i < arr1.length; i++) {
                    const el = arr1[i];
                    for (let j = 0; j < arr2.length; j++) {
                        const k = arr2[j];
                        if (el.id == k.id) { //同一元素 选中
                            k.isChecked = true;
                        }
                    }
                }
                return arr2;
            }
        }

        /**选择人员与部门之间的勾选状态
         * arr1 人员数组
         * arr2 部门数组
         **/
        Vue.prototype.depPeoSelectData = function(arr1, arr2) {
            let $this = this;
            if (arr1 && arr2) {
                for (let i = 0; i < arr2.length; i++) {
                    const department = arr2[i];
                    const peopleNum = Number(department.peopleNum);
                    var num = 0;
                    var departmentNum = [];
                    for (let j = 0; j < arr1.length; j++) {
                        const peopleLength = arr1.length;
                        const people = arr1[j];
                        if (peopleNum <= peopleLength && peopleNum !== 0 && people.mainPosition) {
                            const peopleDepart = people.mainPosition.department;
                            if (department.id == peopleDepart.id) {
                                departmentNum.push(people);
                                num++;
                            }
                            if (peopleNum == num && departmentNum.length == peopleNum) {
                                department.isChecked = true;
                                break;
                            }
                        }
                    }
                }
                return arr2;
            }
        }

        // 提取参数
        Vue.prototype.GetRequest = function(url) {
            var url = decodeURI(url); //获取url中"?"符后的字串  
            var theRequest = new Object();
            if (url.indexOf("?") != -1) {
                var str = url.split("?");
                let strs = str[1].split("&");
                for (var i = 0; i < strs.length; i++) {
                    strs[i] = strs[i].split('#')[0];
                    theRequest[strs[i].split("=")[0]] = unescape(strs[i].split("=")[1]);
                }
                return theRequest;
            } else {
                return '';
            }

        }

        // 提示信息
        Vue.prototype.myToast = function(msg) {
            let mes = msg ? msg : "成功";
            let instance = Toast(mes);
            setTimeout(() => {
                instance.close();
            }, 1500);
        }

        // 注册全局方法
        Vue.prototype.exportFunc = function(funcName, func) {
            if (funcName && func && typeof func == "function") {
                Vue.prototype[funcName] = func;
            }
        }
    }
};


// WEBPACK FOOTER //
// ./src/assets/js/util/util.js