
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
 * &amp;lt;element name="PostResult" type="{http://www.139130.net}GsmsResponse"/&amp;gt;
 * &amp;lt;/sequence&amp;gt;
 * &amp;lt;/restriction&amp;gt;
 * &amp;lt;/complexContent&amp;gt;
 * &amp;lt;/complexType&amp;gt;
 * &lt;/pre&gt;
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "postResult"
})
@XmlRootElement(name = "PostResponse")
public class PostResponse {

    @XmlElement(name = "PostResult", required = true)
    protected GsmsResponse postResult;

    /**
     * 获取postResult属性的值。
     *
     * @return possible object is
     * {@link GsmsResponse }
     */
    public GsmsResponse getPostResult() {
        return postResult;
    }

    /**
     * 设置postResult属性的值。
     *
     * @param value allowed object is
     *              {@link GsmsResponse }
     */
    public void setPostResult(GsmsResponse value) {
        this.postResult = value;
    }

}
