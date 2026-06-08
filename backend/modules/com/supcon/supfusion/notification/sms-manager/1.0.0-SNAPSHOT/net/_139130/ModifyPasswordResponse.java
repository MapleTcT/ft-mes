
package net._139130;

import javax.xml.bind.annotation.*;


/**
 * &lt;p&gt;anonymous complex type的 Java 类。
 * <p>
 * &lt;p&gt;以下模式片段指定包含在此类中的预期内容。
 * <p>
 * &lt;pre&gt;
 * &amp;lt;complexType&amp;gt;
 * &amp;lt;complexContent&amp;gt;
 * &amp;lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&amp;gt;
 * &amp;lt;sequence&amp;gt;
 * &amp;lt;element name="ModifyPasswordResult" type="{http://www.w3.org/2001/XMLSchema}int"/&amp;gt;
 * &amp;lt;/sequence&amp;gt;
 * &amp;lt;/restriction&amp;gt;
 * &amp;lt;/complexContent&amp;gt;
 * &amp;lt;/complexType&amp;gt;
 * &lt;/pre&gt;
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "modifyPasswordResult"
})
@XmlRootElement(name = "ModifyPasswordResponse")
public class ModifyPasswordResponse {

    @XmlElement(name = "ModifyPasswordResult")
    protected int modifyPasswordResult;

    /**
     * 获取modifyPasswordResult属性的值。
     */
    public int getModifyPasswordResult() {
        return modifyPasswordResult;
    }

    /**
     * 设置modifyPasswordResult属性的值。
     */
    public void setModifyPasswordResult(int value) {
        this.modifyPasswordResult = value;
    }

}
