
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
 * &amp;lt;element name="PostMassResult" type="{http://www.w3.org/2001/XMLSchema}int"/&amp;gt;
 * &amp;lt;/sequence&amp;gt;
 * &amp;lt;/restriction&amp;gt;
 * &amp;lt;/complexContent&amp;gt;
 * &amp;lt;/complexType&amp;gt;
 * &lt;/pre&gt;
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "postMassResult"
})
@XmlRootElement(name = "PostMassResponse")
public class PostMassResponse {

    @XmlElement(name = "PostMassResult")
    protected int postMassResult;

    /**
     * 获取postMassResult属性的值。
     */
    public int getPostMassResult() {
        return postMassResult;
    }

    /**
     * 设置postMassResult属性的值。
     */
    public void setPostMassResult(int value) {
        this.postMassResult = value;
    }

}
