window.importCommonJs || loadScript("/msService/WTS/commonJs/loader");
importCommonJs("wtsCommon");
function onload() {
  //iframe打开
  if (ReactAPI.getParamsInRequestUrl().openType == "iframe") {
    //  头部隐藏
    $('#app > div > form > div.m-layout-mian > div > div > div:nth-child(1) > div').hide();
    $('.Nfi2S-uX').hide();
  } else {
    ReactAPI.setHeadBtnAttr("winClose", { icon: "windowClose" });
  }
}