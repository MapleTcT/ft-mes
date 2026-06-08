//初始化流程图绘制脚本
define(function(require, exports, module) {
  var BpmnJS = require("./diagram/bpmn-modeler.developmentForView.js"); //bpmn框架
  var bpmnPublic = require("./diagram/bpmnPublic.js"); //基本方法
  var xmlConvert = require("./diagram/xmlConvert.js"); //流程图数据转换
  var bindDiagramInfo = require("./diagram/bindDiagramInfo.js"); //绑定流程图信息
  // // 建模实例
  var bpmnModeler = new BpmnJS({
    container: "#canvas",
    keyboard: {
      bindTo: window
    }
  });
  //绑定流程图信息
  var version = $("#processVersion", parent.document).val();
  var processName = $("#processName", parent.document).val();
  var processKey = $("#processKey", parent.document).val();
  if (processKey && processKey != "") processKey = "(" + processKey + ")"; //如果编码为空则不显示
  if (!processName || processName == "") processName = getXMLName(); //如果名称为空，则从xml数据中读取
  var info = { name: processName + processKey, version: version };
  $("#toolBox").html(template("toolTemp", info));
  layui.use(["form"], function() {
    var form = layui.form;
    //初始赋值
    form.val("tool", {
      hideDescribe: "",
      hideReject: ""
    });
    form.render(); //渲染
    //流程数据转化
    var flowXML = bpmnPublic.string2XML($("#flowXML", parent.document).val());
    var newXml = xmlConvert.xmlConvertRorBpmn.dealXMLData(flowXML);
    openDiagram(newXml);
    resetFlowName();
  });
  //重置视图自适应层级
  $("#resetFitView").on("click", function() {
    // 缩放以适合容器显示
    var canvas = bpmnModeler.get("canvas");
    canvas.resized();
    canvas.zoom("fit-viewport", true);
    //同步视图缩放滚动条
    var config = window.viewSlider.config;
    config.value = (canvas.zoom() * 100).toFixed(0);
    window.slider.render(config);
  });
  // 工作流查看title名过长工具栏显示不全
  function resetFlowName() {
    var curW = $("#flowName").width();
    var toolW = $("#toolBox").width();
    var fullWidth = toolW - getSibNodeWidth(); // titlt允许完整展示的最大宽度
    if (curW > fullWidth) {
      var $name = $("#flowName");
      $name.css({
        width: fullWidth < 30 ? 30 : fullWidth + "px",
        overflow: "hidden",
        textOverflow: "ellipsis",
        display: "inline-block",
        float: "right"
      });
      $name.attr("title", $name.text());
    }
  }

  // 获取兄弟节点的占位宽度
  function getSibNodeWidth() {
    var sibNode = $("#flowName")
      .closest(".m_tool_item")
      .siblings();
    var titNode = $("#flowName").siblings();
    var sibWidth = 0;
    $.each(titNode, function(index, item) {
      var oWidth = $(item).outerWidth(true);
      sibWidth += oWidth;
    });
    $.each(sibNode, function(index, item) {
      var oWidth = $(item).outerWidth(true) + 5;
      sibWidth += oWidth;
    });
    return sibWidth;
  }

  // -------------------------------函数定义-----------------------------------------
  /**
   * 打开流程图实例
   *
   * @param {String} 要显示的流程图数据格式
   */
  function openDiagram(bpmnXML) {
    // 导入流程图
    bpmnModeler.importXML(bpmnXML, function(err) {
      if (err) {
        return console.error("could not import BPMN 2.0 diagram", err);
      }
      insertBpmnLogo(); //生成bpmn官网标志
      // 访问建模组件
      var canvas = bpmnModeler.get("canvas");
      var overlays = bpmnModeler.get("overlays");
      // 缩放以适合容器显示
      canvas.zoom("fit-viewport", true);
      var zoomScale = (canvas.zoom() * 100).toFixed(0); //缩放比例
      if (!zoomScale || zoomScale == "NaN") {
        zoomScale = 50;
        canvas.zoom(0.5);
      }
      //初始化流程展示
      var initModeler = new bindDiagramInfo(bpmnModeler);
      initModeler.bindTool(zoomScale); //绑定业务逻辑
    });
  }
  //从xml中读取流程名称
  function getXMLName() {
    var xmlName = "";
    var xml = bpmnPublic.string2XML($("#flowXML", parent.document).val());
    var xmlChild = xml.childNodes;
    var xmlObj = xmlConvert.XmlToJson.parse(xmlChild[0]);
    if (xmlObj.items && xmlObj.items[0]) xmlName = xmlObj.items[0].text;
    return xmlName;
  }
  // 生成bpmn官网标志
  function insertBpmnLogo() {
    var logoWrap = document.createElement("div");
    logoWrap.className = "bpmn-logo-wrap";
    var domHtml = '<i class="bpmn-logo"></i>';
    domHtml += '<div class="logo-pop-layer">';
    domHtml += '<div class="layer-mask"></div>';
    domHtml += '<div class="bpmn-info">';
    domHtml += '<i class="bpmn-logo"></i>';
    domHtml +=
      '<span class="bpmn-text">由<a href="https://bpmn.io/" target="_blank">bpmn.io</a>支持的BPMN，DMN和CMMN图的基于Web的工具</span>';
    domHtml += "</div></div>";
    logoWrap.innerHTML = domHtml;
    document.querySelector(".djs-container").appendChild(logoWrap);
    var bpmnLayer = $(".bpmn-logo-wrap .logo-pop-layer");
    $(".bpmn-logo-wrap .bpmn-logo").on("click", function() {
      bpmnLayer.show();
    });
    $(".bpmn-logo-wrap .layer-mask").on("click", function() {
      bpmnLayer.hide();
    });
  }
});
