
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
 * &amp;lt;element name="PostSingleResult" type="{http://www.w3.org/2001/XMLSchema}int"/&amp;gt;
 * &amp;lt;/sequence&amp;gt;
 * &amp;lt;/restriction&amp;gt;
 * &amp;lt;/complexContent&amp;gt;
 * &amp;lt;/complexType&amp;gt;
 * &lt;/pre&gt;
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "postSingleResult"
})
@XmlRootElement(name = "PostSingleResponse")
public class PostSingleResponse {

    @XmlElement(name = "PostSingleResult")
    protected int postSingleResult;

    /**
     * 获取postSingleResult属性的值。
     */
    public int getPostSingleResult() {
        return postSingleResult;
    }

    /**
     * 设置postSingleResult属性的值。
     */
    public void setPostSingleResult(int value) {
        this.postSingleResult = value;
    }

}
