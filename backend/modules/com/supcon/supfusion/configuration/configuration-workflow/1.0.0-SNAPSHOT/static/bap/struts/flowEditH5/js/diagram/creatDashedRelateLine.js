/**
 * 创建连接关系的虚线
 */
define(function (require, exports, module) {
	 var diConfig=require("./diagramConfig.js");//配置信息
	 var shapeSize=diConfig.shape;//实例大小
    //迁移线上的文字拖拽时中间用虚线连接表示连接关系
    function creatDashedLine() {
    }
    creatDashedLine.prototype.createLabel = function (shapes,position) {
    	var linePoint=getBreakLineCenter(shapes[0].labelTarget.waypoints);
		var lineAttr={
				id:"lineRelation",
				x1:position.x,
				y1:position.y,
				x2:linePoint.x,
				y2:linePoint.y,
				stroke:"#333",
				strokeWidth:2,
				strokeDasharray:4
		};
		return lineAttr;
    };
    creatDashedLine.prototype.createShape = function (event) {
    	var startPoint={x:event.previousSelection[0].x+Number(shapeSize.width) / 2,y:event.previousSelection[0].y+Number(shapeSize.width) / 2};
    	var endPoint={x:event.x,y:event.y};
    	var startPointCut=getRadiusCut(startPoint,endPoint);
    	var endPointCut=getRadiusCut(endPoint,startPoint);
    	var lineAttr={
				id:"lineRelation",
				x1:startPointCut.x,
				y1:startPointCut.y,
				x2:endPointCut.x,
				y2:endPointCut.y,
				stroke:"#333",
				strokeWidth:2,
				strokeDasharray:4
		};
		return lineAttr;
    };
    /*
     * 坐标偏移计算;获取箭头指向元素坐标(保证箭头不被图形覆盖，需减去半径)
     * @param {transPos}   转化点
     * @param {referPos}   参考点
     */
    function getRadiusCut(transPos,referPos){
        var endX = Number(transPos.x);
        var endY = Number(transPos.y);
        var prevX = Number(referPos.x);
        var prevY = Number(referPos.y);
        var radius = Number(shapeSize.width) / 2;
        var getX, getY;
        var count = (prevX - endX) * (prevX - endX) + (prevY - endY) * (prevY - endY)
        getX = (prevX - endX) / Math.sqrt(count) * radius;
        getY = (prevY - endY) / Math.sqrt(count) * radius;
        var result = { x: (endX + getX).toFixed(2), y: (endY + getY).toFixed(2) };
        return result;
    }
    //获取折线或者直线中点
	function getBreakLineCenter(po){
		var len = 0;
	    for (var i = 0; i < po.length; i++) {
	        if (i >= 1) {
	            len += Math.sqrt((po[i].x - po[i - 1].x) * (po[i].x - po[i - 1].x) + (po[i].y - po[i - 1].y) * (po[i].y - po[i - 1].y));
	        }
	    }
	    var half = len / 2.0;
	    var x, y;
	    for (var i = 0; i < po.length - 1; i++) {
	        var dis = Math.sqrt((po[i].x - po[i + 1].x) * (po[i].x - po[i + 1].x) + (po[i].y - po[i + 1].y) * (po[i].y - po[i + 1].y));
	        if (half > dis) {
	            half -= dis;
	        } else {
	            var addx, addy;
	            addx = half * (Math.abs(po[i].x - po[i + 1].x) / Math.sqrt((po[i].x - po[i + 1].x) * (po[i].x - po[i + 1].x) + (po[i].y - po[i + 1].y) * (po[i].y - po[i + 1].y)));
	            addy = half * (Math.abs(po[i].y - po[i + 1].y) / Math.sqrt((po[i].x - po[i + 1].x) * (po[i].x - po[i + 1].x) + (po[i].y - po[i + 1].y) * (po[i].y - po[i + 1].y)));
	            if (po[i + 1].x < po[i].x) {
	                x = po[i].x - addx;
	            } else x = po[i].x + addx;
	            if (po[i + 1].y < po[i].y) {
	                y = po[i].y - addy;
	            } else y = po[i].y + addy;
	            break;
	        }
	    }
	    return {x:x,y:y};
	}
  
    return new creatDashedLine();
});