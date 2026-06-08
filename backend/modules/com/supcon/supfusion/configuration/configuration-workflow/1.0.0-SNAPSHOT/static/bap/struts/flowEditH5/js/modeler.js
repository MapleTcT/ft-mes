//初始化流程图绘制脚本
define(function(require, exports, module) {
  var BpmnJS = require("./diagram/bpmn-modeler.development.js"); //bpmn脚本库
  var bpmnPublic = require("./diagram/bpmnPublic.js"); //基本方法
  var xmlConvert = require("./diagram/xmlConvert.js"); //流程图数据转换
  var diagramTool = require("./diagram/diagramTool.js"); //流程图工具栏
  var paramTransform = require("./diagram/paramTransform.js"); //参数转换
  // 建模实例
  var bpmnModeler = new BpmnJS({
    container: "#canvas",
    keyboard: {
      bindTo: window
    }
  });
  //初始化工具栏
  new diagramTool($("#menuTool"), bpmnModeler, xmlConvert);
  $.ajax({
    url: "/msService/ec/workflow/getFlow",
    type: "post",
    dataType: "xml",
    data: { deploymentId: $("#deploymentId").val() },
    success: function(xml) {
      if (xml) {
        closeLoadPanel(); //关闭加载中
        var newXml = xmlConvert.xmlConvertRorBpmn.dealXMLData(xml);
        openDiagram(newXml);
      } else {
        getDefaultXml(); //新建的组态为空的流程默认加载数据
      }
    },
    error: function(e) {
      console.log("错误", "/msService/ec/workflow/getFlow报错");
    }
  });
  //清空控制台
  $("#consoleClear").on("click", function() {
    $("#consoleTab").empty();
  });
  //右侧菜单展开收起
  $(".djs-arrow").on("click", function() {
    var $this = $(this);
    $this.siblings(".djs-paletteBox").slideToggle(200);
    $this.find("i").toggleClass("up");
    $this.find("i").toggleClass("dowm");
    var isUp = $this.find("i").hasClass("up");
    if (isUp) {
      $this.attr(
        "title",
        getInternationalValByKey("js.ec.workflow.config.tool.close")
      );
      $(".djs-container,.m_codeSource .m_codeBox,.m_consoleBox").addClass(
        "cut-part"
      );
      $(".djs-palette").addClass("open");
    } else {
      $this.attr(
        "title",
        getInternationalValByKey("js.ec.workflow.config.tool.open")
      );
      $(".djs-container,.m_codeSource .m_codeBox,.m_consoleBox").removeClass(
        "cut-part"
      );
      $(".djs-palette").removeClass("open");
    }
  });
  //新加元素工具栏交互
  $(".djs-palette").on("mouseover", function() {
    $(this).attr("data-state", "hover");
    $(".layer-snap path").css("display", "none"); //隐藏黄色基准线
  });
  $(".djs-palette").on("mouseout", function() {
    $(this).attr("data-state", "");
  });
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
      $("#showScale").html((canvas.zoom() * 100).toFixed(0) + "%"); //显示缩放比例
      bpmnPublic.bindDefaultAttr(); //绑定默认属性
      paramTransform.getExistParam(); //读取已发布或以保存的参数配置信息(权限+选人范围)
      paramTransform.recordExistEle(); //记录初始节点数据
    });
  }
  //新建的组态为空的流程默认加载数据
  function getDefaultXml() {
    var diagramUrl = "/bap/static/flowEditH5/data/workFlow.xml";
    $.ajax({
      url: diagramUrl,
      type: "get",
      dataType: "xml",
      success: function(xml) {
        closeLoadPanel(); //关闭加载中
        var newXml = xmlConvert.xmlConvertRorBpmn.dealXMLData(xml);
        openDiagram(newXml);
      },
      error: function(e) {
        console.log("请求数据报错", e);
      }
    });
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
//---------template外联函数定义----------------------
//获取随机数
template.defaults.imports.getRand = function(num) {
  var rand = "";
  for (var i = 0; i < num; i++) {
    var r = Math.floor(Math.random() * 10);
    rand += r;
  }
  return rand;
};
//是否可以修改发布
template.defaults.imports.getModifyAble = function() {
  //没版本或者为0就必须全新发布
  var version = $("#processVersion").val();
  var isDisable = false;
  if (!version || version == "0") {
    isDisable = true;
  }
  return isDisable;
};
//获取绑定事件
template.defaults.imports.getBindName = function() {
  var bind_name = "oninput";
  if (navigator.userAgent.indexOf("MSIE") != -1) {
    bind_name = "onpropertychange";
  }
  return bind_name;
};
//获取组限制参数
template.defaults.imports.getGroupRestrict = function() {
  var groupRestrict = $("#groupRestrict").val() == "true";
  return groupRestrict;
};

//正整数正则验证
window.testPositiveInteger = function(value, allowZero) {
  var reg = /^[1-9]\d*$/;
  if (allowZero) reg = /^[0-9]\d*$/; //允许为0
  while (!reg.test(value) && value != "") {
    value = value.substr(0, value.length - 1);
  }
  if (allowZero && value !== "") value = Number(value); //允许为0
  return value;
};
