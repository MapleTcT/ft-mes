<template>
<div class="systemCodeTree"  v-if="isShowSystemCode">
    <header class="m_head" :style="{background: bgColor}">
        <div class="m_hbox">
            <span class="head_arrow" @click="backHistory">
                <a href="javascript:;" class="m_arrow"></a>            
            </span>
            <h2 class="m_title">系统编码</h2>
        </div>
    </header>

    <!--系统编码树形，树组件-->
    <el-tree
        ref="tree"
        node-key="id"        
        :data="data"
        :show-checkbox="showCheckbox"
        :default-expanded-keys="defaultExpandedKeys"
        :props="defaultProps" 
        :default-expand-all="false"
        :expand-on-click-node="expandOnClickNode"
        :check-on-click-node="true"
        :highlight-current="highlightCurrent"
        :indent="20"
        :check-strictly="false"
        @node-click="handleNodeClick">
    </el-tree>

    <div class="m_foot" v-if="showCheckbox">
        <a class="m_botton" v-on:click="btnClickEvent('cancel')">取消</a>
        <a class="m_botton btn_submit" v-on:click="btnClickEvent('confirm')">确认</a>
    </div>
</div>  
</template>

<script>
    import _commonJs from "@/assets/js/itemList/index.js"; //引用模块进来
    import { Toast, Indicator, Popup } from "mint-ui";
    export default {
        name: "systemCodeTree",
        props: ["params"],
        data() {
            return {
                showCheckbox: true, //是否显示复选框
                isShowSystemCode: false, //是否显示系统编码树形界面
                data: [], //系统编码树形数据
                defaultProps: {
                    children: 'children2',
                    label: 'value',
                    isLeaf: 'leaf'
                }, //默认配置选项
                defaultExpandedKeys: [], //默认展开的节点的key的数组
                highlightCurrent: true, //是否高亮当前选中节点
                expandOnClickNode: false, //是否在点击节点的时候展开或者收缩节点
            };
        },
        //数据监听
        watch: {
            'isShowSystemCode': {
                deep: true,
                handler: function(newVal, oldVal) {
                    let $this = this;
                    if (newVal == true) {
                        let params = $this.params;
                        if (!$this.showCheckbox && params.isOnlyLeaf) {//配置仅可选叶子节点,点击节点的时候可以展开或者收缩节点
                            $this.expandOnClickNode = true;
                        } else {
                            $this.expandOnClickNode = false;
                        }
                        $this.$nextTick(function() {
                            $this.setExpandedAndCheckedKeys(params);
                        });
                    }
                }
            }
        },
        beforeMount() {
            if (window._PAGECONFIG && window._PAGECONFIG.navBar) {
                this.bgColor = window._PAGECONFIG.navBar.bgColor?window._PAGECONFIG.navBar.bgColor:''; // 背景色
            }
        },
        methods: {
            //系统编码树形单选点击节点回填数据
            handleNodeClick(data) {
                let $this = this;
                if (!$this.showCheckbox) {
                    const zTree = $this.$refs.tree;
                    const store = zTree.store;
                    const currentNode = store.getCurrentNode(); //获取当前被选中节点的data
                    const currentKey = currentNode.key; //获取当前被选中节点的key
                    if (currentNode && currentKey) {
                        if ($this.params.isOnlyLeaf) { //配置仅可选叶子节点
                            if (currentNode.isLeaf) {
                                $this.params.value = currentNode.label;
                                $this.params.valueId = currentKey;
                                $this.highlightCurrent = true;
                                $this.$forceUpdate();
                                $this.isShowSystemCode = false;
                            } else {
                                $this.highlightCurrent = false;
                            }
                        } else {
                            $this.params.value = currentNode.label;
                            $this.params.valueId = currentKey;
                            $this.$forceUpdate();
                            $this.isShowSystemCode = false; 
                        }
                    }
                }
            },

            //设置树展开的节点和选中的节点
            setExpandedAndCheckedKeys: function(data) {
                let $this = this;
                const zTree = $this.$refs.tree;
                var key = data.valueId;
                $this.packUpAllNodes();
                $this.defaultExpandedKeys = [];
                if (data) {
                    if ($this.showCheckbox) { //系统编码树形多选，在显示复选框的情况下
                        if (key && key.length > 0) {
                            for (let i = 0; i < key.length; i++) {
                                let treeNode = zTree.getNode(key[i]);
                                if (treeNode.isLeaf) { //叶子节点
                                    $this.defaultExpandedKeys.push(key[i]);
                                } else if (treeNode.level != 1) { //其他节点，非一级节点
                                    var parentKey = treeNode.parent.key;
                                    $this.defaultExpandedKeys.push(parentKey);
                                }
                                treeNode.checked = true;                            
                            }
                            zTree.setCheckedKeys(key);                      
                        }
                    } else { //系统编码树形单选
                        if (key) {
                            const treeNode = zTree.getNode(key);
                            const store = zTree.store;
                            if (treeNode.isLeaf) { //叶子节点
                                $this.defaultExpandedKeys.push(key);
                            } else if (treeNode.level != 1) { //其他节点，非一级节点
                                var parentKey = treeNode.parent.key;
                                $this.defaultExpandedKeys.push(parentKey);
                            }
                            treeNode.checked = true;
                            store.setCurrentNode(treeNode); //高亮当前选中的节点
                        }
                    }
                }
                $this.$forceUpdate();
            },

            /**
             * 系统编码树形多选点击取消或确认按钮事件，确定回填数据，只回填叶子节点
             * @params {string} type 按钮的类型
             * 当点击取消时，type="cancel"，关闭页面不回填内容
             * 当点击确认时，type="confirm"，关闭页面回填选择内容
             */
            btnClickEvent: function(type) {
                let $this = this;
                const zTree = $this.$refs.tree;
                if (type == "confirm") {
                    const checkedKeys = zTree.getCheckedKeys(); //获取被选中的节点的key所组成的数组
                    const checkedNodes = zTree.getCheckedNodes(); //获取被选中的节点所组成的数组  
                    if (checkedKeys && checkedNodes && checkedKeys.length > 0 && checkedNodes.length > 0) {
                        var checkedLeafKeys = []; //选中的叶子节点的key所组成的数组   
                        for (let i = 0; i < checkedKeys.length; i++) {
                            let treeNode = zTree.getNode(checkedKeys[i]);
                            if (treeNode.isLeaf) { //叶子节点
                                checkedLeafKeys.push(checkedKeys[i]);
                            }
                        }
                        var checkedLeafNodeVals = []; //选中的叶子节点对象的名称组成的数组                   
                        for( let i = 0; i < checkedNodes.length; i++) {
                            if (checkedNodes[i].leaf) {
                                checkedLeafNodeVals.push(checkedNodes[i].value);                                
                            }
                        }
                        checkedLeafNodeVals = checkedLeafNodeVals.join(",")
                        $this.params.value = checkedLeafNodeVals;
                        $this.params.valueId = checkedLeafKeys;  
                    }
                    $this.$forceUpdate();
                    $this.isShowSystemCode = false;                    
                } else if (type == "cancel") {
                    $this.isShowSystemCode = false;
                }
            },

            //将树节点全部收起,并取消所有勾选的节点
            packUpAllNodes: function() {
                const zTree = this.$refs.tree;
                const store = zTree.store;
                if (store && store._getAllNodes()) {
                    for (let i = 0; i < store._getAllNodes().length; i++) {
                        store._getAllNodes()[i].expanded = false;
                        store._getAllNodes()[i].checked = false;
                    }
                }
            },

            //返回上一个页面
            backHistory: function() {
                this.isShowSystemCode = false; //关闭系统编码树形界面
            },
        }
  };
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less" scoped>
    @import "../../assets/css/systemCodeTree.less";
</style>



// WEBPACK FOOTER //
// src/components/form/systemCodeTree.vue