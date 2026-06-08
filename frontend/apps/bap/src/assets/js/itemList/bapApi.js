var bapApi = {};
//根据name属性设置样式
bapApi.setStyle = function(name, config) {
        var ele = document.querySelectorAll('[name="' + name + '"]');
        for (var i = 0; i < ele.length; i++) {
            var item = ele[i];
            for (var key in config) {
                item.style[key] = config[key];
            }
        }
    }
    //通过index定位元素设置样式
bapApi.setStyleByIndex = function(index, name, config) {
        var ele = document.querySelectorAll('[name="' + name + '"]');
        var target = ele[index];
        if (target) {
            for (var key in config) {
                target.style[key] = config[key];
            }
        }
    }
    //通过index定位元素改值
bapApi.setValueByIndex = function(index, value, name) {
        var ele = document.querySelectorAll('[name="' + name + '"]');
        var target = ele[index];
        if (target) target.innerHTML = value;
    }
    //绑定自定义事件
bapApi.bindEventConfig = function(ev, tag) {
    for (var i = 0; i < ev.length; i++) {
        var event = ev[i];
        var name = event.code;
        tag = tag ? `.${tag}` : '';
        var ele = document.querySelectorAll(`${tag}[name="${name}"]`);
        for (var j = 0; j < ele.length; j++) {
            var obj = ele[j];
            var eventName = event.ename.replace("on", '');
            var func = eval("(" + event.fbody + ")"); // 使js函数字符串生效

            obj.removeEventListener(eventName, func);
            if (obj.getAttribute(`${eventName}_addClickHandleFlag`) !== "1") {
                obj.setAttribute(`${eventName}_addClickHandleFlag`, "1")
                obj.addEventListener(eventName, func); // 监听事件
            }


        }
    }
}
export default bapApi;


// WEBPACK FOOTER //
// ./src/assets/js/itemList/bapApi.js