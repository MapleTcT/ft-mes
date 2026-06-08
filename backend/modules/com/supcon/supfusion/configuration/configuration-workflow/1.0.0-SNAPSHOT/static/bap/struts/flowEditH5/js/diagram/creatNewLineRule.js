/**
 * 新绘制的连接线画线规则
 * 默认使用直线绘制
 * 若连线对象已存在连接线，同方向不绘制，反方向默认使用折线绘制
 * 指向作废、结束活动的迁移线不允许为驳回线类别
 */
define(function (require, exports, module) {
    var creatLine = function (element, start, end, source, target) {
        this._element = element;
        this._start = start;
        this._end = end;
        this._source = source;
        this._target = target;
    };
    /*说明
     *0表示连接对象之间没有连线，（直线）
     *1表示连接对象之间已有同方向线条(不绘制)
     *2表示连接对象之间已有反方向线条（绘制折线）
     *3表示指向作废、结束活动的迁移线且类型为驳回线的情况（不绘制）
     *4表示迁移线指向自己（不绘制）
     *5表示迁移线类别为驳回线
    */
    creatLine.prototype.getLineState = function (reject) {
        var element = this._element,
            start = this._start,
            end = this._end,
            source = this._source,
            target = this._target;
        var state = 0;
        var targetType=target.type;
        if(targetType=="bpmn:EndCancelEvent"||targetType=="bpmn:EndEvent"){
            if(reject=="1"){
                //指向作废、结束活动的迁移线且类型为驳回线的情况
                state=3;
                return state;
            }
        }
        if(target.id==source.id){
            state=4;
            return state;//指向自己的迁移线
        }
        for (var i = 0; i < element.length; i++) {
            var item = element[i];
            var type = item.type;
            if (type == "bpmn:SequenceFlow") {
                var sameD = source.id + target.id;//同方向
                var differD = target.id + source.id;//反方向
                var business = item.businessObject;
                var lineD = business.sourceRef.id + business.targetRef.id;//当前遍历线条方向
                if (sameD == lineD) {
                    //两节点间已存在同方向线条
                    state = 1;
                    break;
                } else if (differD == lineD) {
                    //两节点间已存在反方向线条，存在的线条为直线则使用直线，为折线则使用直线
                	if(business.di.waypoint.length==2) state = 2;
                }
            }
        }
        if(state==0){
        	//迁移线类型为驳回线时不允许绘制直线
        	if(reject=="1"){state=5;}
        }
        return state;
    }
    creatLine.prototype.getPoint = function (reject) {
        var start = this._start,
        end = this._end;
        var state=this.getLineState(reject);
        switch (state) {
            case 0:
                return null;
            case 1:
                return null;
            case 2:
                var center = {};
                var avgX = (end.x + start.x) / 2.05;
                var avgY = (end.y + start.y) / 2.05;
                center.x = avgX - end.x < 50 ? avgX - 50 : avgX;
                center.y = avgY - end.y < 50 ? avgY - 50 : avgY;
                return [start, center, end];
            case 3:
            	return null;
            case 4:
            	return null;
            case 5:
                var center = {};
                var avgX = (end.x + start.x) / 2.05;
                var avgY = (end.y + start.y) / 2.05;
                center.x = avgX - end.x < 50 ? avgX - 50 : avgX;
                center.y = avgY - end.y < 50 ? avgY - 50 : avgY;
                return [start, center, end];
        }
    }
    return creatLine;
});